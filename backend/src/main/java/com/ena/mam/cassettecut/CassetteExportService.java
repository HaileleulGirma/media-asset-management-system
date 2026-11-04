package com.ena.mam.cassettecut;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.cassettecategory.CassetteCategoryRepository;
import com.ena.mam.digitizedmedia.DigitizedMediaService;
import com.ena.mam.dto.request.filter.CassetteCutFilter;
import com.ena.mam.dto.request.filter.DigitizedMediaFilter;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
import com.ena.mam.staffmember.StaffMemberRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exports for the cassette page.
 *  - Search export: cuts (CassetteCut), with the cassette they sit in.
 *  - Summary export: cassettes (DigitizedMedia) imported by one staff member.
 */
@Slf4j
@Service
public class CassetteExportService {

    /** Largest result set an export will accept; bigger ones are rejected before any bytes are written. */
    public static final int MAX_EXPORT_ROWS = 500;

    private static final String[] CUT_HEADERS_STAFF =
            {"Cassette", "Cut Title", "Cut Date", "Cut Category", "File Path", "Privacy"};
    private static final String[] CUT_HEADERS_VIEWER =
            {"Cassette", "Cut Title", "Cut Date", "Cut Category", "File Path"};
    private static final float[] CUT_WIDTHS_STAFF = {1.8f, 3.0f, 1.3f, 1.8f, 3.0f, 1.0f};
    private static final float[] CUT_WIDTHS_VIEWER = {1.9f, 3.2f, 1.4f, 1.9f, 3.2f};

    private static final String[] CASSETTE_HEADERS =
            {"Cassette", "Digitized", "File Path", "Privacy"};
    private static final float[] CASSETTE_WIDTHS = {2.0f, 1.4f, 4.0f, 1.1f};

    private final CassetteCutService cassetteCutService;
    private final DigitizedMediaService digitizedMediaService;
    private final CassetteCategoryRepository categoryRepository;
    private final StaffMemberRepository staffMemberRepository;

    public CassetteExportService(
            CassetteCutService cassetteCutService,
            DigitizedMediaService digitizedMediaService,
            CassetteCategoryRepository categoryRepository,
            StaffMemberRepository staffMemberRepository
    ) {
        this.cassetteCutService = cassetteCutService;
        this.digitizedMediaService = digitizedMediaService;
        this.categoryRepository = categoryRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    // ---------- Limit check (shared by both controllers) ----------

    /**
     * Writes a 422 JSON error and returns true when the result set is over the limit.
     * Call before touching the response headers or body.
     */
    public boolean rejectIfTooLarge(long total, HttpServletResponse response) throws IOException {
        if (total <= MAX_EXPORT_ROWS) {
            return false;
        }
        response.setStatus(422);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"message\":\"Too many results to export (%d). The limit is %d - narrow your filters and try again.\"}"
                        .formatted(total, MAX_EXPORT_ROWS));
        return true;
    }

    @Transactional(readOnly = true)
    public long countCuts(CassetteCutFilter filter) {
        return cassetteCutService.search(filter, PageRequest.of(0, 1)).getTotalElements();
    }

    @Transactional(readOnly = true)
    public long countCassettes(DigitizedMediaFilter filter) {
        return digitizedMediaService.search(filter, PageRequest.of(0, 1)).getTotalElements();
    }

    // ---------- Search export: cuts ----------

    @Transactional(readOnly = true)
    public void exportCutsToCsv(CassetteCutFilter filter, boolean includeStaff, Writer writer) throws Exception {
        List<CreateCassetteCutResponse> data = fetchCuts(filter);
        Map<Long, String> categoryNames = buildCategoryNames(cutCategoryIds(data));
        writeCsv(writer, cutHeaders(includeStaff), cutRows(data, includeStaff, categoryNames));
    }

    @Transactional(readOnly = true)
    public void exportCutsToPdf(CassetteCutFilter filter, boolean includeStaff, OutputStream out) {
        List<CreateCassetteCutResponse> data = fetchCuts(filter);
        Map<Long, String> categoryNames = buildCategoryNames(cutCategoryIds(data));

        Document document = newDocument(out);
        document.open();
        PdfFonts fonts = new PdfFonts();

        Paragraph title = new Paragraph("Cassette Cuts Report", fonts.title);
        title.setSpacingAfter(14f);
        document.add(title);

        document.add(buildTable(
                cutHeaders(includeStaff),
                includeStaff ? CUT_WIDTHS_STAFF : CUT_WIDTHS_VIEWER,
                cutRows(data, includeStaff, categoryNames),
                fonts));
        document.close();
    }

