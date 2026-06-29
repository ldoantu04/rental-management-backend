package com.example.rental.service;

import com.example.rental.domain.NotificationType;
import com.example.rental.model.Notification;
import com.example.rental.model.User;

import java.util.List;

public interface NotificationService {

    Notification createNotification(User nguoiDung, NotificationType loai, String tieuDe, String noiDung, Long maThamChieu);

    List<Notification> findByUser(Long nguoiDungId);

    List<Notification> findByUserAndType(Long nguoiDungId, NotificationType loai);

    List<Notification> findUnreadByUser(Long nguoiDungId);

    long countUnreadByUser(Long nguoiDungId);

    Notification markAsRead(Long id) throws Exception;

    void markAllAsRead(Long nguoiDungId) throws Exception;

    void syncContractExpiryNotifications(User nguoiDung);

    void syncOverdueInvoiceNotifications(User nguoiDung);

    void createSystemNotification(User nguoiDung, String tieuDe, String noiDung, Long maThamChieu);

    void notifyInvoiceCreated(User nguoiDung, Long hoaDonId);

    void notifyInvoicePaid(User nguoiDung, Long hoaDonId, String hinhThuc);
}
