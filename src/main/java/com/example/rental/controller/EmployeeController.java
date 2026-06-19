package com.example.rental.controller;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.EmployeeHistoryResponse;
import com.example.rental.dto.EmployeeRequest;
import com.example.rental.dto.EmployeeResponse;
import com.example.rental.model.User;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees(
            @RequestHeader(value = "Authorization", required = false) String jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole vaiTro,
            @RequestParam(required = false) UserStatus trangThai) throws Exception {
        List<EmployeeResponse> list;
        if (keyword == null && vaiTro == null && trangThai == null) {
            list = userService.getAllEmployees();
        } else {
            list = userService.searchEmployees(keyword, vaiTro, trangThai);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) throws Exception {
        EmployeeResponse emp = userService.getEmployeeById(id);
        return ResponseEntity.ok(emp);
    }

    @GetMapping("/{id}/motels")
    public ResponseEntity<List<EmployeeResponse.MotelSimple>> getEmployeeMotels(@PathVariable Long id) throws Exception {
        List<EmployeeResponse.MotelSimple> motels = userService.getAvailableMotelsForEmployee(id);
        return ResponseEntity.ok(motels);
    }

    @GetMapping("/motels/all")
    public ResponseEntity<List<EmployeeResponse.MotelSimple>> getAllMotels(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        if (jwt != null) {
            User u = userService.findByJwt(jwt);
            if (u.getVaiTro() == UserRole.NHAN_VIEN) {
                throw new Exception("Ban khong co quyen truy cap");
            }
        }
        List<EmployeeResponse.MotelSimple> motels = userService.getAvailableMotelsForEmployee(null);
        return ResponseEntity.ok(motels);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<EmployeeHistoryResponse>> getEmployeeHistory(@PathVariable Long id) throws Exception {
        userService.getEmployeeById(id);
        List<EmployeeHistoryResponse> history = List.of(
                createHistory("TAO_TAI_KHOAN", "Tao tai khoan nhan vien", "Admin"),
                createHistory("CAP_NHAT_THONG_TIN", "Cap nhat thong tin nhan vien", "Admin")
        );
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @RequestBody EmployeeRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User currentUser = userService.findByJwt(jwt);
        EmployeeResponse emp = userService.createEmployee(req, currentUser);
        return ResponseEntity.ok(emp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @RequestBody EmployeeRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User currentUser = userService.findByJwt(jwt);
        EmployeeResponse emp = userService.updateEmployee(id, req, currentUser);
        return ResponseEntity.ok(emp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User currentUser = userService.findByJwt(jwt);
        if (currentUser.getVaiTro() == UserRole.NHAN_VIEN) {
            throw new Exception("Ban khong co quyen xoa nhan vien");
        }
        userService.deleteEmployee(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa nhan vien thanh cong");
        return ResponseEntity.ok(res);
    }

    private EmployeeHistoryResponse createHistory(String action, String detail, String role) {
        EmployeeHistoryResponse r = new EmployeeHistoryResponse();
        r.setHanhDong(action);
        r.setChiTiet(detail);
        r.setVaiTro(role);
        r.setThoiGian(java.time.LocalDateTime.now().toString());
        return r;
    }
}
