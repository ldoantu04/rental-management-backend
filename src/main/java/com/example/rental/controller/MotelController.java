package com.example.rental.controller;

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
        Motel motel = motelService.createMotel(req, user);
        return ResponseEntity.ok(motel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Motel> updateMotel(
            @PathVariable Long id,
            @RequestBody MotelRequest req) throws Exception {
        Motel motel = motelService.updateMotel(id, req);
        return ResponseEntity.ok(motel);
    }
}
