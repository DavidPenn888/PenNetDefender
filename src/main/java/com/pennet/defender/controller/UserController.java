package com.pennet.defender.controller;

import com.pennet.defender.config.WebHookConfig;
import com.pennet.defender.model.User;
import com.pennet.defender.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/server")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/changepsw")
    public ResponseEntity<String> changePassword(
            @RequestParam String username,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            User u = user.get();

            // 校验原密码是否正确
            if (!passwordEncoder.matches(oldPassword, u.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Original password is incorrect");
            }

            // 更新新密码
            u.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(u);

            return ResponseEntity.ok("Password changed successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }



}
