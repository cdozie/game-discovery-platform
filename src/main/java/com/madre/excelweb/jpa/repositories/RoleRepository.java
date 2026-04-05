package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.AppRole;
import com.madre.excelweb.models.Role;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Table(name = "roles")
public interface RoleRepository extends JpaRepository<AppRole, Long> {
    Optional<AppRole> findByName(Role name);
}
