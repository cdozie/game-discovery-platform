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
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comments implements Serializable {
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    @Column(name = "content", nullable = false, updatable = false)
    private String content;

    @Basic(optional = false)
    @Column(name = "author", nullable = false, updatable = false)
    private String author;

    @Basic
    @Column(name = "date", nullable = false, updatable = false)
    private String date;

    @Basic
    @Column(name = "post", nullable = false, updatable = false)
    private Long postId;

}
