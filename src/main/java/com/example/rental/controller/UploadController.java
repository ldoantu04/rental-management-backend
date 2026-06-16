package com.example.rental.controller;

import com.example.rental.service.utils.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        String url = cloudinaryService.uploadImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/file")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "rental-management") String folder) throws Exception {
        String url = cloudinaryService.uploadRaw(file, folder);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
