//package com.madre.excelweb.jpa.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serializable;
//
//@Entity
//@Table(name = "game_stats")
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//@Data
//public class GameStats implements Serializable {
//    @Id
//    @Basic(optional = false)
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long statsId;
//
//    @OneToOne(mappedBy = "gameStats")
//    private GameData game;
//
//    @Basic(optional = false)
//    @Column(name = "interactions", nullable = false, columnDefinition = "bigint default 0")
//    private Long interactions;
//
//    @Basic(optional = false)
//    @Column(name = "wishlists", nullable = false, columnDefinition = "bigint default 0")
//    private Long wishlists;
//
//    @Basic(optional = false)
//    @Column(name = "comments", nullable = false, columnDefinition = "bigint default 0")
//    private Long comments;
//
//    @Basic(optional = false)
//    @Column(name = "posts", nullable = false, columnDefinition = "bigint default 0")
//    private Long posts;
//
//    @Column(name = "average_rating")
//    private Double averageRating;
//
//
//    @PrePersist
//    public void setDefaultValues() {
//        if (interactions == null) interactions = 0L;
//        if (wishlists == null) wishlists = 0L;
//        if (comments == null) comments = 0L;
//        if (posts == null) posts = 0L;
//    }
//}
