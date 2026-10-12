package com.csci318.accountservice.infrastructure;

import com.csci318.accountservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByUsername(String username);
}
