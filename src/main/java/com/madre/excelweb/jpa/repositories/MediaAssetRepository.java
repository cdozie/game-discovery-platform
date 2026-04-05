package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByPublicId(String publicId);
}
