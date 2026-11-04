package com.ena.mam.production;

import com.ena.mam.dto.request.filter.ProductionFilter;
import com.ena.mam.dto.response.CreateProductionResponse;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class ProductionExportService {

    /** Largest result set an export will accept; bigger ones are rejected by the controller. */
    public static final int MAX_EXPORT_ROWS = 500;

    private static final String[] HEADERS = {"Date", "Title", "File Path", "Duration", "Size (GB)"};

    private final ProductionService productionService;

    public ProductionExportService(ProductionService productionService) {
        this.productionService = productionService;
    }

    /** Total number of production items matching the filter (cheap: one row plus a count query). */
    @Transactional(readOnly = true)
    public long countMatching(ProductionFilter filter) {
        return productionService.search(filter, PageRequest.of(0, 1)).getTotalElements();
    }

    @Transactional(readOnly = true)
    public void exportToCsv(ProductionFilter filter, Writer writer) throws Exception {
        List<CreateProductionResponse> data = fetchProduction(filter);

        // Write UTF-8 BOM so Microsoft Excel opens Amharic / Unicode characters properly
        writer.write('\ufeff');

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.Builder.create()
                .setHeader(HEADERS)
                .build())) {

            for (CreateProductionResponse item : data) {
                csvPrinter.printRecord(buildRow(item));
            }
        }
    }

    @Transactional(readOnly = true)
    public void exportToPdf(ProductionFilter filter, OutputStream outputStream) {
        List<CreateProductionResponse> data = fetchProduction(filter);

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

        Paragraph title = new Paragraph("Production Archive Report", titleFont);
        title.setSpacingAfter(14f);
        document.add(title);

        float[] columnWidths = {1.3f, 3.2f, 3.2f, 1.1f, 1.0f};

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(columnWidths);
        table.setHeaderRows(1);

        Color headerBg = new Color(240, 242, 245);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(6f);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        for (CreateProductionResponse item : data) {
            for (String value : buildRow(item)) {
                addCell(table, value, rowFont);
            }
        }

        document.add(table);
        document.close();
    }

    private List<String> buildRow(CreateProductionResponse item) {
        List<String> row = new ArrayList<>();
        row.add(item.date() != null ? item.date().toString() : "");
        row.add(item.title() != null ? item.title() : "");
        row.add(item.filePath() != null ? item.filePath() : "");
        row.add(formatDuration(item.durationSec()));
        row.add(item.fileSize() != null ? String.format(Locale.ROOT, "%.2f", item.fileSize()) : "");
        return row;
    }

    // 3725 -> "1:02:05", same format as the on-screen tables.
    private String formatDuration(Number totalSec) {
        if (totalSec == null) return "—";
        long sec = Math.max(0, Math.round(totalSec.doubleValue()));
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
    }

    private List<CreateProductionResponse> fetchProduction(ProductionFilter filter) {
        // One page is enough: the controller rejects anything over MAX_EXPORT_ROWS before we get here.
        // ProductionService.search applies its own newest-first sort, so only page/size matter.
        return new ArrayList<>(
                productionService.search(filter, PageRequest.of(0, MAX_EXPORT_ROWS)).getContent()
        );
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
}