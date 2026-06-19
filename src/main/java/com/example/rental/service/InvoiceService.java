package com.example.rental.service;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.Invoice;
import com.example.rental.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceService {
    Invoice createInvoice(InvoiceRequest req) throws Exception;
    Invoice createInvoice(InvoiceRequest req, User nguoiTao) throws Exception;
    Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception;
    Invoice updateInvoice(Long id, InvoiceRequest req, User nguoiSua) throws Exception;
    void deleteInvoice(Long id) throws Exception;
    void deleteInvoice(Long id, User currentUser) throws Exception;
    Invoice findById(Long id) throws Exception;
    Invoice findById(Long id, User currentUser) throws Exception;
    Invoice findByMaHoaDon(String maHoaDon) throws Exception;
    List<Invoice> findAll();
    List<Invoice> findAll(User currentUser);
    List<Invoice> findByHopDongId(Long hopDongId);
    List<Invoice> findByHopDongId(Long hopDongId, User currentUser);
    List<Invoice> findByTrangThai(InvoiceStatus trangThai);
    List<Invoice> findByTrangThai(InvoiceStatus trangThai, User currentUser);
    List<Invoice> search(String keyword, InvoiceStatus trangThai);
    List<Invoice> search(String keyword, InvoiceStatus trangThai, User currentUser);
    Invoice markAsPaid(Long id) throws Exception;
    Invoice markAsPaid(Long id, User nguoiThanhToan) throws Exception;
    Invoice findLatestByHopDongId(Long hopDongId);
    BigDecimal computeElectricAmount(Contract contract, InvoiceRequest req);
    BigDecimal computeWaterAmount(Contract contract, InvoiceRequest req);
    BigDecimal computeServiceAmount(InvoiceRequest req);
    BigDecimal computeTotal(Contract contract, InvoiceRequest req);
    int countPeopleByRoomId(Long roomId);
}
