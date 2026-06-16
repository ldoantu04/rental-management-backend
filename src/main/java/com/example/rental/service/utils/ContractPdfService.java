package com.example.rental.service.utils;

import com.example.rental.model.Contract;
import com.example.rental.model.ContractServiceItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ContractPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    public byte[] generate(Contract contract) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(document, out);
        document.open();

        BaseFont base = loadVietnameseFont();
        Font titleFont = new Font(base, 22, Font.BOLD, new Color(128, 0, 28));
        Font brandFont = new Font(base, 14, Font.BOLD, new Color(128, 0, 28));
        Font headingFont = new Font(base, 13, Font.BOLD, new Color(31, 41, 55));
        Font labelFont = new Font(base, 9, Font.BOLD, new Color(156, 163, 175));
        Font valueFont = new Font(base, 11, Font.BOLD, new Color(17, 24, 39));
        Font bodyFont = new Font(base, 10, Font.NORMAL, new Color(75, 85, 99));
        Font smallFont = new Font(base, 9, Font.NORMAL, new Color(107, 114, 128));

        Paragraph brand = new Paragraph("SmartRental", brandFont);
        brand.setAlignment(Element.ALIGN_CENTER);
        document.add(brand);
        Paragraph subtitle = new Paragraph("He thong quan ly nha tro thong minh", smallFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);
        document.add(emptyLine(8));

        Paragraph title = new Paragraph("HOP DONG THUE NHA", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        Paragraph code = new Paragraph("#" + safe(contract.getMaHopDong()), new Font(base, 13, Font.BOLD, new Color(128, 0, 28)));
        code.setAlignment(Element.ALIGN_CENTER);
        document.add(code);
        document.add(emptyLine(10));

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingBefore(4f);
        info.setWidths(new float[]{1f, 1f});
        addInfoCell(info, "Khach thue", safe(contract.getKhachThue() != null ? contract.getKhachThue().getHoTen() : "-"), labelFont, valueFont);
        String roomDisplay = contract.getPhongTro() != null
                ? safe(contract.getPhongTro().getMaPhong())
                + (contract.getPhongTro().getNhaTro() != null ? " - " + safe(contract.getPhongTro().getNhaTro().getTenTro()) : "")
                : "-";
        addInfoCell(info, "Phong", roomDisplay, labelFont, valueFont);
        addInfoCell(info, "Thoi han",
                formatDate(contract.getNgayBatDau()) + " - " + formatDate(contract.getNgayKetThuc()),
                labelFont, valueFont);
        addInfoCell(info, "Thanh toan hang thang", "Ngay " + (contract.getNgayThanhToan() != null ? contract.getNgayThanhToan() : "-"), labelFont, valueFont);
        document.add(info);
        document.add(emptyLine(10));

        PdfPTable price = new PdfPTable(2);
        price.setWidthPercentage(100);
        price.setSpacingBefore(4f);
        price.setWidths(new float[]{1f, 1f});
        addPriceCell(price, "Tien thue hang thang", formatMoney(contract.getGiaThue()), labelFont, valueFont);
        addPriceCell(price, "Tien coc", formatMoney(contract.getTienCoc()), labelFont, valueFont);
        document.add(price);
        document.add(emptyLine(12));

        Paragraph servicesHeader = new Paragraph("DICH VU AP DUNG", headingFont);
        servicesHeader.setSpacingAfter(6f);
        document.add(servicesHeader);

        if (contract.getDanhSachDichVu() != null && !contract.getDanhSachDichVu().isEmpty()) {
            PdfPTable tbl = new PdfPTable(3);
            tbl.setWidthPercentage(100);
            tbl.setWidths(new float[]{2f, 1.4f, 1.2f});
            addHeaderCell(tbl, "Dich vu", base);
            addHeaderCell(tbl, "Kieu tinh", base);
            addHeaderCell(tbl, "Don gia", base);
            for (ContractServiceItem s : contract.getDanhSachDichVu()) {
                addBodyCell(tbl, safe(s.getTenDichVu()), bodyFont, Element.ALIGN_LEFT);
                addBodyCell(tbl, safe(s.getKieuTinh()), bodyFont, Element.ALIGN_LEFT);
                addBodyCell(tbl, formatMoney(s.getDonGia()), bodyFont, Element.ALIGN_RIGHT);
            }
            document.add(tbl);
        } else {
            Paragraph empty = new Paragraph("Chua co dich vu ap dung", smallFont);
            document.add(empty);
        }
        document.add(emptyLine(12));

        Paragraph termsHeader = new Paragraph("DIEU KHOAN HOP DONG", headingFont);
        termsHeader.setSpacingAfter(6f);
        document.add(termsHeader);
        String terms = contract.getDieuKhoan() != null && !contract.getDieuKhoan().isBlank()
                ? contract.getDieuKhoan()
                : "-";
        Paragraph termsBody = new Paragraph(terms, bodyFont);
        document.add(termsBody);
        document.add(emptyLine(30));

        PdfPTable signs = new PdfPTable(2);
        signs.setWidthPercentage(100);
        signs.setWidths(new float[]{1f, 1f});
        addSignCell(signs, "Chu ky khach thue", smallFont);
        addSignCell(signs, "Chu ky ban quan ly", smallFont);
        document.add(signs);

        document.close();
        return out.toByteArray();
    }

    private BaseFont loadVietnameseFont() throws Exception {
        String[] resourceCandidates = {
                "fonts/NotoSans-Bold.ttf",
                "fonts/Roboto-Regular.ttf",
                "fonts/DejaVuSans.ttf"
        };
        for (String path : resourceCandidates) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (!resource.exists()) continue;
                try (InputStream is = resource.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("vnfont-", ".ttf");
                    tmp.deleteOnExit();
                    java.nio.file.Files.write(tmp.toPath(), bytes);
                    return BaseFont.createFont(tmp.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
            }
        }
        String[] systemCandidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/seguisb.ttf"
        };
        for (String path : systemCandidates) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private Paragraph emptyLine(float size) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(size);
        return p;
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBorderColor(new Color(243, 244, 246));
        Paragraph lp = new Paragraph(label.toUpperCase(), labelFont);
        Paragraph vp = new Paragraph(value, valueFont);
        cell.addElement(lp);
        cell.addElement(vp);
        table.addCell(cell);
    }

    private void addPriceCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(243, 244, 246));
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text, BaseFont base) {
        Font f = new Font(base, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(new Color(128, 0, 28));
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8f);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addSignCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(20f);
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        Paragraph line = new Paragraph(" ");
        line.setLeading(40f);
        cell.addElement(line);
        table.addCell(cell);
    }

    private String formatDate(Object value) {
        if (value == null) return "-";
        if (value instanceof java.time.LocalDate d) return d.format(DATE_FMT);
        return value.toString();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0 VND";
        return MONEY_FMT.format(value) + " VND";
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