    // ---------- Summary export: cassettes imported by one staff member ----------

    @Transactional(readOnly = true)
    public void exportCassettesToCsv(DigitizedMediaFilter filter, Writer writer) throws Exception {
        List<CreateDigitizedMediaResponse> data = fetchCassettes(filter);
        Map<Long, String> categoryNames = buildCategoryNames(cassetteCategoryIds(data));
        writeCsv(writer, CASSETTE_HEADERS, cassetteRows(data, categoryNames));
    }

    @Transactional(readOnly = true)
    public void exportCassettesToPdf(DigitizedMediaFilter filter, OutputStream out) {
        List<CreateDigitizedMediaResponse> data = fetchCassettes(filter);
        Map<Long, String> categoryNames = buildCategoryNames(cassetteCategoryIds(data));

        Document document = newDocument(out);
        document.open();
        PdfFonts fonts = new PdfFonts();

        Paragraph title = new Paragraph("Cassette Staff Summary", fonts.title);
        title.setSpacingAfter(4f);
        document.add(title);

        String staffName = filter.importerId() == null ? "" : staffMemberRepository.findById(filter.importerId())
                .map(s -> s.getStaffMemberName()).orElse("Unknown (#" + filter.importerId() + ")");
        String range = filter.startDate() != null
                ? filter.startDate() + " to " + filter.effectiveEndDate() : "";
        Paragraph subtitle = new Paragraph(
                (staffName + (range.isEmpty() ? "" : "  |  " + range)).trim(), fonts.row);
        subtitle.setSpacingAfter(10f);
        document.add(subtitle);

        // Totals + by-category breakdown, computed from the same rows as the table.
        long privateCount = data.stream().filter(c -> Boolean.TRUE.equals(c.isPrivate())).count();
        Map<Long, Integer> perCategory = new LinkedHashMap<>();
        data.forEach(c -> perCategory.merge(c.identifierCategoryId(), 1, Integer::sum));

        Paragraph totals = new Paragraph(
                "Cassettes imported: %d    Private: %d    Categories: %d"
                        .formatted(data.size(), privateCount, perCategory.size()),
                fonts.header);
        totals.setSpacingAfter(10f);
        document.add(totals);

        if (!perCategory.isEmpty()) {
            List<String[]> categoryRows = new ArrayList<>();
            perCategory.entrySet().stream()
                    .sorted((a, b) -> b.getValue() != a.getValue().intValue()
                            ? Integer.compare(b.getValue(), a.getValue())
                            : categoryLabel(a.getKey(), categoryNames).compareTo(categoryLabel(b.getKey(), categoryNames)))
                    .forEach(e -> categoryRows.add(new String[]{
                            categoryLabel(e.getKey(), categoryNames), String.valueOf(e.getValue())}));
            PdfPTable byCategory = buildTable(
                    new String[]{"Category", "Cassettes"}, new float[]{3f, 1f}, categoryRows, fonts);
            byCategory.setWidthPercentage(45);
            byCategory.setHorizontalAlignment(Element.ALIGN_LEFT);
            byCategory.setSpacingAfter(14f);
            document.add(byCategory);
        }

        document.add(buildTable(CASSETTE_HEADERS, CASSETTE_WIDTHS, cassetteRows(data, categoryNames), fonts));
        document.close();
    }

    // ---------- Fetching ----------

    private List<CreateCassetteCutResponse> fetchCuts(CassetteCutFilter filter) {
        // One page is enough: the controller rejects anything over MAX_EXPORT_ROWS first.
        // The service applies its own newest-first sort and the viewer privacy rule.
        return new ArrayList<>(cassetteCutService
                .search(filter, PageRequest.of(0, MAX_EXPORT_ROWS)).getContent());
    }

    private List<CreateDigitizedMediaResponse> fetchCassettes(DigitizedMediaFilter filter) {
        return new ArrayList<>(digitizedMediaService
                .search(filter, PageRequest.of(0, MAX_EXPORT_ROWS)).getContent());
    }

    // ---------- Rows ----------

    private String[] cutHeaders(boolean includeStaff) {
        return includeStaff ? CUT_HEADERS_STAFF : CUT_HEADERS_VIEWER;
    }

