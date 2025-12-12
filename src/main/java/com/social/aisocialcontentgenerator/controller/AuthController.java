package com.social.aisocialcontentgenerator.controller;

import com.social.aisocialcontentgenerator.dto.AuthRequest;
import com.social.aisocialcontentgenerator.dto.AuthResponse;
import com.social.aisocialcontentgenerator.dto.SignupRequest;
import com.social.aisocialcontentgenerator.entity.User;
import com.social.aisocialcontentgenerator.service.UserService;
import com.social.aisocialcontentgenerator.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new AuthResponse("Passwords do not match"));
        }
        if (userService.emailExists(req.getEmail())) {
            return ResponseEntity.badRequest().body(new AuthResponse("Email already in use"));
        }
        User user = userService.createUser(req.getEmail(), req.getPassword());
        String token = jwtUtils.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        User user = userService.validateUser(req.getEmail(), req.getPassword());
        String token = jwtUtils.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
