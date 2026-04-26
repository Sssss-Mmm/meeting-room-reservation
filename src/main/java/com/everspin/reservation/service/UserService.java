package com.everspin.reservation.service;

import com.everspin.reservation.domain.User;
import com.everspin.reservation.domain.enums.Role;
import com.everspin.reservation.dto.SignupRequest;
import com.everspin.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 시 username 중복 여부 확인
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // 회원가입 시 email 중복 여부 확인
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 회원가입 처리: 비밀번호 인코딩 후 ROLE_USER로 저장
    @Transactional
    public void register(SignupRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .department(request.getDepartment())
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);
    }
}
