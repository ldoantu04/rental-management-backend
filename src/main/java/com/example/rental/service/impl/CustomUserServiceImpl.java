package com.example.rental.service.impl;

import com.example.rental.domain.UserRole;
import com.example.rental.model.User;
import com.example.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CustomUserServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if (user != null) {
            return buildUserDetails(user.getEmail(), user.getVaiTro());
        }
        throw new UsernameNotFoundException("Khong tim thay nguoi dung voi email - " + username);
    }

    private UserDetails buildUserDetails(String email, UserRole role) {
        if (role == null) {
            role = UserRole.NHAN_VIEN;
        }

        List<GrantedAuthority> authorityList = new ArrayList<>();
        authorityList.add(new SimpleGrantedAuthority(role.toString()));
        return new org.springframework.security.core.userdetails.User(email, "N/A", authorityList);
    }
}
