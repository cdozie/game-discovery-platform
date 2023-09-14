package com.madre.excelweb.jpa.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "posts")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Posts implements Serializable {
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    @Column(name = "title", nullable = false, updatable = false)
    private String title;

    @Basic(optional = false)
    @Column(name = "content", nullable = false, updatable = false)
    private String content;

    @Basic(optional = false)
    @Column(name = "author", nullable = false, updatable = false)
    private String author;

    @Basic(optional = false)
    @Column(name = "date", nullable = false, updatable = false)
    private String date;

    @Basic(optional = false)
    @Column(name = "game", nullable = false, updatable = false)
    private Long gameId;

}
