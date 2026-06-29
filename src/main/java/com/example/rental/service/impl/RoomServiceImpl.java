package com.example.rental.service.impl;

import com.example.rental.domain.RoomStatus;
import com.example.rental.dto.RoomRequest;
import com.example.rental.model.Motel;
import com.example.rental.model.Room;
import com.example.rental.model.User;
import com.example.rental.repository.MotelRepository;
import com.example.rental.repository.RoomRepository;
import com.example.rental.service.RoomService;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final MotelRepository motelRepository;
    private final UserService userService;

    @Override
    public Room createRoom(RoomRequest req) throws Exception {
        return createRoom(req, null);
    }

    @Override
    public Room createRoom(RoomRequest req, User nguoiTao) throws Exception {
        if (nguoiTao != null && !userService.isAdmin(nguoiTao)) {
            boolean canAccess = userService.canAccessMotel(nguoiTao, req.getMaNhaTro());
            if (!canAccess) {
                throw new Exception("Ban khong co quyen tao phong tai nha tro nay");
            }
        }

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
        room.setTang(req.getTang());
        room.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : RoomStatus.TRONG);
        room.setGhiChu(req.getGhiChu());
        room.setNgayTao(LocalDateTime.now());
        room.setNgaySua(LocalDateTime.now());

        return roomRepository.save(room);
    }

    @Override
    public Room updateRoom(Long id, RoomRequest req) throws Exception {
        return updateRoom(id, req, null);
    }

    @Override
    public Room updateRoom(Long id, RoomRequest req, User nguoiSua) throws Exception {
        Room room = findById(id, nguoiSua);

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
        if (req.getTang() != null) {
            room.setTang(req.getTang());
        }
        if (req.getTrangThai() != null) {
            room.setTrangThai(req.getTrangThai());
        }
        if (req.getGhiChu() != null) {
            room.setGhiChu(req.getGhiChu());
        }
        if (req.getMaNhaTro() != null) {
            if (nguoiSua != null && !userService.isAdmin(nguoiSua)) {
                boolean canAccessOld = userService.canAccessMotel(nguoiSua, room.getNhaTro().getId());
                boolean canAccessNew = userService.canAccessMotel(nguoiSua, req.getMaNhaTro());
                if (!canAccessOld || !canAccessNew) {
                    throw new Exception("Ban khong co quyen cap nhat phong nay");
                }
            }
            Motel motel = motelRepository.findById(req.getMaNhaTro())
                    .orElseThrow(() -> new Exception("Khong tim thay nha tro voi id " + req.getMaNhaTro()));
            room.setNhaTro(motel);
        }
        room.setNgaySua(LocalDateTime.now());

        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) throws Exception {
        deleteRoom(id, null);
    }

    @Override
    public void deleteRoom(Long id, User nguoiXoa) throws Exception {
        Room room = findById(id, nguoiXoa);

        if (room.getTrangThai() == RoomStatus.DANG_THUE) {
            throw new Exception("Khong the xoa phong dang co nguoi thue");
        }

        roomRepository.delete(room);
    }

    @Override
    public Room findById(Long id) throws Exception {
        return findById(id, null);
    }

    @Override
    public Room findById(Long id, User currentUser) throws Exception {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new Exception("Khong tim thay phong tro voi id " + id));
        if (currentUser != null && !userService.isAdmin(currentUser)) {
            if (!userService.canAccessMotel(currentUser, room.getNhaTro().getId())) {
                throw new Exception("Ban khong co quyen truy cap phong nay");
            }
        }
        return room;
    }

    @Override
    public List<Room> findAll() {
        return roomRepository.findAllByOrderByNgayTaoDesc();
    }

    @Override
    public List<Room> findAll(User currentUser) {
        if (currentUser == null || userService.isAdmin(currentUser)) {
            return findAll();
        }
        Set<Long> allowedMotelIds = userService.getAssignedMotelIds(currentUser);
        return roomRepository.findAllByOrderByNgayTaoDesc().stream()
                .filter(r -> r.getNhaTro() != null && allowedMotelIds.contains(r.getNhaTro().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Room> findByMotelId(Long nhaTroId) {
        return findByMotelId(nhaTroId, null);
    }

    @Override
    public List<Room> findByMotelId(Long nhaTroId, User currentUser) {
        if (currentUser != null && !userService.isAdmin(currentUser)) {
            if (!userService.canAccessMotel(currentUser, nhaTroId)) {
                return List.of();
            }
        }
        return roomRepository.findByNhaTroId(nhaTroId);
    }

    @Override
    public List<Room> findByTrangThai(RoomStatus trangThai) {
<<<<<<< HEAD
        return roomRepository.findByTrangThai(trangThai);
=======
        return findByTrangThai(trangThai, null);
    }

    @Override
    public List<Room> findByTrangThai(RoomStatus trangThai, User currentUser) {
        return findAll(currentUser).stream()
                .filter(r -> trangThai == null || r.getTrangThai() == trangThai)
                .collect(Collectors.toList());
>>>>>>> 033bebc3c7b21b80e66cb91c0a8b6db61c375b4b
    }

    @Override
    public List<Room> search(String maPhong, Long nhaTroId, RoomStatus trangThai) {
        return search(maPhong, nhaTroId, trangThai, null);
    }

    @Override
    public List<Room> search(String maPhong, Long nhaTroId, RoomStatus trangThai, User currentUser) {
        List<Room> rooms = findAll(currentUser);

        return rooms.stream()
                .filter(r -> maPhong == null || r.getMaPhong().toLowerCase().contains(maPhong.toLowerCase()))
                .filter(r -> nhaTroId == null || (r.getNhaTro() != null && r.getNhaTro().getId().equals(nhaTroId)))
                .filter(r -> trangThai == null || r.getTrangThai() == trangThai)
                .collect(Collectors.toList());
    }
}
