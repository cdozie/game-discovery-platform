package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.AppUser;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Table(name = "users")
public interface UserRepository extends JpaRepository<AppUser, Long> {
    @Query(value = "SELECT * FROM users ", nativeQuery = true)
    List<AppUser> listUsers();

    @Query(value = "SELECT * FROM users WHERE username = :username LIMIT 1", nativeQuery = true)
    Optional<AppUser> getUserByUsername(@Param("username") String username);

    @Query(value = "SELECT * FROM users WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<AppUser> getUserByEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE id = :id LIMIT 1", nativeQuery = true)
    Optional<AppUser> getUserById(@Param("id") Long id);
}
