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
    @Query(value = "SELECT * FROM games WHERE id IN :ids", nativeQuery = true)
    List<GameData> findGamesByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM games WHERE name IN :names", nativeQuery = true)
    List<GameData> findGamesByNames(@Param("names") List<String> names);

    @Query(value = "SELECT * FROM games WHERE id = :id LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameById(@Param("id") Long id);

    @Query(value = "SELECT * FROM games WHERE psn_id = :psnId LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByPsnId(@Param("psnId") String psnId);

    @Query(value = "SELECT id from games order by random() LIMIT 1", nativeQuery = true)
    Optional<Long> findRandomGameId();

    @Query(value = "SELECT * FROM games WHERE name = :name AND id = :id LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByNameAndId(@Param("name") String name, @Param("id") Long id);

    @Query(value = "SELECT * FROM games WHERE name = :name AND psn_id = :psnId LIMIT 1", nativeQuery = true)
    Optional<GameData> findGameByNameAndPsnId(@Param("name") String name, @Param("psnId") String psnId);

    @Query(value = "SELECT * FROM games order by random() LIMIT :limit", nativeQuery = true)
    List<GameData> findRandomGames(@Param("limit") Integer limit);


}
