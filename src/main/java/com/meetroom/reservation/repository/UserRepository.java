package com.meetroom.reservation.repository;

import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // username으로 사용자 조회
    Optional<User> findByUsername(String username);

    // username 중복 확인
    boolean existsByUsername(String username);

    // email 중복 확인
    boolean existsByEmail(String email);

    // 특정 역할의 사용자 목록 조회
    List<User> findByRole(Role role);
}
