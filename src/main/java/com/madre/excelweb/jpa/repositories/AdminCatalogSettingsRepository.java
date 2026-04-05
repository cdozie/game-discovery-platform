package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.AdminCatalogSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminCatalogSettingsRepository extends JpaRepository<AdminCatalogSettings, Long> {
}
