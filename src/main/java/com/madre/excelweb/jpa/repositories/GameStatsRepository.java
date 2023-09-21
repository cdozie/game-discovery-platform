//package com.madre.excelweb.jpa.repositories;
//
//import com.madre.excelweb.jpa.entity.GameStats;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.Optional;
//
//
//public interface GameStatsRepository extends JpaRepository<GameStats, Long> {
//
//    //Create queries to order based off of analytics in GameStats
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY wishlists DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
//    List<GameStats> findAllByOrderByWishlistsDesc(@Param("limit") Long limit, @Param("offset") Integer offset);
//
//    @Query(value = "SELECT * FROM game_stats WHERE (:lastInteractions IS NULL OR :lastId IS NULL OR " +
//            "(interactions, game_id) < (:lastInteractions, :lastId)) ORDER BY interactions DESC , game_id DESC LIMIT :limit"
//            , nativeQuery = true)
//    List<GameStats> findAllByOrderByInteractionsDesc(@Param("limit") Long limit,
//                                                     @Param("lastId") Long lastId,
//                                                     @Param("lastInteractions") Long lastInteractions);
//
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY comments DESC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByCommentsDesc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY posts DESC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByPostsDesc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY average_rating DESC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByAverageRatingDesc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY interactions ASC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByInteractionsAsc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY wishlists ASC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByWishlistsAsc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY comments ASC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByCommentsAsc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY posts ASC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByPostsAsc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats ORDER BY average_rating ASC LIMIT :limit", nativeQuery = true)
//    List<GameStats> findAllByOrderByAverageRatingAsc(@Param("limit") Long limit);
//
//    @Query(value = "SELECT * FROM game_stats WHERE game_id = :gameId LIMIT 1", nativeQuery = true)
//    Optional<GameStats> findGameStatsByGameId(Long gameId);
//
//
//}
