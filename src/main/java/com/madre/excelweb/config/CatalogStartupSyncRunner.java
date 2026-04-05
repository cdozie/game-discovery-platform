package com.madre.excelweb.config;

import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.service.CatalogSyncCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogStartupSyncRunner {
    @Value("${web.app.psn-sync.bootstrap-on-empty-catalog:false}")
    private boolean bootstrapOnEmptyCatalog;

    private final GameRepository gameRepository;
    private final CatalogSyncCoordinator catalogSyncCoordinator;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapCatalogIfNeeded() {
        if (!bootstrapOnEmptyCatalog) {
            return;
        }

        if (gameRepository.count() > 0) {
            log.info("Skipping startup catalog sync because games already exist");
            return;
        }

        log.info("Game catalog is empty. Triggering startup sync.");
        catalogSyncCoordinator.triggerStartupSyncsIfConfigured();
    }
}
