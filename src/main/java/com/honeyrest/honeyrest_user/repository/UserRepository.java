package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findBySocialTypeAndSocialId(String socialType, String socialId);
}
