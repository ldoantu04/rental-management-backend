package com.example.rental.service;

import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.RoomRequest;
import com.example.rental.model.Room;

import java.util.List;

public interface RoomService {
    Room createRoom(RoomRequest req) throws Exception;
    Room updateRoom(Long id, RoomRequest req) throws Exception;
    void deleteRoom(Long id) throws Exception;
    Room findById(Long id) throws Exception;
    List<Room> findAll();
    List<Room> findByMotelId(Long nhaTroId);
    List<Room> findByTrangThai(RoomStatus trangThai);
    List<Room> search(String maPhong, Long nhaTroId, RoomStatus trangThai);
}
