package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.GameSourceEntry;
import com.madre.excelweb.models.CatalogSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GameSourceEntryRepository extends JpaRepository<GameSourceEntry, Long> {
    List<GameSourceEntry> findByCatalogSourceAndExternalIdIn(CatalogSource catalogSource, Collection<String> externalIds);

    java.util.Optional<GameSourceEntry> findByCatalogSourceAndExternalId(CatalogSource catalogSource, String externalId);

    List<GameSourceEntry> findByGameIdIn(Collection<Long> gameIds);
}
