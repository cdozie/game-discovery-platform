package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.Wishlist;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Table(name = "wishlist")
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

//    @Query(value = "SELECT * FROM wishlist WHERE  = :userid AND game_id = :gameid LIMIT 1", nativeQuery = true)
//    Optional<Wishlist> findByUserIdAndGameId(@Param("userid") Long userid, @Param("gameid") Long gameid);

    Optional<Wishlist> findByUserinfoIdAndGameinfoId(Long userId, Long gameId);

    List<Wishlist> findByUserinfoId(Long userId);


}
