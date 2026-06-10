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
import java.util.stream.Collectors;

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
        Room room = findById(id);

        if (req.getMaPhong() != null) {
            room.setMaPhong(req.getMaPhong());
        }
        if (req.getDienTich() != null) {
            room.setDienTich(req.getDienTich());
        }
        if (req.getGiaThue() != null) {
            room.setGiaThue(req.getGiaThue());
        }
        if (req.getSoNguoi() != null) {
            room.setSoNguoi(req.getSoNguoi());
        }
        if (req.getTrangThai() != null) {
            room.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            room.setGhiChu(req.getGhiChu());
        }
        if (req.getMaNhaTro() != null) {
            Motel motel = motelRepository.findById(req.getMaNhaTro())
                    .orElseThrow(() -> new Exception("Khong tim thay nha tro voi id " + req.getMaNhaTro()));
            room.setNhaTro(motel);
        }
        room.setNgaySua(LocalDateTime.now());

        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) throws Exception {
        Room room = findById(id);

        if (room.getTrangThai() == RoomStatus.DANG_THUE) {
            throw new Exception("Khong the xoa phong dang co nguoi thue");
        }

        roomRepository.delete(room);
    }

    @Override
    public Room findById(Long id) throws Exception {
        return roomRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay phong tro voi id " + id));
    }

    @Override
    public List<Room> findAll() {
        return roomRepository.findAllByOrderByNgayTaoDesc();
    }

    @Override
    public List<Room> findByMotelId(Long nhaTroId) {
        return roomRepository.findByNhaTroId(nhaTroId);
    }

    @Override
    public List<Room> search(String maPhong, Long nhaTroId, RoomStatus trangThai) {
        List<Room> rooms = roomRepository.findAll();

        return rooms.stream()
                .filter(r -> maPhong == null || r.getMaPhong().toLowerCase().contains(maPhong.toLowerCase()))
                .filter(r -> nhaTroId == null || r.getNhaTro().getId().equals(nhaTroId))
                .filter(r -> trangThai == null || r.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }
}