    private List<String[]> cutRows(List<CreateCassetteCutResponse> data, boolean includeStaff,
                                   Map<Long, String> categoryNames) {
        List<String[]> rows = new ArrayList<>();
        for (CreateCassetteCutResponse cut : data) {
            String cassette = cut.identifierNumber() != null
                    ? cassetteLabel(cut.identifierCategoryId(), cut.identifierNumber(), categoryNames)
                    : "Cassette #" + cut.digitizedMediaId();
            String[] base = {
                    cassette,
                    nz(cut.CutTitle()),
                    cut.cutDate() != null ? cut.cutDate().toString() : "",
                    categoryLabel(cut.CassetteCategoryId(), categoryNames),
                    nz(cut.filePath())
            };
            if (includeStaff) {
                rows.add(new String[]{base[0], base[1], base[2], base[3], base[4], privacy(cut.isPrivate())});
            } else {
                rows.add(base);
            }
        }
        return rows;
    }

    private List<String[]> cassetteRows(List<CreateDigitizedMediaResponse> data, Map<Long, String> categoryNames) {
        List<String[]> rows = new ArrayList<>();
        for (CreateDigitizedMediaResponse c : data) {
            rows.add(new String[]{
                    cassetteLabel(c.identifierCategoryId(), c.identifierNumber(), categoryNames),
                    c.digitizedDate() != null ? c.digitizedDate().toString() : "",
                    nz(c.filePath()),
                    privacy(c.isPrivate())
            });
        }
        return rows;
    }

    private static String privacy(Boolean isPrivate) {
        return Boolean.TRUE.equals(isPrivate) ? "Private" : "Public";
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    // ---------- Category names ----------

    private Set<Long> cutCategoryIds(List<CreateCassetteCutResponse> data) {
        Set<Long> ids = new HashSet<>();
        for (CreateCassetteCutResponse c : data) {
            if (c.CassetteCategoryId() != null) ids.add(c.CassetteCategoryId());
            if (c.identifierCategoryId() != null) ids.add(c.identifierCategoryId());
        }
        return ids;
    }

    private Set<Long> cassetteCategoryIds(List<CreateDigitizedMediaResponse> data) {
        Set<Long> ids = new HashSet<>();
        for (CreateDigitizedMediaResponse c : data) {
            if (c.identifierCategoryId() != null) ids.add(c.identifierCategoryId());
        }
        return ids;
    }

    private Map<Long, String> buildCategoryNames(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, String> names = new HashMap<>();
        for (CassetteCategory c : categoryRepository.findAllById(ids)) {
            names.put(c.getCategoryId(), c.getCategoryName());
        }
        return names;
    }

    private String categoryLabel(Long id, Map<Long, String> names) {
        if (id == null) return "—";
        String name = names.get(id);
        return name != null ? name : "Category " + id;
    }

    private String cassetteLabel(Long categoryId, Long number, Map<Long, String> names) {
        return categoryLabel(categoryId, names) + " #" + number;
    }

    // ---------- CSV / PDF plumbing ----------

    private void writeCsv(Writer writer, String[] headers, List<String[]> rows) throws Exception {
        // UTF-8 BOM so Microsoft Excel opens Amharic / Unicode characters properly
        writer.write('\ufeff');
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.Builder.create().setHeader(headers).build())) {
            for (String[] row : rows) {
                printer.printRecord((Object[]) row);
            }
        }
    }

    private Document newDocument(OutputStream out) {
        Document document = new Document(PageSize.A4.rotate(), 28f, 28f, 28f, 28f);
        PdfWriter.getInstance(document, out);
        return document;
    }

    private PdfPTable buildTable(String[] headers, float[] widths, List<String[]> rows, PdfFonts fonts) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setHeaderRows(1);

        Color headerBg = new Color(240, 242, 245);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fonts.header));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(6f);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
        for (String[] row : rows) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value, fonts.row));
                cell.setPadding(5f);
                cell.setVerticalAlignment(Element.ALIGN_TOP);
                table.addCell(cell);
            }
        }
        return table;
    }

    private final class PdfFonts {
        final Font title;
        final Font header;
        final Font row;

        PdfFonts() {
            BaseFont base = loadUnicodeBaseFont();
            title = base != null ? new Font(base, 15, Font.BOLD) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
            header = base != null ? new Font(base, 10, Font.BOLD) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            row = base != null ? new Font(base, 9, Font.NORMAL) : FontFactory.getFont(FontFactory.HELVETICA, 9);
        }
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
}