package com.example.rental.service;

import com.example.rental.domain.InvoiceStatus;
import com.example.rental.dto.InvoiceRequest;
import com.example.rental.model.Invoice;

import java.util.List;

public interface InvoiceService {
    Invoice createInvoice(InvoiceRequest req) throws Exception;
    Invoice updateInvoice(Long id, InvoiceRequest req) throws Exception;
    void deleteInvoice(Long id) throws Exception;
    Invoice findById(Long id) throws Exception;
    List<Invoice> findAll();
    List<Invoice> findByHopDongId(Long hopDongId);
    List<Invoice> search(String keyword, InvoiceStatus trangThai);
    Invoice markAsPaid(Long id) throws Exception;
}
