package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.CatalogSyncRun;
import com.madre.excelweb.models.CatalogSyncStatus;
import com.madre.excelweb.models.CatalogSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogSyncRunRepository extends JpaRepository<CatalogSyncRun, Long> {
    Optional<CatalogSyncRun> findByPublicId(String publicId);

    Optional<CatalogSyncRun> findTopByOrderByStartedAtDesc();

    Optional<CatalogSyncRun> findTopByStatusOrderByStartedAtDesc(CatalogSyncStatus status);

    Optional<CatalogSyncRun> findTopByCatalogSourceOrderByStartedAtDesc(CatalogSource catalogSource);

    Optional<CatalogSyncRun> findTopByCatalogSourceAndStatusOrderByStartedAtDesc(CatalogSource catalogSource,
                                                                                 CatalogSyncStatus status);

    List<CatalogSyncRun> findAllByStatusOrderByStartedAtAsc(CatalogSyncStatus status);
}
