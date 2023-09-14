package com.madre.excelweb.service;

import com.madre.excelweb.api.model.LoginRequest;
import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.service.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;

    public ResponseEntity<MessageResponse> processLoginDetails(LoginRequest loginRequest) {
        final String username = loginRequest.username();
        final String password = loginRequest.password();

        Optional<AppUser> userByUsername = userRepository.getUserByUsername(username);
        Optional<AppUser> userByEmail = userRepository.getUserByEmail(username);

        if (userByUsername.isEmpty() && userByEmail.isEmpty()) {
            log.error("User {} not found", username);
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> user = userByUsername.isEmpty() ? userByEmail : userByUsername;

        if (!PasswordEncoderUtil.matches(password, user.get().getPassword())) {
            log.error("Password for user {} is incorrect", username);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Password is incorrect"));
        }

        log.info("User {} logged in successfully", username);
        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Login successful"));

    }
}
