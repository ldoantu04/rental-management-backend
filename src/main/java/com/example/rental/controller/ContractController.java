package com.example.rental.controller;

import com.example.rental.domain.ContractStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.ContractRequest;
import com.example.rental.model.Contract;
import com.example.rental.model.User;
import com.example.rental.service.ContractService;
import com.example.rental.service.UserService;
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
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Contract> createContract(
            @RequestBody ContractRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Contract contract = contractService.createContract(req, user);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(
            @PathVariable Long id,
            @RequestBody ContractRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Contract contract = contractService.updateContract(id, req, user);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelContract(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        String lyDoHuy = body != null ? body.get("lyDoHuy") : null;
        User user = userService.findByJwt(jwt);
        contractService.cancelContract(id, lyDoHuy, user);
        ApiResponse res = new ApiResponse();
        res.setMessage("Huy hop dong thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        Contract contract = contractService.findById(id, user);
        return ResponseEntity.ok(contract);
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Contract> contracts = contractService.findAll(user);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Contract>> getActiveContracts(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Contract> contracts = contractService.findByTrangThai(ContractStatus.DANG_HIEU_LUC, user);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/by-room/{roomId}/active")
    public ResponseEntity<Contract> getActiveContractByRoom(
            @PathVariable Long roomId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        List<Contract> contracts = contractService.findByPhongTroIdAndTrangThai(roomId, ContractStatus.DANG_HIEU_LUC);
        if (contracts == null || contracts.isEmpty()) {
            throw new Exception("Phong tro chua co hop dong hieu luc");
        }
        return ResponseEntity.ok(contracts.get(0));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        Contract contract = contractService.findById(id, user);
        byte[] pdf = contractPdfService.generate(contract);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String safeCode = (contract.getMaHopDong() == null ? "hop-dong" : contract.getMaHopDong()).replaceAll("[^\\w\\-]", "_");
        headers.setContentDispositionFormData("attachment", safeCode + ".pdf");
        headers.setCacheControl("must-revalidate, no-store");
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
