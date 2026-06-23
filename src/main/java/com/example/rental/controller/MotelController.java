package com.example.rental.controller;

import com.example.rental.domain.UserRole;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.MotelRequest;
import com.example.rental.model.Motel;
import com.example.rental.model.User;
import com.example.rental.service.MotelService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/motels")
@RequiredArgsConstructor
public class MotelController {

    private final MotelService motelService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Motel> createMotel(
            @RequestBody MotelRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        if (user.getVaiTro() == UserRole.NHAN_VIEN) {
            throw new Exception("Ban khong co quyen tao nha tro");
        }
        Motel motel = motelService.createMotel(req, user);
        return ResponseEntity.ok(motel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Motel> updateMotel(
            @PathVariable Long id,
            @RequestBody MotelRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        if (user.getVaiTro() == UserRole.NHAN_VIEN) {
            throw new Exception("Ban khong co quyen cap nhat nha tro");
        }
        Motel motel = motelService.updateMotel(id, req);
        return ResponseEntity.ok(motel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteMotel(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        if (user.getVaiTro() == UserRole.NHAN_VIEN) {
            throw new Exception("Ban khong co quyen xoa nha tro");
        }
        motelService.deleteMotel(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa nha tro thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Motel> getMotelById(@PathVariable Long id) throws Exception {
        Motel motel = motelService.findById(id);
        return ResponseEntity.ok(motel);
    }

    @GetMapping
    public ResponseEntity<List<Motel>> getAllMotels() {
        List<Motel> motels = motelService.findAll();
        return ResponseEntity.ok(motels);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Motel>> searchMotels(@RequestParam String keyword) {
        List<Motel> motels = motelService.search(keyword);
        return ResponseEntity.ok(motels);
    }
}
