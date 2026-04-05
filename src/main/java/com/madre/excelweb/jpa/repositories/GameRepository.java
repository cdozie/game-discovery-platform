package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.GameData;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Table(name = "games")
public interface GameRepository extends JpaRepository<GameData, Long> {

    Double SIMILARITY_THRESHOLD = .3;
    String GAME_SEARCH_DOCUMENT_SQL = """
            (
                setweight(to_tsvector('english', coalesce(g.name, '')), 'A') ||
                setweight(to_tsvector('english', coalesce(g.provider_name, '')), 'B') ||
                setweight(to_tsvector('english', coalesce(g.categories, '')), 'C') ||
                setweight(to_tsvector('english', coalesce(g.platforms, '')), 'C') ||
                setweight(to_tsvector('english', coalesce(g.description, '')), 'D')
            )
            """;
    String SEARCH_BY_QUERY_SQL = """
            WITH input AS (
                SELECT CAST(:query AS TEXT) AS raw_query,
                       CAST(:likeQuery AS TEXT) AS like_query,
                       CASE
                           WHEN BTRIM(CAST(:query AS TEXT)) = '' THEN false
                           ELSE true
                       END AS has_query,
                       CASE
                           WHEN BTRIM(CAST(:query AS TEXT)) = '' THEN NULL
                           ELSE websearch_to_tsquery('english', CAST(:query AS TEXT))
                       END AS ts_query
            ),
            ranked AS (
                SELECT g.id,
                       CASE
                           WHEN NOT input.has_query THEN 0
                           WHEN g.name ILIKE input.like_query ESCAPE '\\' THEN 400
                           WHEN g.name ILIKE CONCAT(input.like_query, '%') ESCAPE '\\' THEN 250
                           WHEN g.provider_name ILIKE input.like_query ESCAPE '\\' THEN 200
                           WHEN g.provider_name ILIKE CONCAT(input.like_query, '%') ESCAPE '\\' THEN 140
                           WHEN g.name ILIKE CONCAT('%', input.like_query, '%') ESCAPE '\\' THEN 90
                           WHEN g.provider_name ILIKE CONCAT('%', input.like_query, '%') ESCAPE '\\' THEN 70
                           ELSE 0
                       END AS lexical_boost,
                       CASE
                           WHEN input.has_query AND input.ts_query IS NOT NULL AND numnode(input.ts_query) > 0 THEN ts_rank_cd(
            """
            + GAME_SEARCH_DOCUMENT_SQL
            + """
                           , input.ts_query)
                           ELSE 0
                       END AS text_rank,
                       CASE
                           WHEN input.has_query THEN GREATEST(
                               similarity(g.name, input.raw_query),
                               similarity(g.provider_name, input.raw_query)
                           )
                           ELSE 0
                       END AS fuzzy_rank
                FROM games g
                CROSS JOIN input
                WHERE (
                    NOT input.has_query
                    OR (input.ts_query IS NOT NULL AND numnode(input.ts_query) > 0 AND
            """
            + GAME_SEARCH_DOCUMENT_SQL
            + """
                    @@ input.ts_query)
                    OR (input.has_query AND g.name % input.raw_query)
                    OR (input.has_query AND g.provider_name % input.raw_query)
                    OR (input.has_query AND g.name ILIKE CONCAT('%', input.like_query, '%') ESCAPE '\\')
                    OR (input.has_query AND g.provider_name ILIKE CONCAT('%', input.like_query, '%') ESCAPE '\\')
                )
                AND (
                    :platform IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM game_platform_tags platform_tag
                        WHERE platform_tag.game_id = g.id
                          AND platform_tag.normalized_value = LOWER(:platform)
                    )
                )
                AND (
                    :category IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM game_category_tags category_tag
                        WHERE category_tag.game_id = g.id
                          AND category_tag.normalized_value = LOWER(:category)
                    )
                )
                AND (
                    :publisher IS NULL
                    OR (
                        COALESCE(:publisherExact, false) = true
                        AND g.provider_name = :publisher
                    )
                    OR (
                        COALESCE(:publisherExact, false) = false
                        AND g.provider_name ILIKE CONCAT('%', :publisher, '%')
                    )
                )
                AND (
                    :catalogSource IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM game_catalog_source_tags source_tag
                        WHERE source_tag.game_id = g.id
                          AND source_tag.normalized_value = LOWER(:catalogSource)
                    )
                )
                AND (COALESCE(:hasDescription, false) = false OR NULLIF(BTRIM(g.description), '') IS NOT NULL)
                AND (COALESCE(:hasImage, false) = false OR NULLIF(BTRIM(g.image_url), '') IS NOT NULL)
            )
            SELECT g.*, s.* FROM ranked r
            JOIN games g ON g.id = r.id
            LEFT JOIN game_stats s ON g.id = s.game_id
            ORDER BY CASE WHEN :sort = 'NEWEST' THEN g.created END DESC NULLS LAST,
                     CASE WHEN :sort = 'RECENTLY_UPDATED' THEN g.last_modified END DESC NULLS LAST,
                     CASE WHEN :sort = 'POPULAR' THEN COALESCE(s.interactions, 0) END DESC NULLS LAST,
                     CASE WHEN :sort = 'WISHLIST' THEN COALESCE(s.wishlist_total, 0) END DESC NULLS LAST,
                     CASE WHEN :sort = 'ALPHABETICAL' THEN LOWER(g.name) END ASC NULLS LAST,
                     CASE WHEN :sort = 'RELEVANCE' THEN r.lexical_boost END DESC NULLS LAST,
                     CASE WHEN :sort = 'RELEVANCE' THEN r.text_rank END DESC NULLS LAST,
                     CASE WHEN :sort = 'RELEVANCE' THEN r.fuzzy_rank END DESC NULLS LAST,
                     CASE WHEN :sort = 'RELEVANCE' THEN COALESCE(s.interactions, 0) END DESC NULLS LAST,
                     g.name ASC
            LIMIT :limit
            """;

