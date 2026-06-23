package com.example.rental.controller;

import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.RoomRequest;
import com.example.rental.model.Room;
import com.example.rental.model.User;
import com.example.rental.service.InvoiceService;
import com.example.rental.service.RoomService;
import com.example.rental.service.UserService;
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
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Room> createRoom(
            @RequestBody RoomRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Room room = roomService.createRoom(req, user);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody RoomRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        Room room = roomService.updateRoom(id, req, user);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRoom(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findByJwt(jwt);
        roomService.deleteRoom(id, user);
        ApiResponse res = new ApiResponse();
        res.setMessage("Xoa phong tro thanh cong");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        Room room = roomService.findById(id, user);
        return ResponseEntity.ok(room);
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Room> rooms = roomService.findAll(user);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Room>> getAvailableRooms(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Room> rooms = roomService.findByTrangThai(RoomStatus.TRONG, user);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rented")
    public ResponseEntity<List<Room>> getRentedRooms(
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Room> rooms = roomService.findByTrangThai(RoomStatus.DANG_THUE, user);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/motel/{nhaTroId}")
    public ResponseEntity<List<Room>> getRoomsByMotelId(
            @PathVariable Long nhaTroId,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Room> rooms = roomService.findByMotelId(nhaTroId, user);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Room>> searchRooms(
            @RequestParam(required = false) String maPhong,
            @RequestParam(required = false) Long nhaTroId,
            @RequestParam(required = false) RoomStatus trangThai,
            @RequestHeader(value = "Authorization", required = false) String jwt) throws Exception {
        User user = jwt != null ? userService.findByJwt(jwt) : null;
        List<Room> rooms = roomService.search(maPhong, nhaTroId, trangThai, user);
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
