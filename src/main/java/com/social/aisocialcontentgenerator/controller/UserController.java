package com.social.aisocialcontentgenerator.controller;

import com.social.aisocialcontentgenerator.dto.UserProfileDto;
import com.social.aisocialcontentgenerator.entity.User;
import com.social.aisocialcontentgenerator.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
public class UserController {

    private final UserRepository userRepository;
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/v1/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        Object principal = authentication.getPrincipal();

        // If principal is userId (Long), look up the user
        if (principal instanceof Long userId) {
            Optional<User> u = userRepository.findById(userId);
            if (u.isPresent()) {
                User user = u.get();
                UserProfileDto dto = new UserProfileDto(user.getId(), user.getEmail(), user.getRole());
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
        }

        // If principal is something else (e.g., email string), return it in a structured way
        return ResponseEntity.ok(Map.of("principal", principal));
    }
}


