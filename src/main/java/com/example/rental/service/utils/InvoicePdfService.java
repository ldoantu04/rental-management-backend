package com.example.rental.service.utils;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.InvoiceServiceItem;
import com.example.rental.model.Room;
import com.example.rental.model.Tenant;
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
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    public byte[] generate(Invoice invoice) throws Exception {
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

        Paragraph title = new Paragraph("HOA DON TIEN THUE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        Paragraph code = new Paragraph("#" + safe(invoice.getMaHoaDon()), new Font(base, 13, Font.BOLD, new Color(128, 0, 28)));
        code.setAlignment(Element.ALIGN_CENTER);
        document.add(code);
        document.add(emptyLine(10));

        Contract contract = invoice.getHopDong();
        Tenant tenant = contract != null ? contract.getKhachThue() : null;
        Room room = contract != null ? contract.getPhongTro() : null;

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingBefore(4f);
        info.setWidths(new float[]{1f, 1f});
        addInfoCell(info, "Khach thue", safe(tenant != null ? tenant.getHoTen() : null), labelFont, valueFont);
        String roomDisplay = room != null
                ? safe(room.getMaPhong())
                + (room.getNhaTro() != null ? " - " + safe(room.getNhaTro().getTenTro()) : "")
                : "-";
        addInfoCell(info, "Phong", roomDisplay, labelFont, valueFont);
        addInfoCell(info, "Ky hoa don", formatDate(invoice.getKyHoaDon()), labelFont, valueFont);
        addInfoCell(info, "Han thanh toan", formatDate(invoice.getHanThanhToan()), labelFont, valueFont);
        document.add(info);
        document.add(emptyLine(10));

        Paragraph itemsHeader = new Paragraph("CHI TIET HOA DON", headingFont);
        itemsHeader.setSpacingAfter(6f);
        document.add(itemsHeader);

        // Build table with 8 columns: STT, Danh muc, Don vi, Chi so dau, Chi so moi, So luong, Don gia, Thanh tien
        PdfPTable tbl = new PdfPTable(8);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{0.4f, 1.8f, 0.8f, 0.8f, 0.8f, 0.7f, 1.1f, 1.2f});
        addHeaderCell(tbl, "STT", base);
        addHeaderCell(tbl, "Danh muc", base);
        addHeaderCell(tbl, "Don vi", base);
        addHeaderCell(tbl, "Chi so dau", base);
        addHeaderCell(tbl, "Chi so moi", base);
        addHeaderCell(tbl, "So luong", base);
        addHeaderCell(tbl, "Don gia", base);
        addHeaderCell(tbl, "Thanh tien", base);

        // Use the stored danhSachDichVu line items as single source of truth
        List<InvoiceServiceItem> items = invoice.getDanhSachDichVu();
        BigDecimal subtotal = BigDecimal.ZERO;
        int stt = 1;
        if (items != null && !items.isEmpty()) {
            for (InvoiceServiceItem item : items) {
                String name = safe(item.getTenDichVu());
                String unit = safe(item.getKieuTinh());
                BigDecimal qty = item.getSoLuong() != null ? item.getSoLuong() : BigDecimal.ONE;
                BigDecimal price = item.getDonGia() != null ? item.getDonGia() : BigDecimal.ZERO;
                BigDecimal amount = item.getThanhTien() != null ? item.getThanhTien() : qty.multiply(price);
                subtotal = subtotal.add(amount);

                // Chi so: read from loaiDichVu tag instead of name matching
                String chiSoDau = "";
                String chiSoMoi = "";
                String loai = item.getLoaiDichVu();
                if ("DIEN".equals(loai)) {
                    chiSoDau = item.getChiSoDau() != null ? String.valueOf(item.getChiSoDau()) : "";
                    chiSoMoi = item.getChiSoCuoi() != null ? String.valueOf(item.getChiSoCuoi()) : "";
                } else if ("NUOC".equals(loai)) {
                    chiSoDau = item.getChiSoDau() != null ? String.valueOf(item.getChiSoDau()) : "";
                    chiSoMoi = item.getChiSoCuoi() != null ? String.valueOf(item.getChiSoCuoi()) : "";
                }

                addBodyCell(tbl, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addBodyCell(tbl, name, bodyFont, Element.ALIGN_LEFT);
                addBodyCell(tbl, unit, bodyFont, Element.ALIGN_CENTER);
                addBodyCell(tbl, chiSoDau, bodyFont, Element.ALIGN_CENTER);
                addBodyCell(tbl, chiSoMoi, bodyFont, Element.ALIGN_CENTER);
                addBodyCell(tbl, formatNumber(qty), bodyFont, Element.ALIGN_CENTER);
                addBodyCell(tbl, formatMoney(price), bodyFont, Element.ALIGN_RIGHT);
                Font bold = new Font(bodyFont.getBaseFont(), 10, Font.BOLD, new Color(17, 24, 39));
                addBodyCell(tbl, formatMoney(amount), bold, Element.ALIGN_RIGHT);
            }
        }
        document.add(tbl);
        document.add(emptyLine(4));

        // Use subtotal from line items; fall back to invoice.tongTien for legacy data
        if (subtotal.signum() == 0 && invoice.getTongTien() != null) {
            subtotal = invoice.getTongTien();
        }

        // Subtotal
        PdfPTable subtotalTable = new PdfPTable(2);
        subtotalTable.setWidthPercentage(100);
        subtotalTable.setWidths(new float[]{2f, 1f});
        PdfPCell subLabelCell = new PdfPCell(new Phrase("Thanh tien", new Font(base, 10, Font.NORMAL, new Color(75, 85, 99))));
        subLabelCell.setBorder(Rectangle.NO_BORDER);
        subLabelCell.setPadding(4f);
        subtotalTable.addCell(subLabelCell);
        PdfPCell subValueCell = new PdfPCell(new Phrase(formatMoney(subtotal), new Font(base, 10, Font.NORMAL, new Color(75, 85, 99))));
        subValueCell.setBorder(Rectangle.NO_BORDER);
        subValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subValueCell.setPadding(4f);
        subtotalTable.addCell(subValueCell);
        document.add(subtotalTable);

        // Penalty fee
        BigDecimal phiPhat = invoice.getPhiPhat() != null ? invoice.getPhiPhat() : BigDecimal.ZERO;
        BigDecimal tongCong = subtotal.add(phiPhat);
        if (phiPhat.signum() > 0) {
            PdfPTable penaltyTable = new PdfPTable(2);
            penaltyTable.setWidthPercentage(100);
            penaltyTable.setWidths(new float[]{2f, 1f});

            Font penaltyFont = new Font(base, 10, Font.BOLD, new Color(220, 38, 38));
            PdfPCell penaltyLabelCell = new PdfPCell(new Phrase("Phi phat qua han", penaltyFont));
            penaltyLabelCell.setBorder(Rectangle.NO_BORDER);
            penaltyLabelCell.setPadding(4f);
            penaltyTable.addCell(penaltyLabelCell);

            PdfPCell penaltyValueCell = new PdfPCell(new Phrase(formatMoney(phiPhat), penaltyFont));
            penaltyValueCell.setBorder(Rectangle.NO_BORDER);
            penaltyValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            penaltyValueCell.setPadding(4f);
            penaltyTable.addCell(penaltyValueCell);

            document.add(penaltyTable);
            document.add(emptyLine(4));
        }

        // Total row
        PdfPTable total = new PdfPTable(2);
        total.setWidthPercentage(100);
        total.setWidths(new float[]{2f, 1f});
        PdfPCell labelCell = new PdfPCell(new Phrase("TONG CONG", new Font(base, 12, Font.BOLD, new Color(17, 24, 39))));
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(new Color(254, 226, 226));
        labelCell.setBackgroundColor(new Color(254, 242, 242));
        labelCell.setPadding(10f);
        total.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(formatMoney(tongCong), new Font(base, 12, Font.BOLD, new Color(128, 0, 28))));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(new Color(254, 226, 226));
        valueCell.setBackgroundColor(new Color(254, 242, 242));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(10f);
        total.addCell(valueCell);
        document.add(total);
        document.add(emptyLine(8));

        Paragraph status = new Paragraph("Trang thai: " + trangThaiHienThi(invoice.getTrangThai()),
                new Font(base, 11, Font.BOLD, statusColor(invoice.getTrangThai())));
        status.setAlignment(Element.ALIGN_RIGHT);
        document.add(status);

        if (invoice.getGhiChu() != null && !invoice.getGhiChu().isBlank()) {
            document.add(emptyLine(8));
            Paragraph noteHeader = new Paragraph("GHI CHU", headingFont);
            noteHeader.setSpacingAfter(4f);
            document.add(noteHeader);
            document.add(new Paragraph(invoice.getGhiChu(), bodyFont));
        }
        document.add(emptyLine(20));

        PdfPTable signs = new PdfPTable(2);
        signs.setWidthPercentage(100);
        signs.setWidths(new float[]{1f, 1f});
        addSignCell(signs, "Chu ky khach thue", smallFont);
        addSignCell(signs, "Chu ky ban quan ly", smallFont);
        document.add(signs);

        document.close();
        return out.toByteArray();
    }

    private String trangThaiHienThi(InvoiceStatus status) {
        if (status == null) return "-";
        switch (status) {
            case CHUA_THANH_TOAN: return "CHUA THANH TOAN";
            case DA_THANH_TOAN: return "DA THANH TOAN";
            case QUA_HAN: return "QUA HAN";
            default: return status.name();
        }
    }

    private Color statusColor(InvoiceStatus status) {
        if (status == null) return new Color(75, 85, 99);
        switch (status) {
            case CHUA_THANH_TOAN: return new Color(234, 88, 12);
            case DA_THANH_TOAN: return new Color(22, 163, 74);
            case QUA_HAN: return new Color(220, 38, 38);
            default: return new Color(75, 85, 99);
        }
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

    private String formatNumber(BigDecimal value) {
        if (value == null) return "0";
        if (value.stripTrailingZeros().scale() <= 0) {
            return MONEY_FMT.format(value.longValue());
        }
        return MONEY_FMT.format(value);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
