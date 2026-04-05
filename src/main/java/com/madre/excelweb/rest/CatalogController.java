package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.CatalogSyncRequest;
import com.madre.excelweb.api.model.dto.CatalogDashboardDto;
import com.madre.excelweb.api.model.dto.CatalogDiscoveryDto;
import com.madre.excelweb.api.model.dto.CatalogSyncRunDto;
import com.madre.excelweb.api.model.dto.GameDataInteractionsDto;
import com.madre.excelweb.api.model.dto.GameDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.service.CatalogSyncCoordinator;
import com.madre.excelweb.service.GameCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final GameCatalogService gameCatalogService;
    private final CatalogSyncCoordinator catalogSyncCoordinator;

    @GetMapping("/dashboard")
    public ResponseEntity<CatalogDashboardDto> getDashboard(HttpServletRequest request,
                                                            @RequestParam(required = false) Integer latestLimit,
                                                            @RequestParam(required = false) Long popularLimit) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCatalogService.getDashboard(appUser, latestLimit, popularLimit));
    }

    @GetMapping("/discovery")
    public ResponseEntity<CatalogDiscoveryDto> getDiscovery(HttpServletRequest request) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCatalogService.getDiscovery(appUser));
    }

    @GetMapping("/games/latest")
    public ResponseEntity<List<GameDto>> getLatestGames(HttpServletRequest request,
                                                        @RequestParam(required = false) Integer limit) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCatalogService.getLatestGames(appUser, limit));
    }

    @GetMapping("/games/popular")
    public ResponseEntity<List<GameDataInteractionsDto>> getPopularGames(HttpServletRequest request,
                                                                         @RequestParam(required = false) Long limit,
                                                                         @RequestParam(required = false) Long lastId,
                                                                         @RequestParam(required = false) Long lastInteractions) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCatalogService.getPopularGames(appUser, limit, lastId, lastInteractions));
    }

    @GetMapping("/sync")
    public ResponseEntity<CatalogSyncRunDto> getLatestSyncRun(@RequestParam(required = false) String catalogSource) {
        final CatalogSource resolvedCatalogSource = catalogSource == null || catalogSource.isBlank()
                ? null
                : CatalogSource.fromNullable(catalogSource);
        return ResponseEntity.ok((resolvedCatalogSource == null
                        ? catalogSyncCoordinator.getLatestRun()
                        : catalogSyncCoordinator.getLatestRun(resolvedCatalogSource))
                .map(CatalogSyncRunDto::fromEntity)
                .orElse(null));
    }

    @PostMapping("/sync")
    public ResponseEntity<CatalogSyncRunDto> triggerSync(@RequestBody(required = false) CatalogSyncRequest request) {
        log.info("Received manual catalog sync request");
        return ResponseEntity.accepted().body(CatalogSyncRunDto.fromEntity(catalogSyncCoordinator.triggerSync(request)));
    }
}
