package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.Wishlist;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Table(name = "wishlist")
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserinfoIdAndGameinfoId(Long userId, Long gameId);

    List<Wishlist> findByUserinfoId(Long userId);


}
