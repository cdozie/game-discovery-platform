package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.AdminBanUserRequest;
import com.madre.excelweb.api.model.AdminGameRequest;
import com.madre.excelweb.api.model.CreateSystemJobRequest;
import com.madre.excelweb.api.model.CreateGameSubmissionRequest;
import com.madre.excelweb.api.model.ReviewGameSubmissionRequest;
import com.madre.excelweb.api.model.dto.AdminAuditLogDto;
import com.madre.excelweb.api.model.dto.AdminDashboardDto;
import com.madre.excelweb.api.model.dto.AdminGameDto;
import com.madre.excelweb.api.model.dto.AdminUserDto;
import com.madre.excelweb.api.model.dto.GameSubmissionDto;
import com.madre.excelweb.api.model.dto.OffsetPageDto;
import com.madre.excelweb.api.model.dto.SystemJobDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.service.AdminService;
import com.madre.excelweb.service.GameSubmissionService;
import com.madre.excelweb.service.SystemJobService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final GameSubmissionService gameSubmissionService;
    private final SystemJobService systemJobService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDto> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/submissions")
    public ResponseEntity<OffsetPageDto<GameSubmissionDto>> getSubmissions(@RequestParam(required = false) final String status,
                                                                           @RequestParam(required = false) final Integer limit,
                                                                           @RequestParam(required = false) final Integer offset) {
        return ResponseEntity.ok(gameSubmissionService.getSubmissions(status, limit, offset));
    }

    @PostMapping("/submissions/{submissionId}/review")
    public ResponseEntity<GameSubmissionDto> reviewSubmission(final HttpServletRequest request,
                                                              @PathVariable final String submissionId,
                                                              @RequestBody final ReviewGameSubmissionRequest reviewGameSubmissionRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameSubmissionService.reviewSubmission(appUser, submissionId, reviewGameSubmissionRequest));
    }

    @PutMapping("/submissions/{submissionId}")
    public ResponseEntity<GameSubmissionDto> updateSubmission(final HttpServletRequest request,
                                                              @PathVariable final String submissionId,
                                                              @RequestBody final CreateGameSubmissionRequest createGameSubmissionRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameSubmissionService.updateSubmissionAsAdmin(appUser, submissionId, createGameSubmissionRequest));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getUsers(@RequestParam(required = false) final String query,
                                                       @RequestParam(required = false, defaultValue = "false") final boolean onlyBanned) {
        return ResponseEntity.ok(adminService.getUsers(query, onlyBanned));
    }

    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<AdminUserDto> banUser(final HttpServletRequest request,
                                                @PathVariable final String userId,
                                                @RequestBody final AdminBanUserRequest adminBanUserRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(adminService.banUser(appUser, userId, adminBanUserRequest));
    }

    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<AdminUserDto> unbanUser(final HttpServletRequest request,
                                                  @PathVariable final String userId) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(adminService.unbanUser(appUser, userId));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AdminAuditLogDto>> getAuditLogs(@RequestParam(required = false) final Integer limit) {
        return ResponseEntity.ok(adminService.getAuditLogs(limit));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<SystemJobDto>> getSystemJobs(@RequestParam(required = false) final Integer limit) {
        return ResponseEntity.ok(systemJobService.getRecentJobs(limit));
    }

    @PostMapping("/jobs")
    public ResponseEntity<SystemJobDto> queueSystemJob(final HttpServletRequest request,
                                                       @RequestBody final CreateSystemJobRequest createSystemJobRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.accepted().body(systemJobService.queueJob(appUser, createSystemJobRequest));
    }

    @GetMapping("/games")
    public ResponseEntity<List<AdminGameDto>> getGames(@RequestParam(required = false) final String query,
                                                       @RequestParam(required = false) final Integer limit) {
        return ResponseEntity.ok(adminService.getGames(query, limit));
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<AdminGameDto> getGame(@PathVariable final String gameId) {
        return ResponseEntity.ok(adminService.getGame(gameId));
    }

    @PostMapping("/games")
    public ResponseEntity<AdminGameDto> createGame(final HttpServletRequest request,
                                                   @RequestBody final AdminGameRequest adminGameRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(adminService.createGame(appUser, adminGameRequest));
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<AdminGameDto> updateGame(final HttpServletRequest request,
                                                   @PathVariable final String gameId,
                                                   @RequestBody final AdminGameRequest adminGameRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(adminService.updateGame(appUser, gameId, adminGameRequest));
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Void> deleteGame(final HttpServletRequest request,
                                           @PathVariable final String gameId) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        adminService.deleteGame(appUser, gameId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/spotlight/{gameId}")
    public ResponseEntity<AdminDashboardDto> setSpotlightGame(final HttpServletRequest request,
                                                              @PathVariable final String gameId) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(adminService.setSpotlightGame(appUser, gameId));
    }

    @DeleteMapping("/spotlight")
    public ResponseEntity<AdminDashboardDto> clearSpotlightGame(final HttpServletRequest request) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(adminService.clearSpotlightGame(appUser));
    }
}
