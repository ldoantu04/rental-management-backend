package com.example.rental.service.impl;

import com.example.rental.config.JwtProvider;
import com.example.rental.model.User;
import com.example.rental.repository.UserRepository;
import com.example.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public User findByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("Khong tim thay nguoi dung voi email " + email);
        }
        return user;
    }

    @Override
    public User findByJwt(String jwt) throws Exception {
        String email = jwtProvider.getEmailFromJwtToken(jwt);
        return this.findByEmail(email);
    }
}
