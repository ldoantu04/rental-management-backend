package com.example.rental.service;

import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.RoomRequest;
import com.example.rental.model.Room;
import com.example.rental.model.User;

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

    Room createRoom(RoomRequest req, User nguoiTao) throws Exception;
    Room updateRoom(Long id, RoomRequest req, User nguoiSua) throws Exception;
    void deleteRoom(Long id, User nguoiXoa) throws Exception;
    List<Room> findAll(User currentUser);
    List<Room> findByMotelId(Long nhaTroId, User currentUser);
    List<Room> findByTrangThai(RoomStatus trangThai, User currentUser);
    List<Room> search(String maPhong, Long nhaTroId, RoomStatus trangThai, User currentUser);
    Room findById(Long id, User currentUser) throws Exception;
}
