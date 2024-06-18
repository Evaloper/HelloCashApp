package com.helloCash.helloCash.repository;

import com.helloCash.helloCash.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    UserEntity findByPhoneNumber(String phoneNumber);
}
