package com.madre.excelweb.jpa.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
}
)

public class AppUser implements Serializable {
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    @Column(name = "username", nullable = false, updatable = false)
    private String username;

    @Basic(optional = false)
    @Column(name = "email", nullable = false, updatable = false)
    private String email;


    @Basic(optional = false)
    @Column(name = "password", nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false, updatable = false))
    private Set<AppRole> roles;

//    @ManyToMany
//    @JoinTable(
//            name = "wishlists",
//            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, updatable = false, columnDefinition = "BIGINT"),
//            inverseJoinColumns = @JoinColumn(name = "game_id", referencedColumnName = "id", nullable = false, updatable = false, columnDefinition = "BIGINT"),
//            indexes = {
//                    @Index(columnList = "user_id", name = "index_user_id_on_wishlists"),
//            }
//    )

    @OneToMany(mappedBy = "userinfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Wishlist> wishedGames;
}
