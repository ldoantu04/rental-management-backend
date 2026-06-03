package com.example.rental.service.impl;

import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.RoomRequest;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final MotelRepository motelRepository;

    @Override
    public Room createRoom(RoomRequest req) throws Exception {
        Motel motel = motelRepository.findById(req.getMaNhaTro())
                .orElseThrow(() -> new Exception("Khong tim thay nha tro voi id " + req.getMaNhaTro()));

        Room existRoom = roomRepository.findByMaPhongAndNhaTroId(req.getMaPhong(), req.getMaNhaTro());
        if (existRoom != null) {
            throw new Exception("Ma phong da ton tai trong nha tro nay");
        }

        Room room = new Room();
        room.setNhaTro(motel);
        room.setMaPhong(req.getMaPhong());
        room.setDienTich(req.getDienTich());
        room.setGiaThue(req.getGiaThue());
        room.setSoNguoi(req.getSoNguoi());
        room.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : RoomStatus.TRONG);
        room.setGhiChu(req.getGhiChu());
        room.setNgayTao(LocalDateTime.now());
        room.setNgaySua(LocalDateTime.now());

        return roomRepository.save(room);
    }

    @Override
    public Room updateRoom(Long id, RoomRequest req) throws Exception {
        return null;
    }

    @Override
    public void deleteRoom(Long id) throws Exception {

    }

    @Override
    public Room findById(Long id) throws Exception {
        return roomRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay phong tro voi id " + id));
    }

    @Override
    public List<Room> findAll() {
        return List.of();
    }

    @Override
    public List<Room> findByMotelId(Long nhaTroId) {
        return List.of();
    }

    @Override
    public List<Room> search(String maPhong, Long nhaTroId, RoomStatus trangThai) {
        return List.of();
    }
}
