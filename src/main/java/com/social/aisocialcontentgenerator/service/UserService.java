package com.social.aisocialcontentgenerator.service;

import com.social.aisocialcontentgenerator.entity.User;
import com.social.aisocialcontentgenerator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User createUser(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }


    public User validateUser(String email, String rawPassword) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) throw new RuntimeException("Invalid credentials");
        User user = userOpt.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) throw new RuntimeException("Invalid credentials");
        return user;
    }


    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
