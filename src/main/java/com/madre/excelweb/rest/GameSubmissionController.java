package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.CreateGameSubmissionRequest;
import com.madre.excelweb.api.model.dto.GameSubmissionDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.service.GameSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog/submissions")
public class GameSubmissionController {
    private final GameSubmissionService gameSubmissionService;

    @PostMapping
    public ResponseEntity<GameSubmissionDto> submitGame(final HttpServletRequest request,
                                                        @RequestBody final CreateGameSubmissionRequest createGameSubmissionRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameSubmissionService.submitGame(appUser, createGameSubmissionRequest));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<GameSubmissionDto>> getMySubmissions(final HttpServletRequest request) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameSubmissionService.getMySubmissions(appUser));
    }

    @PutMapping("/mine/{submissionId}")
    public ResponseEntity<GameSubmissionDto> updateMySubmission(final HttpServletRequest request,
                                                                @PathVariable final String submissionId,
                                                                @RequestBody final CreateGameSubmissionRequest createGameSubmissionRequest) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameSubmissionService.updateMySubmission(appUser, submissionId, createGameSubmissionRequest));
    }

    @PutMapping("/mine/{submissionId}/withdraw")
    public ResponseEntity<GameSubmissionDto> withdrawMySubmission(final HttpServletRequest request,
                                                                  @PathVariable final String submissionId) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameSubmissionService.withdrawMySubmission(appUser, submissionId));
    }
}
