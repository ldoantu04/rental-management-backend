package com.example.rental.service;

import com.example.rental.dto.UserRequest;
import com.example.rental.model.User;

import java.util.List;

public interface StaffService {
    User createStaff(UserRequest req) throws Exception;
    User updateStaff(Long id, UserRequest req) throws Exception;
    void deleteStaff(Long id) throws Exception;
    User findStaffById(Long id) throws Exception;
    List<User> findAllStaff();
    List<User> searchStaff(String keyword);
}
