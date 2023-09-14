package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.Posts;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Table(name = "posts")
public interface PostsRepository extends JpaRepository<Posts, Long> {
}
