package com.example.rental.service;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceService {
    Invoice createInvoice(InvoiceRequest req) throws Exception;
    Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception;
    void deleteInvoice(Long id) throws Exception;
    Invoice findById(Long id) throws Exception;
    Invoice findByMaHoaDon(String maHoaDon) throws Exception;
    List<Invoice> findAll();
    List<Invoice> findByHopDongId(Long hopDongId);
    List<Invoice> findByTrangThai(InvoiceStatus trangThai);
    List<Invoice> search(String keyword, InvoiceStatus trangThai);
    Invoice markAsPaid(Long id) throws Exception;
    Invoice findLatestByHopDongId(Long hopDongId);
    BigDecimal computeElectricAmount(Contract contract, InvoiceRequest req);
    BigDecimal computeWaterAmount(Contract contract, InvoiceRequest req);
    BigDecimal computeServiceAmount(InvoiceRequest req);
    BigDecimal computeTotal(Contract contract, InvoiceRequest req);
    int countPeopleByRoomId(Long roomId);
}
