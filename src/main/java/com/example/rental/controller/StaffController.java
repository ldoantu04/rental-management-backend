package com.example.rental.controller;

import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.UserRequest;
import com.example.rental.model.User;
import com.example.rental.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<User> createStaff(@RequestBody UserRequest req) throws Exception {
        User user = staffService.createStaff(req);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateStaff(
            @PathVariable Long id,
            @RequestBody UserRequest req) throws Exception {
        User user = staffService.updateStaff(id, req);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteStaff(@PathVariable Long id) throws Exception {
        staffService.deleteStaff(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa nhan vien thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getStaffById(@PathVariable Long id) throws Exception {
        User user = staffService.findStaffById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllStaff() {
        List<User> users = staffService.findAllStaff();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchStaff(@RequestParam String keyword) {
        List<User> users = staffService.searchStaff(keyword);
        return ResponseEntity.ok(users);
    }
}
