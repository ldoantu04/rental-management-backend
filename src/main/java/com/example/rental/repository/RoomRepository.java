package com.example.rental.repository;

import com.example.rental.domain.RoomStatus;
import com.example.rental.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByNhaTroId(Long nhaTroId);
    List<Room> findByTrangThai(RoomStatus trangThai);
    Room findByMaPhongAndNhaTroId(String maPhong, Long nhaTroId);
    List<Room> findAllByOrderByNgayTaoDesc();
}
