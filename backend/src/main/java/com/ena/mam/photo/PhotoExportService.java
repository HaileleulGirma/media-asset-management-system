package com.ena.mam.photo;

import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.dto.request.filter.PhotoFilter;
import com.ena.mam.dto.response.CreatePhotoResponse;
import com.ena.mam.location.LocationRepository;
import com.ena.mam.staffmember.StaffMemberRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PhotoExportService {

    /** Largest result set an export will accept; bigger ones are rejected by the controller. */
    public static final int MAX_EXPORT_ROWS = 500;

    private static final Sort NEWEST_FIRST_SORT = Sort.by(
            Sort.Order.desc("photoDate"),
            Sort.Order.desc("photoId")
    );

    private final PhotoService photoService;
    private final CameramanRepository cameramanRepository;
    private final LocationRepository locationRepository;
    private final StaffMemberRepository staffMemberRepository;

    public PhotoExportService(
            PhotoService photoService,
            CameramanRepository cameramanRepository,
            LocationRepository locationRepository,
            StaffMemberRepository staffMemberRepository
    ) {
        this.photoService = photoService;
        this.cameramanRepository = cameramanRepository;
        this.locationRepository = locationRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    /** Total number of photos matching the filter (cheap: one row plus a count query). */
    @Transactional(readOnly = true)
    public long countMatching(PhotoFilter filter) {
        return photoService.search(filter, PageRequest.of(0, 1, NEWEST_FIRST_SORT)).getTotalElements();
    }

    @Transactional(readOnly = true)
    public void exportToCsv(PhotoFilter filter, boolean includeStaff, Writer writer) throws Exception {
        List<CreatePhotoResponse> data = fetchSortedPhotos(filter);

        Map<Long, String> cameramanMap = buildCameramanMap(data);
        Map<Long, String> locationMap = buildLocationMap(data);
        Map<Long, String> staffMap = includeStaff ? buildStaffMap(data) : Map.of();

        // Write UTF-8 BOM so Microsoft Excel opens Amharic / Unicode characters properly
        writer.write('\ufeff');

        String[] headers = includeStaff
                ? new String[]{"Date", "Title", "Cameramen", "Locations", "File Path", "Files", "Size (MB)", "Privacy", "Imported By"}
                : new String[]{"Date", "Title", "Cameramen", "Locations", "File Path", "Files", "Size (MB)"};

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create()
                .setHeader(headers)
                .build())) {

            for (CreatePhotoResponse item : data) {
                String dateStr = item.photoDate() != null ? item.photoDate().toString() : "";
                String titleStr = item.title() != null ? item.title() : "";
                String cameramenStr = formatIdsToNames(item.cameramen(), cameramanMap);
                String locationsStr = formatIdsToNames(item.locations(), locationMap);
                String filePathStr = item.filePath() != null ? item.filePath() : "";
                String filesStr = item.numberOfFiles() != null ? String.valueOf(item.numberOfFiles()) : "";
                String sizeStr = item.fileSizeMb() != null ? String.format("%.2f", item.fileSizeMb()) : "";

                if (includeStaff) {
                    String privacyStr = Boolean.TRUE.equals(item.isPrivate()) ? "Private" : "Public";
                    String importedByStr = item.importedBy() != null
                            ? staffMap.getOrDefault(item.importedBy(), "—")
                            : "—";
                    csvPrinter.printRecord(dateStr, titleStr, cameramenStr, locationsStr, filePathStr, filesStr, sizeStr, privacyStr, importedByStr);
                } else {
                    csvPrinter.printRecord(dateStr, titleStr, cameramenStr, locationsStr, filePathStr, filesStr, sizeStr);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public void exportToPdf(PhotoFilter filter, boolean includeStaff, OutputStream outputStream) {
        List<CreatePhotoResponse> data = fetchSortedPhotos(filter);

        Map<Long, String> cameramanMap = buildCameramanMap(data);
        Map<Long, String> locationMap = buildLocationMap(data);
        Map<Long, String> staffMap = includeStaff ? buildStaffMap(data) : Map.of();

        Document document = new Document(PageSize.A4.rotate(), 28f, 28f, 28f, 28f);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        BaseFont baseFont = loadUnicodeBaseFont();
        Font titleFont = baseFont != null
                ? new Font(baseFont, 15, Font.BOLD)
                : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
        Font headerFont = baseFont != null
                ? new Font(baseFont, 10, Font.BOLD)
                : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font rowFont = baseFont != null
                ? new Font(baseFont, 9, Font.NORMAL)
                : FontFactory.getFont(FontFactory.HELVETICA, 9);

        Paragraph title = new Paragraph("Photo Archive Report", titleFont);
        title.setSpacingAfter(14f);
        document.add(title);

        String[] headers = includeStaff
                ? new String[]{"Date", "Title", "Cameramen", "Locations", "File Path", "Files", "Size (MB)", "Privacy", "Imported By"}
                : new String[]{"Date", "Title", "Cameramen", "Locations", "File Path", "Files", "Size (MB)"};

        float[] columnWidths = includeStaff
                ? new float[]{1.2f, 2.3f, 1.7f, 1.7f, 2.3f, 0.8f, 1.0f, 1.0f, 1.4f}
                : new float[]{1.3f, 2.8f, 2.0f, 2.0f, 2.8f, 0.9f, 1.1f};

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(columnWidths);
        table.setHeaderRows(1);

        Color headerBg = new Color(240, 242, 245);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(6f);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        for (CreatePhotoResponse item : data) {
            addCell(table, item.photoDate() != null ? item.photoDate().toString() : "", rowFont);
            addCell(table, item.title() != null ? item.title() : "", rowFont);
            addCell(table, formatIdsToNames(item.cameramen(), cameramanMap), rowFont);
            addCell(table, formatIdsToNames(item.locations(), locationMap), rowFont);
            addCell(table, item.filePath() != null ? item.filePath() : "", rowFont);
            addCell(table, item.numberOfFiles() != null ? String.valueOf(item.numberOfFiles()) : "", rowFont);
            addCell(table, item.fileSizeMb() != null ? String.format("%.2f", item.fileSizeMb()) : "", rowFont);

            if (includeStaff) {
                String privacyStr = Boolean.TRUE.equals(item.isPrivate()) ? "Private" : "Public";
                String importedByStr = item.importedBy() != null
                        ? staffMap.getOrDefault(item.importedBy(), "—")
                        : "—";
                addCell(table, privacyStr, rowFont);
                addCell(table, importedByStr, rowFont);
            }
        }

        document.add(table);
        document.close();
    }

    private List<CreatePhotoResponse> fetchSortedPhotos(PhotoFilter filter) {
        // One page is enough: the controller rejects anything over MAX_EXPORT_ROWS before we get here.
        Page<CreatePhotoResponse> result =
                photoService.search(filter, PageRequest.of(0, MAX_EXPORT_ROWS, NEWEST_FIRST_SORT));
        List<CreatePhotoResponse> all = new ArrayList<>(result.getContent());

        all.sort(
                Comparator.comparing(CreatePhotoResponse::photoDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CreatePhotoResponse::photoId, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return all;
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);
    }

    private BaseFont loadUnicodeBaseFont() {
        String[] candidatePaths = {
                "fonts/AbyssinicaSIL-Regular.ttf",
                "fonts/NotoSansEthiopic-Regular.ttf",
                "fonts/Nyala.ttf"
        };
        for (String path : candidatePaths) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
                // Try next font candidate
            }
        }
        log.warn("No Ethiopic/Unicode TTF font found in classpath:/fonts/. Falling back to standard Helvetica.");
        return null;
    }

    // --- Bulk Resolution Helpers ---

    private Map<Long, String> buildCameramanMap(List<CreatePhotoResponse> data) {
        Set<Long> ids = data.stream()
                .filter(p -> p.cameramen() != null)
                .flatMap(p -> p.cameramen().stream())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return cameramanRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(c -> c.getCameramanId(), c -> c.getCameramanName()));
    }

    private Map<Long, String> buildLocationMap(List<CreatePhotoResponse> data) {
        Set<Long> ids = data.stream()
                .filter(p -> p.locations() != null)
                .flatMap(p -> p.locations().stream())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return locationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(l -> l.getLocationId(), l -> l.getLocationName()));
    }

    private Map<Long, String> buildStaffMap(List<CreatePhotoResponse> data) {
        Set<Long> ids = data.stream()
                .map(CreatePhotoResponse::importedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return staffMemberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(s -> s.getStaffMemberId(), s -> s.getStaffMemberName()));
    }

    private String formatIdsToNames(Set<Long> ids, Map<Long, String> nameMap) {
        if (ids == null || ids.isEmpty()) return "—";
        return ids.stream()
                .map(id -> nameMap.getOrDefault(id, "Unknown (#" + id + ")"))
                .collect(Collectors.joining(", "));
    }
}