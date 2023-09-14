package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.AppRole;
import com.madre.excelweb.models.Role;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Table(name = "roles")
public interface RoleRepository extends JpaRepository<AppRole, Long> {

    @Query(value = "SELECT * from roles WHERE role = :name LIMIT 1", nativeQuery = true)
    Optional<AppRole> findByName(Role name);


}
