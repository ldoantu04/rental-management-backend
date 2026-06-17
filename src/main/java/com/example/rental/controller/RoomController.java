package com.example.rental.controller;

import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.RoomRequest;
import com.example.rental.model.Room;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody RoomRequest req) throws Exception {
        Room room = roomService.createRoom(req);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody RoomRequest req) throws Exception {
        Room room = roomService.updateRoom(id, req);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRoom(@PathVariable Long id) throws Exception {
        roomService.deleteRoom(id);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa phong tro thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) throws Exception {
        Room room = roomService.findById(id);
        return ResponseEntity.ok(room);
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomService.findAll();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        List<Room> rooms = roomService.findByTrangThai(RoomStatus.TRONG);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rented")
    public ResponseEntity<List<Room>> getRentedRooms() {
        List<Room> rooms = roomService.findByTrangThai(RoomStatus.DANG_THUE);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/motel/{nhaTroId}")
    public ResponseEntity<List<Room>> getRoomsByMotelId(@PathVariable Long nhaTroId) {
        List<Room> rooms = roomService.findByMotelId(nhaTroId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Room>> searchRooms(
            @RequestParam(required = false) String maPhong,
            @RequestParam(required = false) Long nhaTroId,
            @RequestParam(required = false) RoomStatus trangThai) {
        List<Room> rooms = roomService.search(maPhong, nhaTroId, trangThai);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}/people")
    public ResponseEntity<Map<String, Object>> getRoomPeople(@PathVariable Long roomId) {
        int people = invoiceService.countPeopleByRoomId(roomId);
        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("soNguoi", people);
        return ResponseEntity.ok(body);
    }
}
