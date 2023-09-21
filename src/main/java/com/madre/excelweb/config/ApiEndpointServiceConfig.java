package com.madre.excelweb.config;

import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.service.WishlistModificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiEndpointServiceConfig {

    @Bean
    public WishlistModificationService wishlistModificationService(final WishlistRepository wishlistRepository,
                                                                   final GameRepository gameRepository
    ) {
        return new WishlistModificationService(wishlistRepository, gameRepository);
    }
}
