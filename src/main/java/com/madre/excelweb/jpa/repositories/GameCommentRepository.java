package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.GameComment;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Table(name = "game_comments")
public interface GameCommentRepository extends JpaRepository<GameComment, Long> {

    @Query(value = """
            select id
            from game_comments
            where game_id = :gameId
              and parent_comment_id is null
            order by created_at desc, id desc
            limit :limit offset :offset
            """, nativeQuery = true)
    List<Long> findTopLevelCommentIdsByGameId(@Param("gameId") Long gameId,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Query(value = """
            with recursive thread as (
                select gc.id, gc.parent_comment_id
                from game_comments gc
                where gc.id in (:rootIds)
                union all
                select child.id, child.parent_comment_id
                from game_comments child
                join thread on child.parent_comment_id = thread.id
            )
            select distinct thread.id
            from thread
            """, nativeQuery = true)
    List<Long> findThreadIdsByRootIds(@Param("rootIds") List<Long> rootIds);

    @Query("""
            select distinct comment from GameComment comment
            join fetch comment.author
            left join fetch comment.author.roles
            left join fetch comment.parentComment
            where comment.id in :commentIds
            """)
    List<GameComment> findAllByIdInWithAuthorAndParent(@Param("commentIds") List<Long> commentIds);

    @Query("""
            select distinct comment from GameComment comment
            join fetch comment.author
            left join fetch comment.author.roles
            left join fetch comment.parentComment
            where comment.game.id = :gameId
            order by comment.createdAt asc, comment.id asc
            """)
    List<GameComment> findThreadByGameId(@Param("gameId") Long gameId);

    @Query("""
            select distinct comment from GameComment comment
            join fetch comment.game
            join fetch comment.author
            left join fetch comment.author.roles
            left join fetch comment.parentComment
            where comment.id = :commentId
            """)
    Optional<GameComment> findByIdWithGame(@Param("commentId") Long commentId);

    @Query("""
            select distinct comment from GameComment comment
            join fetch comment.game
            join fetch comment.author
            left join fetch comment.author.roles
            left join fetch comment.parentComment
            where comment.publicId = :publicId
            """)
    Optional<GameComment> findByPublicIdWithGame(@Param("publicId") String publicId);

    boolean existsByParentComment_Id(Long parentCommentId);

    @Query("""
            select count(comment)
            from GameComment comment
            where comment.game.id = :gameId
            """)
    long countByGameId(@Param("gameId") Long gameId);

    @Query("""
            select comment from GameComment comment
            where comment.game.id in :gameIds
            """)
    List<GameComment> findByGameIdIn(@Param("gameIds") List<Long> gameIds);
}
