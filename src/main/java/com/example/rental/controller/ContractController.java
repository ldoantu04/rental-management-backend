package com.example.rental.controller;

import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.ContractRequest;
import com.example.rental.model.Contract;
import com.example.rental.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteContract(@PathVariable Long id) throws Exception {
        contractService.deleteContract(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa hop dong thanh cong");
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
}
