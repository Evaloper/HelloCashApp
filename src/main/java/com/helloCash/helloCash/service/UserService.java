package com.helloCash.helloCash.service;

import com.helloCash.helloCash.model.UserEntity;
import com.helloCash.helloCash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void saveUser(UserEntity user) {
        userRepository.save(user);
    }
}
