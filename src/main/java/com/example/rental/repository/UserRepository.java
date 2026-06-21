package com.example.rental.repository;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import com.example.rental.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findBySdt(String sdt);
    User findByUsername(String username);
    List<User> findByCccd(String cccd);
    Optional<User> findById(Long id);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedMotels WHERE u.email = :email")
    User findByEmailWithMotels(@Param("email") String email);

    List<User> findAllByOrderByNgayTaoDesc();

    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedMotels m WHERE m.id = :motelId")
    List<User> findByAssignedMotelId(@Param("motelId") Long motelId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedMotels WHERE " +
           "(:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(u.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:vaiTro IS NULL OR u.vaiTro = :vaiTro) " +
           "AND (:trangThai IS NULL OR u.trangThai = :trangThai)")
    List<User> searchEmployees(
            @Param("keyword") String keyword,
            @Param("vaiTro") UserRole vaiTro,
            @Param("trangThai") UserStatus trangThai);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsBySdt(String sdt);
    boolean existsBySdtAndIdNot(String sdt, Long id);
    boolean existsByCccd(String cccd);
    boolean existsByCccdAndIdNot(String cccd, Long id);
}
