package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.LoginRequest;
import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.SignupRequest;
import com.madre.excelweb.api.model.UserInfoResponse;
import com.madre.excelweb.api.model.UserIsAuthenticated;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.jpa.repositories.VerificationTokenRepository;
import com.madre.excelweb.security.jwt.JwtUtils;
import com.madre.excelweb.security.service.UserDetailsImpl;
import com.madre.excelweb.service.api.PsnGameSyncService;
import com.madre.excelweb.service.redis.PendingUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(value = "*", maxAge = 3600)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class SetUpController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JavaMailSender javaMailSender;
    private final PendingUserService pendingUserService;
    private final GameRepository gameRepository;
    private final PsnGameSyncService psnGameSyncService;


    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Attempting to log user with username: {} in", loginRequest.username());

            if (loginRequest.password().length() == 0 || loginRequest.username().length() == 0) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Username/Password cannot be empty"));
            }
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info(authentication.getPrincipal().toString());
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();


            // Want to check if the user is in Redis pending verification
            if (pendingUserService.getPendingUser(userDetails.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR,
                        "Account is currently pending verification, please check the email associated with this account."));
            }

            ResponseCookie cookie = jwtUtils.generateJwtCookie(userDetails);
            log.info(cookie.toString());

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            log.info("User {} logged in successfully", loginRequest.username());
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Incorrect Username or Password"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error logging you in. Please try again later."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        try {
            log.info("Logging user out");
            ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
            log.info("User logged out successfully");
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new MessageResponse(MessageType.SUCCESS, "User Logged Out Successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error Logging You Out. Please try again later."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {

        if (userRepository.getUserByUsername(signupRequest.username()).isPresent() || userRepository.getUserByEmail(signupRequest.username()).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Username/Email Already Taken"));
        }
        if (userRepository.getUserByEmail(signupRequest.email()).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Email Already Associated with an Account"));
        }

        if (signupRequest.password().length() < 8) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Password must be at least 8 characters long"));
        }
        if (signupRequest.username().length() < 4) {
            if (signupRequest.username().length() == 0) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Username cannot be empty"));
            }
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Username must be at least 4 characters long"));
        }

        // Check for accounts already pending  verification in Redis
        if (pendingUserService.getPendingUser(signupRequest.email()).isPresent()) {
            log.warn("Email is already associated with an account pending verification. Please check your email for verification link.");
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR,
                    "Email is already associated with an account pending verification. Please check your email for verification link."));
        }

        AppUser appUser = AppUser.builder().
                username(signupRequest.username())
                .email(signupRequest.email())
                .password(passwordEncoder.encode(signupRequest.password()))
                .build();

        final String token = UUID.randomUUID().toString();
        final EmailVerificationToken emailVerificationToken = EmailVerificationToken.builder()
                .token(token)
                .email(appUser.getEmail())
                .expiryDate(EmailVerificationToken.calculateExpiryDate(EmailVerificationToken.EXPIRATION))
                .build();

        pendingUserService.savePendingUser(appUser);

        verificationTokenRepository.save(emailVerificationToken);

        sendVerificationEmail(appUser.getEmail(), token);

        return ResponseEntity.ok(new MessageResponse(MessageType.SUCCESS, "An email has been sent to " + appUser.getEmail() +
                " for verification. Click on the link sent within the next 24 hours to verify your email."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {

        try {
            final EmailVerificationToken emailVerificationToken = verificationTokenRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("Error: Invalid Link"));

            if (emailVerificationToken == null || emailVerificationToken.isExpired()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Invalid or Expired Link"));
            }

            final Optional<AppUser> possibleUser = pendingUserService.getPendingUser(emailVerificationToken.getEmail());

            if (possibleUser.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "No User Found with this Link"));
            }

            final AppUser user = possibleUser.get();

            try {
                userRepository.save(user);
            } catch (Exception e) {
                log.error("Error saving user", e);
                throw e;
            }
            try {
                verificationTokenRepository.delete(emailVerificationToken);
                pendingUserService.removePendingUser(emailVerificationToken.getEmail());
            } catch (Exception e) {
                userRepository.delete(user);
                log.error("Error deleting verification token", e);
                throw e;
            }

            log.info("Email has been verified and user {} has been successfully registered", user);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://localhost:3000/api/login?verified=true"))
                    .body(new MessageResponse(MessageType.SUCCESS, "Email Verified Successfully"));
        } catch (Exception e) {

            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error Verifying Email, please try again later"));
        }
    }

    @GetMapping("/randomGameData")
    public ResponseEntity<List<GameData>> getRandomGameData(@RequestParam Integer limit) {
        limit = limit == null ? 40 : limit;
        return ResponseEntity.ok(gameRepository.findRandomGames(limit));
    }

    //Want to return isAuthenticated boolean based on whether the current user is logged in/authenticated
    @GetMapping("/userAuthenticationStatus")
    public ResponseEntity<UserIsAuthenticated> getUserAuthenticationStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Checking if user is authenticated");
        try {
            if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
                Object principal = authentication.getPrincipal();
                log.info("Principal: {}", principal);
                if (principal instanceof UserDetailsImpl userDetails) {
                    log.info("User {} is authenticated", userDetails.getUsername());
                    return ResponseEntity.ok(new UserIsAuthenticated(true));
                }
            }
            log.info("User is not authenticated");
            return ResponseEntity.ok(new UserIsAuthenticated(false));
        } catch (Exception e) {
            log.error("Error checking if user is authenticated", e);
            return ResponseEntity.ok(new UserIsAuthenticated(false));
        }

    }

    @PostMapping("/admin/sync")
    public void syncGames() {
        try {
            log.info("Starting manually triggered sync job for PSN games");

            psnGameSyncService.syncGames();
            log.info("Successfully synced all PSN games");
        } catch (Exception e) {
            log.error("Error syncing PSN games", e);
        }


    }


    private void sendVerificationEmail(String email, String token) {
        final String verificationURL = "http://localhost:8081/api/auth/verify-email?token=" + token;
        final SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject("Email Verification: Games DB Network");
        mailMessage.setText("Thank you for registering with us. Please click on the link below to verify your email.\n" + verificationURL);
        javaMailSender.send(mailMessage);

    }
}
