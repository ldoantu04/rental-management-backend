package com.example.rental.repository;

import com.example.rental.domain.NotificationType;
import com.example.rental.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByNguoiDungIdOrderByNgayTaoDesc(Long nguoiDungId);
    List<Notification> findByNguoiDungIdAndLoaiOrderByNgayTaoDesc(Long nguoiDungId, NotificationType loai);
    List<Notification> findByNguoiDungIdAndDaDocOrderByNgayTaoDesc(Long nguoiDungId, Boolean daDoc);
    long countByNguoiDungIdAndDaDoc(Long nguoiDungId, Boolean daDoc);
}