    @Query(value = """
                SELECT g.* FROM games g
                LEFT JOIN game_stats gs ON g.id = gs.game_id
                WHERE g.id IN :ids
            """, nativeQuery = true)
    List<GameData> findGamesByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT g.*, s.* FROM games g LEFT JOIN game_stats s ON g.id = s.game_id WHERE g.name IN :names", nativeQuery = true)
    List<GameData> findGamesByNames(@Param("names") List<String> names);

    //    @Query(value = "SELECT * FROM games WHERE game_id = :id LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameById(@Param("id") Long id);

    Optional<GameData> findByPublicId(String publicId);

    List<GameData> findByCatalogGroupKeyIn(Collection<String> catalogGroupKeys);

    List<GameData> findByCatalogGroupKeyInOrderByCatalogGroupKeyAscCreatedAscIdAsc(Collection<String> catalogGroupKeys);

    List<GameData> findByNormalizedTitleKeyIn(Collection<String> normalizedTitleKeys);

    List<GameData> findByNormalizedTitleKeyInOrderByNormalizedTitleKeyAscCreatedAscIdAsc(Collection<String> normalizedTitleKeys);

    List<GameData> findByIdIn(Collection<Long> ids);

    @Query("""
            select g.catalogGroupKey
            from GameData g
            where g.catalogGroupKey is not null
            group by g.catalogGroupKey
            having count(g.id) > 1
            order by min(g.created), min(g.id)
            """)
    List<String> findDuplicateCatalogGroupKeys();

    @Query("""
            select g.normalizedTitleKey
            from GameData g
            where g.normalizedTitleKey is not null and g.normalizedTitleKey <> ''
            group by g.normalizedTitleKey
            having count(g.id) > 1
            order by min(g.created), min(g.id)
            """)
    List<String> findDuplicateNormalizedTitleKeys();

    List<GameData> findAllByOrderByCreatedDesc(Pageable pageable);

