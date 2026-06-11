package com.example.rental.service;

import com.example.rental.dto.MotelRequest;
import com.example.rental.model.Motel;
import com.example.rental.model.User;

import java.util.List;

public interface MotelService {
    Motel createMotel(MotelRequest req, User nguoiTao) throws Exception;
    Motel updateMotel(Long id, MotelRequest req) throws Exception;
    void deleteMotel(Long id) throws Exception;
    Motel findById(Long id) throws Exception;
    List<Motel> findAll();
    List<Motel> search(String keyword);
}
