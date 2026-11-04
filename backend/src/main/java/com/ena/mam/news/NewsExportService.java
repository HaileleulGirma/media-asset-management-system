package com.ena.mam.news;

import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.dto.request.filter.NewsFilter;
import com.ena.mam.dto.response.CreateNewsResponse;
import com.ena.mam.location.LocationRepository;
import com.ena.mam.reporter.ReporterRepository;
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
public class NewsExportService {

    /** Largest result set an export will accept; bigger ones are rejected by the controller. */
    public static final int MAX_EXPORT_ROWS = 500;

    private final NewsService newsService;
    private final CameramanRepository cameramanRepository;
    private final ReporterRepository reporterRepository;
    private final LocationRepository locationRepository;
    private final StaffMemberRepository staffMemberRepository;

    public NewsExportService(
            NewsService newsService,
            CameramanRepository cameramanRepository,
            ReporterRepository reporterRepository,
            LocationRepository locationRepository,
            StaffMemberRepository staffMemberRepository
    ) {
        this.newsService = newsService;
        this.cameramanRepository = cameramanRepository;
        this.reporterRepository = reporterRepository;
        this.locationRepository = locationRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    /** Total number of news items matching the filter (cheap: one row plus a count query). */
    @Transactional(readOnly = true)
    public long countMatching(NewsFilter filter) {
        return newsService.search(filter, PageRequest.of(0, 1)).getTotalElements();
    }

    /** id -> name lookups used to turn the ids in each news row into readable names. */
    private record Lookups(
            Map<Long, String> cameramen,
            Map<Long, String> reporters,
            Map<Long, String> locations,
            Map<Long, String> staff
    ) {
    }

    private static String[] headers(boolean includeStaff) {
        return includeStaff
                ? new String[]{"Date", "Title", "Cameramen", "Reporters", "Locations", "File Path", "Files", "Size (GB)", "Privacy", "Imported By", "Ingested By"}
                : new String[]{"Date", "Title", "Cameramen", "Reporters", "Locations", "File Path", "Files", "Size (GB)"};
    }

    @Transactional(readOnly = true)
    public void exportToCsv(NewsFilter filter, boolean includeStaff, Writer writer) throws Exception {
        List<CreateNewsResponse> data = fetchSortedNews(filter);
        Lookups lookups = buildLookups(data, includeStaff);

        // Write UTF-8 BOM so Microsoft Excel opens Amharic / Unicode characters properly
        writer.write('\ufeff');

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create()
                .setHeader(headers(includeStaff))
                .build())) {

            for (CreateNewsResponse item : data) {
                csvPrinter.printRecord(buildRow(item, lookups, includeStaff));
            }
        }
    }

    @Transactional(readOnly = true)
    public void exportToPdf(NewsFilter filter, boolean includeStaff, OutputStream outputStream) {
        List<CreateNewsResponse> data = fetchSortedNews(filter);
        Lookups lookups = buildLookups(data, includeStaff);

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

        Paragraph title = new Paragraph("News Archive Report", titleFont);
        title.setSpacingAfter(14f);
        document.add(title);

        String[] headers = headers(includeStaff);

        float[] columnWidths = includeStaff
                ? new float[]{1.0f, 2.0f, 1.4f, 1.4f, 1.4f, 1.9f, 0.7f, 0.9f, 0.9f, 1.2f, 1.2f}
                : new float[]{1.2f, 2.5f, 1.8f, 1.8f, 1.8f, 2.4f, 0.8f, 1.0f};

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

        for (CreateNewsResponse item : data) {
            for (String value : buildRow(item, lookups, includeStaff)) {
                addCell(table, value, rowFont);
            }
        }

        document.add(table);
        document.close();
    }

    private List<String> buildRow(CreateNewsResponse item, Lookups lookups, boolean includeStaff) {
        List<String> row = new ArrayList<>();
        row.add(item.newsDate() != null ? item.newsDate().toString() : "");
        row.add(item.title() != null ? item.title() : "");
        row.add(formatIdsToNames(item.cameramanIds(), lookups.cameramen()));
        row.add(formatIdsToNames(item.reporterIds(), lookups.reporters()));
        row.add(formatIdsToNames(item.locationIds(), lookups.locations()));
        row.add(item.filePath() != null ? item.filePath() : "");
        row.add(item.numberOfFiles() != null ? String.valueOf(item.numberOfFiles()) : "");
        row.add(item.totalSize() != null ? String.format(Locale.ROOT, "%.2f", item.totalSize()) : "");

        if (includeStaff) {
            row.add(Boolean.TRUE.equals(item.isPrivate()) ? "Private" : "Public");
            row.add(item.importerId() != null
                    ? lookups.staff().getOrDefault(item.importerId(), "—")
                    : "—");
            row.add(item.ingestorId() != null
                    ? lookups.staff().getOrDefault(item.ingestorId(), "—")
                    : "Not ingested");
        }
        return row;
    }

    private List<CreateNewsResponse> fetchSortedNews(NewsFilter filter) {
        // One page is enough: the controller rejects anything over MAX_EXPORT_ROWS before we get here.
        // NewsService.search applies its own newest-first sort, so only page/size matter.
        Page<CreateNewsResponse> result = newsService.search(filter, PageRequest.of(0, MAX_EXPORT_ROWS));
        List<CreateNewsResponse> all = new ArrayList<>(result.getContent());

        all.sort(
                Comparator.comparing(CreateNewsResponse::newsDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CreateNewsResponse::newsId, Comparator.nullsLast(Comparator.reverseOrder()))
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

    private Lookups buildLookups(List<CreateNewsResponse> data, boolean includeStaff) {
        return new Lookups(
                buildCameramanMap(data),
                buildReporterMap(data),
                buildLocationMap(data),
                includeStaff ? buildStaffMap(data) : Map.of()
        );
    }

    private Map<Long, String> buildCameramanMap(List<CreateNewsResponse> data) {
        Set<Long> ids = data.stream()
                .filter(n -> n.cameramanIds() != null)
                .flatMap(n -> n.cameramanIds().stream())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return cameramanRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(c -> c.getCameramanId(), c -> c.getCameramanName()));
    }

    private Map<Long, String> buildReporterMap(List<CreateNewsResponse> data) {
        Set<Long> ids = data.stream()
                .filter(n -> n.reporterIds() != null)
                .flatMap(n -> n.reporterIds().stream())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return reporterRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(r -> r.getReporterId(), r -> r.getReporterName()));
    }

    private Map<Long, String> buildLocationMap(List<CreateNewsResponse> data) {
        Set<Long> ids = data.stream()
                .filter(n -> n.locationIds() != null)
                .flatMap(n -> n.locationIds().stream())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return locationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(l -> l.getLocationId(), l -> l.getLocationName()));
    }

    private Map<Long, String> buildStaffMap(List<CreateNewsResponse> data) {
        Set<Long> ids = new HashSet<>();
        for (CreateNewsResponse n : data) {
            if (n.importerId() != null) ids.add(n.importerId());
            if (n.ingestorId() != null) ids.add(n.ingestorId());
        }
        if (ids.isEmpty()) return Map.of();
        return staffMemberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(s -> s.getStaffMemberId(), s -> s.getStaffMemberName()));
    }

    private String formatIdsToNames(Collection<Long> ids, Map<Long, String> nameMap) {
        if (ids == null || ids.isEmpty()) return "—";
        return ids.stream()
                .map(id -> nameMap.getOrDefault(id, "Unknown (#" + id + ")"))
                .collect(Collectors.joining(", "));
    }
}