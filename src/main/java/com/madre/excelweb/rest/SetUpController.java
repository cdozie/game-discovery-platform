package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.*;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.security.jwt.JwtUtils;
import com.madre.excelweb.security.service.UserDetailsImpl;
import com.madre.excelweb.service.api.PsnGameSyncService;
import com.madre.excelweb.service.redis.PendingUserService;
import com.madre.excelweb.service.redis.VerificationTokenService;
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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    private final JavaMailSender javaMailSender;
    private final PendingUserService pendingUserService;
    private final VerificationTokenService verificationTokenService;
    private final GameRepository gameRepository;
    private final PsnGameSyncService psnGameSyncService;


    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Attempting to log user with username: {} in", loginRequest.username());

            if (loginRequest.password().isEmpty() || loginRequest.username().isEmpty()) {
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

            log.info("User {} logged in successfully", loginRequest.username());
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
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

        try {

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
                if (signupRequest.username().isEmpty()) {
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

            final AppUser appUser = AppUser.builder().
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

            verificationTokenService.saveVerificationToken(emailVerificationToken);

            pendingUserService.savePendingUser(appUser);


            sendVerificationEmail(appUser.getEmail(), token);

            return ResponseEntity.ok(new MessageResponse(MessageType.SUCCESS, "An email has been sent to " + appUser.getEmail() +
                    " for verification. Click on the link sent within the next 24 hours to verify your email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error Registering User. Please try again later."));
        }
    }

    @GetMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam String email) {
        try {
            log.info("Attempting to resend verification email with new token to {}", email);

            Optional<AppUser> possibleUser = pendingUserService.getPendingUser(email);

            if (possibleUser.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "No Pending User Found with the email +"
                        + email + ". Please re-register"));
            }


            verificationTokenService.removeVerificationToken(email);

            final String token = UUID.randomUUID().toString();
            final EmailVerificationToken emailVerificationToken = EmailVerificationToken.builder()
                    .token(token)
                    .email(email)
                    .expiryDate(EmailVerificationToken.calculateExpiryDate(EmailVerificationToken.EXPIRATION))
                    .build();


            verificationTokenService.saveVerificationToken(emailVerificationToken);

            pendingUserService.savePendingUser(possibleUser.get());

            sendVerificationEmail(email, token);

            return ResponseEntity.ok(new MessageResponse(MessageType.SUCCESS, "An new email has been sent to " + email +
                    " for verification. Click on the link sent within the next 24 hours to verify your email." +
                    " If you do not see the email, please check your spam folder. Click here to resend the email again."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error Resending Verification Email. Please try again later."));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token, @RequestParam String email) {

        try {

            log.info("Attempting to verify email with token {} for email {}", token, email);
            final Optional<EmailVerificationToken> emailVerificationTokenOptional = verificationTokenService.getVerificationToken(email);
            log.info("Retrieved verification token optional {}", emailVerificationTokenOptional);

            if (emailVerificationTokenOptional.isEmpty()) {
                log.error("No verification token found for email {}", email);
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Invalid or Expired Link"));
            }

            final EmailVerificationToken emailVerificationToken = emailVerificationTokenOptional.get();
            log.info("Retrieved verification token {}", emailVerificationToken);

            if (!Objects.equals(emailVerificationToken.getToken(), token)) {
                log.error("Invalid token in link");
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Invalid Token in Link"));
            }

            if (emailVerificationToken.isExpired()) {
                log.error("Expired token in link");
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Invalid or Expired Link"));
            }

            final Optional<AppUser> possibleUser = pendingUserService.getPendingUser(emailVerificationToken.getEmail());

            if (possibleUser.isEmpty()) {
                log.error("No user found with email {}", emailVerificationToken.getEmail());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "No User Found with this Link"));
            }

            final AppUser user = possibleUser.get();

            userRepository.save(user);

            try {
                verificationTokenService.removeVerificationToken(emailVerificationToken.getEmail());
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
            log.error("Error verifying email", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error Verifying Email, please try again later"));
        }
    }

    @GetMapping("/games/random")
    public ResponseEntity<List<GameData>> getRandomGameData(@RequestParam Integer limit) {
        limit = limit == null ? 40 : limit;
        return ResponseEntity.ok(gameRepository.findRandomGames(limit));
    }

    //Want to return isAuthenticated boolean based on whether the current user is logged in/authenticated
    @GetMapping("/user/authentication-status")
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


    private void sendVerificationEmail(final String email, final String token) {
        final String urlFormat = "http://localhost:8081/api/auth/verify-email?token=%s&email=%s";
        final String verificationURL = urlFormat.formatted(token, email);
        final SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject("Email Verification: Games DB Network");
        mailMessage.setText("Thank you for registering with us. Please click on the link below to verify your email.\n" + verificationURL);
        javaMailSender.send(mailMessage);

    }
}
