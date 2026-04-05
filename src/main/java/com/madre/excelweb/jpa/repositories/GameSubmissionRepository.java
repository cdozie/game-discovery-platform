package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.GameSubmission;
import com.madre.excelweb.models.GameSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameSubmissionRepository extends JpaRepository<GameSubmission, Long> {
    java.util.Optional<GameSubmission> findByPublicId(String publicId);

    List<GameSubmission> findByRequestedByUserIdOrderByCreatedAtDesc(Long requestedByUserId);

    List<GameSubmission> findByStatusOrderByCreatedAtDesc(GameSubmissionStatus status);

    List<GameSubmission> findAllByOrderByCreatedAtDesc();

    long countByStatus(GameSubmissionStatus status);

    @Query(value = """
            select id
            from game_submissions
            order by created_at desc, id desc
            limit :limit offset :offset
            """, nativeQuery = true)
    List<Long> findPageIdsOrderByCreatedAtDesc(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            select id
            from game_submissions
            where status = :status
            order by created_at desc, id desc
            limit :limit offset :offset
            """, nativeQuery = true)
    List<Long> findPageIdsByStatusOrderByCreatedAtDesc(@Param("status") String status,
                                                       @Param("limit") int limit,
                                                       @Param("offset") int offset);

    @Query("""
            select submission
            from GameSubmission submission
            left join fetch submission.requestedByUser
            left join fetch submission.reviewedByUser
            left join fetch submission.createdGame
            where submission.id in :submissionIds
            """)
    List<GameSubmission> findAllByIdInWithDetails(@Param("submissionIds") List<Long> submissionIds);
}
