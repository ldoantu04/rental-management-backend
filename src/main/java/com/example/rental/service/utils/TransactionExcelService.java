package com.example.rental.service.utils;

import com.example.rental.domain.PaymentMethod;
import com.example.rental.domain.PaymentStatus;
import com.example.rental.model.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionExcelService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    public byte[] exportTransactions(List<Transaction> transactions) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh sach giao dich");

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle borderStyle = createBorderStyle(workbook, HorizontalAlignment.LEFT);
        CellStyle borderCenterStyle = createBorderStyle(workbook, HorizontalAlignment.CENTER);
        CellStyle moneyTitleStyle = createMoneyTitleStyle(workbook);
        CellStyle moneyCellStyle = createMoneyCellStyle(workbook);

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.setHeight((short) 500);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SACH GIAO DICH");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        Row infoRow = sheet.createRow(rowIndex++);
        infoRow.setHeight((short) 300);
        Cell countCell = infoRow.createCell(0);
        countCell.setCellValue("So luong: " + transactions.size());
        countCell.setCellStyle(borderStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

        Cell totalCell = infoRow.createCell(4);
        BigDecimal tongTien = transactions.stream()
                .filter(t -> t.getTrangThai() == PaymentStatus.THANH_CONG)
                .map(Transaction::getSoTien)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCell.setCellValue("Tong tien: " + MONEY_FMT.format(tongTien) + " VND");
        totalCell.setCellStyle(moneyTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, 7));

        rowIndex++;

        String[] headers = { "STT", "Ma GD", "Khach thue", "Hoa don", "So tien (VND)", "Hinh thuc", "Ngay giao dich", "Ghi chu" };
        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.setHeight((short) 350);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int stt = 1;
        for (Transaction tx : transactions) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeight((short) 300);

            writeTextCell(row, 0, String.valueOf(stt++), borderCenterStyle);
            writeTextCell(row, 1, tx.getMaGiaoDich() != null ? tx.getMaGiaoDich() : "-", borderStyle);
            writeTextCell(row, 2, tenKhachThue(tx), borderStyle);
            writeTextCell(row, 3, tx.getHoaDon() != null && tx.getHoaDon().getMaHoaDon() != null ? tx.getHoaDon().getMaHoaDon() : "-", borderStyle);
            writeMoneyCell(row, 4, tx.getSoTien(), moneyCellStyle);
            writeTextCell(row, 5, hinhThucHienThi(tx.getHinhThucTT()), borderCenterStyle);
            writeTextCell(row, 6, tx.getNgayThanhToan() != null ? tx.getNgayThanhToan().format(DATE_TIME_FMT) : "-", borderCenterStyle);
            writeTextCell(row, 7, tx.getGhiChu() != null ? tx.getGhiChu() : "-", borderStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private String tenKhachThue(Transaction tx) {
        if (tx.getHoaDon() == null || tx.getHoaDon().getHopDong() == null) return "-";
        if (tx.getHoaDon().getHopDong().getKhachThue() == null) return "-";
        return tx.getHoaDon().getHopDong().getKhachThue().getHoTen();
    }

    private String hinhThucHienThi(PaymentMethod method) {
        if (method == null) return "-";
        return switch (method) {
            case TIEN_MAT -> "Tien mat";
            case TRUC_TUYEN -> "Chuyen khoan";
        };
    }

    private void writeTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "-");
        cell.setCellStyle(style);
    }

    private void writeMoneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0);
        }
        cell.setCellStyle(style);
    }

    private CellStyle createBorderStyle(Workbook workbook, HorizontalAlignment align) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createMoneyTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMoneyCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
