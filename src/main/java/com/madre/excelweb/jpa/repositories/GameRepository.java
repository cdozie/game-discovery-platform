package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.GameData;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Table(name = "games")
public interface GameRepository extends JpaRepository<GameData, Long> {

    Double SIMILARITY_THRESHOLD = .3;

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

    //    @Query(value = "SELECT g.*, s.* FROM games g LEFT JOIN game_stats s ON g.game_id = s.game_id WHERE g.psn_id = :psnId LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByPsnId(@Param("psnId") String psnId);

    @Query(value = "SELECT id FROM games ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Long> findRandomGameId();

    //    @Query(value = "SELECT * FROM games WHERE name = :name AND game_id = :id LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByNameAndId(@Param("name") String name, @Param("id") Long id);

    //    @Query(value = "SELECT * FROM games WHERE name = :name AND psn_id = :psnId LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByNameAndPsnId(@Param("name") String name, @Param("psnId") String psnId);

    @Query(value = "SELECT g.*, s.* FROM games g LEFT JOIN game_stats s ON g.id = s.game_id ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<GameData> findRandomGames(@Param("limit") Integer limit);

    @Query(value = "SELECT g.*, s.* FROM games g LEFT JOIN game_stats s ON g.id = s.game_id WHERE LOWER(g.name) LIKE %:name% LIMIT :limit", nativeQuery = true)
    List<GameData> findGamesByName(@Param("name") String name, @Param("limit") Integer limit);

    //    @Query(value = """
//                SELECT g.*, s.*
//                FROM games g
//                LEFT JOIN game_stats s ON g.id = s.game_id
//                WHERE to_tsvector('english', setweight('A', g.name) || ' ' || setweight('B', g.provider_name)) @@ plainto_tsquery(:query)
//                LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);
//    @Query(value = """
//            SELECT g.*, s.*,
//                   ts_rank(to_tsvector('english', setweight('A', g.name) || ' ' || setweight('B', g.provider_name)), plainto_tsquery(:query)) AS rank
//            FROM games g
//            LEFT JOIN game_stats s ON g.id = s.game_id
//            WHERE to_tsvector('english', setweight('A', g.name) || ' ' || setweight('B', g.provider_name)) @@ plainto_tsquery(:query)
//            ORDER BY rank DESC
//            LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

//    @Query(value = """
//            SELECT g.*, s.*,
//                   ts_rank(setweight(to_tsvector('english', g.name), 'A') || setweight(to_tsvector('english', g.provider_name), 'B'), plainto_tsquery(:query)) AS rank
//            FROM games g
//            LEFT JOIN game_stats s ON g.id = s.game_id
//            WHERE setweight(to_tsvector('english', g.name), 'A') || setweight(to_tsvector('english', g.provider_name), 'B') @@ plainto_tsquery(:query)
//            ORDER BY rank DESC
//            LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

//    @Query(value = """
//            SELECT g.*, s.*,
//                   ts_rank(setweight(to_tsvector('english', g.name), 'A') || setweight(to_tsvector('english', g.provider_name), 'B'), plainto_tsquery(:query)) AS rank
//            FROM games g
//            LEFT JOIN game_stats s ON g.id = s.game_id
//            WHERE (setweight(to_tsvector('english', g.name), 'A') || setweight(to_tsvector('english', g.provider_name), 'B')) @@ plainto_tsquery(:query)
//            ORDER BY rank DESC
//            LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

//    @Query(value = """
//            SELECT g.*, s.*,
//                   ts_rank(to_tsvector('english', g.name || ' ' || g.provider_name), plainto_tsquery(:query)) AS rank
//            FROM games g
//            LEFT JOIN game_stats s ON g.id = s.game_id
//            WHERE to_tsvector('english', g.name || ' ' || g.provider_name) @@ plainto_tsquery(:query)
//            ORDER BY rank DESC
//            LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

//    @Transactional
//    @Query(value = """
//                SELECT g.*, s.* FROM games g
//                LEFT JOIN game_stats s ON g.id = s.game_id
//                WHERE g.name % :query OR g.provider_name % :query
//                ORDER BY CASE
//                    WHEN g.name % :query THEN similarity(g.name, :query)
//                    WHEN g.provider_name % :query THEN similarity(g.provider_name, :query)
//                END DESC
//                LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

    @Query(value = """
            SELECT g.*, gs.* FROM games g JOIN game_stats gs ON g.id = gs.game_id WHERE
            (:lastInteractions IS NULL OR :lastId IS NULL OR
            (gs.interactions, gs.game_id) < (:lastInteractions, :lastId))
            ORDER BY gs.interactions DESC, gs.game_id DESC LIMIT :limit""", nativeQuery = true)
    List<GameData> findAllByOrderByInteractionsDesc(@Param("limit") Long limit,
                                                    @Param("lastId") Long lastId,
                                                    @Param("lastInteractions") Long lastInteractions);

//    @Query(value = """
//                SELECT g.*, s.* FROM games g
//                LEFT JOIN game_stats s ON g.id = s.game_id
//                WHERE (g.name % :query OR g.provider_name % :query)
//                AND (similarity(g.name, :query) > 0.3 OR similarity(g.provider_name, :query) > 0.3)
//                ORDER BY CASE
//                    WHEN g.name % :query THEN similarity(g.name, :query)
//                    WHEN g.provider_name % :query THEN similarity(g.provider_name, :query)
//                END DESC
//                LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

    @Query(value = """
                SELECT g.*, s.* FROM games g
                LEFT JOIN game_stats s ON g.id = s.game_id
                WHERE similarity(g.name, :query) > 0.1
                ORDER BY similarity(g.name, :query) DESC
                LIMIT :limit
            """, nativeQuery = true)
    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);

//    @Query(value = """
//                SELECT g.*, s.* FROM games g
//                LEFT JOIN game_stats s ON g.id = s.game_id
//                WHERE similarity(g.name, :query) > 0.2 OR similarity(g.provider_name, :query) > 0.2
//                ORDER BY CASE
//                    WHEN g.name % :query THEN similarity(g.name, :query)
//                    WHEN g.provider_name % :query THEN similarity(g.provider_name, :query)
//                END DESC
//                LIMIT :limit
//            """, nativeQuery = true)
//    List<GameData> searchByQuery(@Param("query") String query, @Param("limit") Integer limit);
}


