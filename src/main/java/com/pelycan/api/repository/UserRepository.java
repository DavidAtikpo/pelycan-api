package com.pelycan.api.repository;

import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByRoleOrderByCreatedAtDesc(UserRole role);

    List<User> findByRoleAndStatusOrderByCreatedAtDesc(UserRole role, String status);

    Optional<User> findByPhoneNumber(String phoneNumber);
}