    @Query(value = "SELECT id FROM games ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Long> findRandomGameId();

    //    @Query(value = "SELECT * FROM games WHERE name = :name AND game_id = :id LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByNameAndId(@Param("name") String name, @Param("id") Long id);

    @Query(value = "SELECT g.*, s.* FROM games g LEFT JOIN game_stats s ON g.id = s.game_id ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<GameData> findRandomGames(@Param("limit") Integer limit);

    @Query(value = """
            SELECT g.*, s.* FROM games g
            LEFT JOIN game_stats s ON g.id = s.game_id
            WHERE LOWER(g.name) LIKE CONCAT('%', LOWER(:name), '%')
            LIMIT :limit
            """, nativeQuery = true)
    List<GameData> findGamesByName(@Param("name") String name, @Param("limit") Integer limit);

    @Query(value = """
            SELECT g.*, gs.* FROM games g JOIN game_stats gs ON g.id = gs.game_id WHERE
            (:lastInteractions IS NULL OR :lastId IS NULL OR
            gs.interactions < :lastInteractions OR (gs.interactions = :lastInteractions AND gs.game_id < :lastId))
            ORDER BY gs.interactions DESC, gs.game_id DESC LIMIT :limit""", nativeQuery = true)
    List<GameData> findAllByOrderByInteractionsDesc(@Param("limit") Long limit,
                                                    @Param("lastId") Long lastId,
                                                    @Param("lastInteractions") Long lastInteractions);

    @Query(value = """
            SELECT g.*, gs.* FROM games g
            JOIN game_stats gs ON g.id = gs.game_id
            ORDER BY gs.wishlist_total DESC, gs.game_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<GameData> findAllByOrderByWishlistTotalDesc(@Param("limit") Integer limit);

    @Query(value = SEARCH_BY_QUERY_SQL, nativeQuery = true)
    List<GameData> searchByQuery(@Param("query") String query,
                                 @Param("likeQuery") String likeQuery,
                                 @Param("platform") String platform,
                                 @Param("category") String category,
                                 @Param("publisher") String publisher,
                                 @Param("publisherExact") Boolean publisherExact,
                                 @Param("catalogSource") String catalogSource,
                                 @Param("hasDescription") Boolean hasDescription,
                                 @Param("hasImage") Boolean hasImage,
                                 @Param("sort") String sort,
                                 @Param("limit") Integer limit);

    @Query(value = SEARCH_BY_QUERY_SQL + " OFFSET :offset", nativeQuery = true)
    List<GameData> searchByQuery(@Param("query") String query,
                                 @Param("likeQuery") String likeQuery,
                                 @Param("platform") String platform,
                                 @Param("category") String category,
                                 @Param("publisher") String publisher,
                                 @Param("publisherExact") Boolean publisherExact,
                                 @Param("catalogSource") String catalogSource,
                                 @Param("hasDescription") Boolean hasDescription,
                                 @Param("hasImage") Boolean hasImage,
                                 @Param("sort") String sort,
                                 @Param("limit") Integer limit,
                                 @Param("offset") Integer offset);

    @Query(value = """
            SELECT tag.value AS value, COUNT(*) AS count
            FROM game_publisher_tags tag
            WHERE (:prefix IS NULL OR :prefix = '' OR tag.normalized_value LIKE CONCAT(LOWER(:prefix), '%'))
            GROUP BY tag.value
            ORDER BY COUNT(*) DESC, tag.value ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SearchMetadataValueProjection> searchPublisherMetadata(@Param("prefix") String prefix,
                                                                @Param("limit") Integer limit);

    @Query(value = """
            SELECT tag.value AS value, COUNT(*) AS count
            FROM game_platform_tags tag
            WHERE (:prefix IS NULL OR :prefix = '' OR tag.normalized_value LIKE CONCAT(LOWER(:prefix), '%'))
            GROUP BY tag.value
            ORDER BY COUNT(*) DESC, tag.value ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SearchMetadataValueProjection> searchPlatformMetadata(@Param("prefix") String prefix,
                                                               @Param("limit") Integer limit);

    @Query(value = """
            SELECT tag.value AS value, COUNT(*) AS count
            FROM game_category_tags tag
            WHERE (:prefix IS NULL OR :prefix = '' OR tag.normalized_value LIKE CONCAT(LOWER(:prefix), '%'))
            GROUP BY tag.value
            ORDER BY COUNT(*) DESC, tag.value ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SearchMetadataValueProjection> searchCategoryMetadata(@Param("prefix") String prefix,
                                                               @Param("limit") Integer limit);

    @Query(value = """
            SELECT tag.value AS value, COUNT(*) AS count
            FROM game_catalog_source_tags tag
            WHERE (:prefix IS NULL OR :prefix = '' OR tag.normalized_value LIKE CONCAT(LOWER(:prefix), '%'))
            GROUP BY tag.value
            ORDER BY COUNT(*) DESC, tag.value ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SearchMetadataValueProjection> searchCatalogSourceMetadata(@Param("prefix") String prefix,
                                                                    @Param("limit") Integer limit);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE game_stats
            SET comments = (
                SELECT COUNT(*)
                FROM game_comments
                WHERE game_id = :gameId
                  AND deleted = false
            )
            WHERE game_id = :gameId
            """, nativeQuery = true)
    void refreshCommentCount(@Param("gameId") Long gameId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO game_stats (game_id, interactions, wishlist_total, comments, posts, average_rating)
            SELECT g.id, 0, 0, 0, 0, NULL
            FROM games g
            LEFT JOIN game_stats gs ON gs.game_id = g.id
            WHERE gs.game_id IS NULL
            """, nativeQuery = true)
    int insertMissingGameStatsRows();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE game_stats gs
            SET wishlist_total = aggregated.wishlist_total,
                average_rating = aggregated.average_rating
            FROM (
                SELECT g.id AS game_id,
                       COUNT(w.id) AS wishlist_total,
                       CAST(ROUND(CAST(AVG(w.rating) AS numeric), 1) AS double precision) AS average_rating
                FROM games g
                LEFT JOIN wishlist w ON w.gameinfo = g.id
                GROUP BY g.id
            ) aggregated
            WHERE gs.game_id = aggregated.game_id
            """, nativeQuery = true)
    int refreshAllWishlistStats();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE game_stats gs
            SET comments = aggregated.comment_total
            FROM (
                SELECT g.id AS game_id,
                       COUNT(gc.id) FILTER (WHERE gc.deleted = false) AS comment_total
                FROM games g
                LEFT JOIN game_comments gc ON gc.game_id = g.id
                GROUP BY g.id
            ) aggregated
            WHERE gs.game_id = aggregated.game_id
            """, nativeQuery = true)
    int refreshAllCommentCounts();
}
