package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.SystemJob;
import com.madre.excelweb.models.SystemJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemJobRepository extends JpaRepository<SystemJob, Long> {
    Optional<SystemJob> findByPublicId(String publicId);

    List<SystemJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<SystemJob> findAllByStatusOrderByCreatedAtAsc(SystemJobStatus status);
}
