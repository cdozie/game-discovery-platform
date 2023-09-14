package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.ListGameStatus;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    private AppUser userinfo;

    @ManyToOne
    @JoinColumn(name = "gameinfo", referencedColumnName = "id")
    private GameData gameinfo;

    @Column(name = "rating")
    private Double rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ListGameStatus status;


}
