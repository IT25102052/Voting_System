package com.slit.realityvote.repository;

import com.slit.realityvote.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    java.util.List<User> findByRole(com.slit.realityvote.entity.Role role);

    java.util.List<User> findByStatus(com.slit.realityvote.entity.UserStatus status);

    java.util.Optional<User> findById(Long id);

}
