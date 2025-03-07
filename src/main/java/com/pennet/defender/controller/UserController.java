package com.pennet.defender.controller;

import com.pennet.defender.config.WebHookConfig;
import com.pennet.defender.model.User;
import com.pennet.defender.repository.UserRepository;
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
    public String changePassword(@RequestParam String username, @RequestParam String newPassword) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            User u = user.get();
            u.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(u);
            return "Password changed successfully";
        } else {
            return "User not found";
        }
    }


}
