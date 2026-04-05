package com.madre.excelweb.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.madre.excelweb.models.ListGameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "wishlist", indexes = {@Index(name = "wishlist_user_id_index", columnList = "userinfo")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"userinfo", "gameinfo"})})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Wishlist implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "userinfo", referencedColumnName = "id")
    @JsonIgnore
    private AppUser userinfo;

    @ManyToOne
    @JoinColumn(name = "gameinfo", referencedColumnName = "id")
    private GameData gameinfo;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "selected_platform")
    private String selectedPlatform;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ListGameStatus status;

//    Want to integrate Date added and Last Modified Column to the wishlist table

}
