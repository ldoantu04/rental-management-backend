package com.example.rental.service;

import com.example.rental.model.User;

public interface UserService {
    User findByEmail(String email) throws Exception;
    User findByJwt(String jwt) throws Exception;
}
