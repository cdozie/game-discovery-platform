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
    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            order by user.createdAt desc
            """)
    List<AppUser> listUsers();

    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            where user.username = :username
            """)
    Optional<AppUser> getUserByUsername(@Param("username") String username);

    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            where user.email = :email
            """)
    Optional<AppUser> getUserByEmail(@Param("email") String email);

    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            where user.publicId = :publicId
            """)
    Optional<AppUser> getUserByPublicId(@Param("publicId") String publicId);

    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            where user.id = :id
            """)
    Optional<AppUser> getUserById(@Param("id") Long id);

    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            order by user.createdAt desc
            """)
    List<AppUser> findAllByOrderByCreatedAtDesc();
}
