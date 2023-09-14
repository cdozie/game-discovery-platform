package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.Comments;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Table(name = "comments")
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    @Query(value = "SELECT * FROM comments WHERE id = :id LIMIT 1", nativeQuery = true)
    Optional<Comments> findCommentById(@Param("id") Long id);

    @Query(value = "SELECT * FROM comments WHERE id IN :ids", nativeQuery = true)
    List<Comments> findCommentsByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM comments WHERE post_id = :id", nativeQuery = true)
    List<Comments> findCommentsByPostId(@Param("id") Long id);

    @Query(value = "SELECT * FROM comments WHERE user_id = :id", nativeQuery = true)
    List<Comments> findCommentsByUserId(@Param("id") Long id);

    @Query(value = "SELECT * FROM comments WHERE content like  :content", nativeQuery = true)
    List<Comments> findCommentsByContent(@Param("content") String content);
}
