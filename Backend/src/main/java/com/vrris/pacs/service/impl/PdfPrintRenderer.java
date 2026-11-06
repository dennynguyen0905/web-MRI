package com.vrris.pacs.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vrris.pacs.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dung file PDF cua mot phan phieu in.
 *
 * FONT: cac font dung san trong PDF (Helvetica...) dung bang ma WinAnsi, KHONG co dau tieng Viet.
 * Phai nhung mot font TTF Unicode. Thu tu tim:
 *   1. app.print.font-path trong application.yml (neu dat)
 *   2. cac duong dan pho bien tren Linux (DejaVu/Noto/Liberation) va Windows (Segoe UI/Arial)
 * Khong tim thay thi tra 503 kem huong dan, KHONG im lang xuat ra PDF mat dau - phieu ket qua sai
 * dau tieng Viet la sai noi dung y te.
 *
 * Image backend cai font qua deploy/docker/backend.Dockerfile (apk add ttf-dejavu).
 */
@Component
public class PdfPrintRenderer {

    private static final Logger log = LoggerFactory.getLogger(PdfPrintRenderer.class);

    private static final String[] FALLBACK_FONTS = {
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/TTF/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/arial.ttf",
    };

    private final String configuredFontPath;

    public PdfPrintRenderer(@Value("${app.print.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
    }

    /** Mot dong "nhan : gia tri" trong phieu. */
    public record Line(String label, String value) {
    }

    /** Mot bang trong phieu (dung cho danh sach xet nghiem, don thuoc...). */
    public record Table(List<String> headers, List<List<String>> rows) {
    }

    /**
     * @param title      tieu de phieu (ten phan in)
     * @param patient    khoi thong tin benh nhan
     * @param body       noi dung phan in dang "nhan : gia tri"; rong thi in ghi chu chua co du lieu
     * @param table      bang kem theo, co the null
     * @param emptyNote  ghi chu khi khong co du lieu
     * @param footer     dong chan trang (nguoi in, thoi diem in)
     */
    public byte[] render(String title, List<Line> patient, List<Line> body, Table table,
                         String emptyNote, String footer) {
        BaseFont base = loadBaseFont();
        Font h1 = new Font(base, 14, Font.BOLD);
        Font h2 = new Font(base, 11, Font.BOLD);
        Font normal = new Font(base, 10, Font.NORMAL);
        Font small = new Font(base, 8, Font.NORMAL);
        Font italic = new Font(base, 10, Font.ITALIC);

        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph heading = new Paragraph("PHIEU KET QUA — " + title.toUpperCase(), h1);
        heading.setAlignment(Element.ALIGN_CENTER);
        document.add(heading);

        Paragraph sub = new Paragraph(
                "VR-RIS PACS · Du lieu gia phuc vu do an, khong dung cho muc dich lam sang", small);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14f);
        document.add(sub);

        document.add(section("THONG TIN BENH NHAN", h2));
        document.add(lineTable(patient, normal));

        document.add(section(title.toUpperCase(), h2));
        if (body.isEmpty() && (table == null || table.rows().isEmpty())) {
            Paragraph note = new Paragraph(emptyNote, italic);
            note.setSpacingBefore(4f);
            document.add(note);
        } else {
            if (!body.isEmpty()) {
                document.add(lineTable(body, normal));
            }
            if (table != null && !table.rows().isEmpty()) {
                document.add(dataTable(table, normal, h2));
            }
        }

        Paragraph foot = new Paragraph(footer, small);
        foot.setSpacingBefore(22f);
        document.add(foot);

        document.close();
        return out.toByteArray();
    }

    private Paragraph section(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(12f);
        p.setSpacingAfter(4f);
        return p;
    }

    private PdfPTable lineTable(List<Line> lines, Font font) {
        PdfPTable table = new PdfPTable(new float[]{1f, 2.2f});
        table.setWidthPercentage(100);
        for (Line line : lines) {
            table.addCell(borderless(line.label(), font));
            table.addCell(borderless(line.value() == null || line.value().isBlank() ? "—" : line.value(), font));
        }
        return table;
    }

    private PdfPCell borderless(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPaddingBottom(3f);
        return cell;
    }

    private PdfPTable dataTable(Table data, Font normal, Font header) {
        PdfPTable table = new PdfPTable(data.headers().size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        for (String h : data.headers()) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, header));
            cell.setPadding(5f);
            table.addCell(cell);
        }
        for (List<String> row : data.rows()) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Paragraph(value == null ? "—" : value, normal));
                cell.setPadding(5f);
                table.addCell(cell);
            }
        }
        return table;
    }

    private BaseFont loadBaseFont() {
        // Dat app.print.font-path tuc la chi dinh ro font nao - khong am tham dung font khac,
        // vi font sai co the ra phieu mat dau tieng Viet ma khong ai biet.
        List<String> candidates = new ArrayList<>();
        if (configuredFontPath != null && !configuredFontPath.isBlank()) {
            candidates.add(configuredFontPath);
        } else {
            candidates.addAll(List.of(FALLBACK_FONTS));
        }

        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (!Files.isReadable(path)) {
                continue;
            }
            try {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                log.warn("Khong nap duoc font {} cho phieu in: {}", candidate, e.getMessage());
            }
        }

        throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PRINT_FONT_MISSING",
                "May chu chua co font Unicode de dung phieu PDF tieng Viet. "
                        + "Cai font (vi du: apk add ttf-dejavu / apt install fonts-dejavu-core) "
                        + "hoac dat app.print.font-path toi mot file .ttf.");
    }
}
