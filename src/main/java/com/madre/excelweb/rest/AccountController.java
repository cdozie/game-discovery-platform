package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.UpdatePasswordRequest;
import com.madre.excelweb.api.model.UpdateUserProfileRequest;
import com.madre.excelweb.api.model.dto.UserProfileDto;
import com.madre.excelweb.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(final HttpServletRequest request) {
        return ResponseEntity.ok(accountService.getProfile(ControllerUtils.getAppUserFromRequest(request)));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(final HttpServletRequest request,
                                                        @RequestBody final UpdateUserProfileRequest updateUserProfileRequest) {
        return ResponseEntity.ok(accountService.updateProfile(ControllerUtils.getAppUserFromRequest(request), updateUserProfileRequest));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(final HttpServletRequest request,
                                                          @RequestBody final UpdatePasswordRequest updatePasswordRequest) {
        accountService.updatePassword(ControllerUtils.getAppUserFromRequest(request), updatePasswordRequest);
        return ResponseEntity.ok(new MessageResponse(MessageType.SUCCESS, "Password updated successfully."));
    }
}
