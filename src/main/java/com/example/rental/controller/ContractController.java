package com.example.rental.controller;

import com.example.rental.domain.ContractStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.ContractRequest;
import com.example.rental.model.Contract;
import com.example.rental.service.ContractService;
import com.example.rental.service.utils.ContractPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractPdfService contractPdfService;

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody ContractRequest req) throws Exception {
        Contract contract = contractService.createContract(req);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(
            @PathVariable Long id,
            @RequestBody ContractRequest req) throws Exception {
        Contract contract = contractService.updateContract(id, req);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelContract(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) throws Exception {
        String lyDoHuy = body != null ? body.get("lyDoHuy") : null;
        contractService.cancelContract(id, lyDoHuy);
        ApiResponse res = new ApiResponse();
        res.setMessage("Huy hop dong thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) throws Exception {
        Contract contract = contractService.findById(id);
        return ResponseEntity.ok(contract);
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        List<Contract> contracts = contractService.findAll();
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Contract>> getActiveContracts() {
        List<Contract> contracts = contractService.findByTrangThai(ContractStatus.DANG_HIEU_LUC);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/by-room/{roomId}/active")
    public ResponseEntity<Contract> getActiveContractByRoom(@PathVariable Long roomId) throws Exception {
        List<Contract> contracts = contractService.findByPhongTroIdAndTrangThai(roomId, ContractStatus.DANG_HIEU_LUC);
        if (contracts == null || contracts.isEmpty()) {
            throw new Exception("Phong tro chua co hop dong hieu luc");
        }
        return ResponseEntity.ok(contracts.get(0));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(@PathVariable Long id) throws Exception {
        Contract contract = contractService.findById(id);
        byte[] pdf = contractPdfService.generate(contract);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String safeCode = (contract.getMaHopDong() == null ? "hop-dong" : contract.getMaHopDong()).replaceAll("[^\\w\\-]", "_");
        headers.setContentDispositionFormData("attachment", safeCode + ".pdf");
        headers.setCacheControl("must-revalidate, no-store");
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
