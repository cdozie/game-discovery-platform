package com.madre.excelweb.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.madre.excelweb.jpa.entity.CatalogSyncRun;
import com.madre.excelweb.jpa.repositories.CatalogSyncRunRepository;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.PendingUserRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.jpa.repositories.VerificationTokenRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.rest.ControllerUtils;
import com.madre.excelweb.security.jwt.AuthEntryPointJwt;
import com.madre.excelweb.security.jwt.JwtUtils;
import com.madre.excelweb.security.service.UserDetailsServiceImpl;
import com.madre.excelweb.service.CatalogUpsertService;
import com.madre.excelweb.service.IdGenerationService;
import com.madre.excelweb.service.LoginService;
import com.madre.excelweb.service.RedissonLockClient;
import com.madre.excelweb.service.api.IgdbApiReader;
import com.madre.excelweb.service.api.IgdbGameSyncService;
import com.madre.excelweb.service.api.PsnApiIterator;
import com.madre.excelweb.service.api.PsnApiReader;
import com.madre.excelweb.service.api.PsnGameSyncService;
import com.madre.excelweb.service.redis.GameStatsService;
import com.madre.excelweb.service.redis.PendingUserService;
import com.madre.excelweb.service.redis.VerificationTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;

@Configuration
public class WorkerConfiguration {

    public static final String PSN_BASE_URL = "https://store.playstation.com/store/api/chihiro/00_09_000/container/US/en/999/";
    public static final String IGDB_BASE_URL = "https://api.igdb.com/v4";
    public static final String TWITCH_AUTH_BASE_URL = "https://id.twitch.tv";

    private static final int DEFAULT_ITERATOR_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_ITERATOR_RETRY_BACKOFF = 1000;

    private static final String ITERATOR_RETRY_ATTEMPTS = "web.app.retry.iterator-attempts";
    private static final String ITERATOR_RETRY_BACKOFF = "web.app.retry.iterator-backoff";

    @Bean
    public LoginService loginService(final UserRepository userRepository) {
        return new LoginService(userRepository);
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService(UserRepository userRepository) {
        return new UserDetailsServiceImpl(userRepository);
    }

    @Bean
    public AuthEntryPointJwt authEntryPointJwt(final ObjectMapper objectMapper) {
        return new AuthEntryPointJwt(objectMapper);
    }

    @Bean
    public PendingUserService pendingUserService(final PendingUserRepository pendingUserRepository) {
        return new PendingUserService(pendingUserRepository);
    }

    @Bean
    public VerificationTokenService verificationTokenService(final VerificationTokenRepository verificationTokenRepository) {
        return new VerificationTokenService(verificationTokenRepository);
    }

    @Bean(name = "psnWebClient")
    public WebClient psnApiWebClient() {
        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        return WebClient.builder()
                .baseUrl(PSN_BASE_URL)
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean(name = "igdbApiWebClient")
    public WebClient igdbApiWebClient() {
        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        return WebClient.builder()
                .baseUrl(IGDB_BASE_URL)
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean(name = "igdbAuthWebClient")
    public WebClient igdbAuthWebClient() {
        return WebClient.builder()
                .baseUrl(TWITCH_AUTH_BASE_URL)
                .build();
    }

    @Bean
    public PsnApiReader apiReader(final WebClient psnWebClient) {
        return new PsnApiReader(psnWebClient);
    }

    @Bean
    public IgdbApiReader igdbApiReader(@Qualifier("igdbApiWebClient") final WebClient igdbApiWebClient,
                                       final com.madre.excelweb.service.api.IgdbAccessTokenService igdbAccessTokenService,
                                       final Environment env) {
        return new IgdbApiReader(igdbApiWebClient, igdbAccessTokenService, env);
    }

    @Bean(name = "iteratorRetryTemplate")
    public RetryTemplate retryTemplate(final Environment env) {
        return RetryTemplate.builder()
                .fixedBackoff(env.getProperty(ITERATOR_RETRY_BACKOFF, Integer.class, DEFAULT_ITERATOR_RETRY_BACKOFF))
                .maxAttempts(env.getProperty(ITERATOR_RETRY_ATTEMPTS, Integer.class, DEFAULT_ITERATOR_RETRY_ATTEMPTS))
                .build();
    }

    @Bean
    public PsnApiIterator psnApiIterator(final PsnApiReader psnApiReader,
                                         @Qualifier("iteratorRetryTemplate") final RetryTemplate iteratorRetryTemplate) {
        return new PsnApiIterator(psnApiReader, iteratorRetryTemplate);
    }

    @Bean(name = "catalogSyncExecutor")
    public Executor catalogSyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("catalog-sync-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.initialize();
        return executor;
    }

    @Bean(name = "systemJobExecutor")
    public Executor systemJobExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("system-job-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(16);
        executor.initialize();
        return executor;
    }

    @Bean
    public PsnGameSyncService psnGameSyncService(final PsnApiReader psnApiReader,
                                                 @Qualifier("iteratorRetryTemplate") final RetryTemplate iteratorRetryTemplate,
                                                 final CatalogUpsertService catalogUpsertService,
                                                 final CatalogSyncRunRepository catalogSyncRunRepository,
                                                 final RedissonLockClient redissonLockClient,
                                                 final Environment env,
                                                 @Qualifier("catalogSyncExecutor") final Executor catalogSyncExecutor) {
        return new PsnGameSyncService(psnApiReader, iteratorRetryTemplate, catalogUpsertService, catalogSyncRunRepository,
                redissonLockClient, env, catalogSyncExecutor);
    }

    @Bean
    public IgdbGameSyncService igdbGameSyncService(final IgdbApiReader igdbApiReader,
                                                   final com.madre.excelweb.service.api.IgdbAccessTokenService igdbAccessTokenService,
                                                   @Qualifier("iteratorRetryTemplate") final RetryTemplate iteratorRetryTemplate,
                                                   final CatalogUpsertService catalogUpsertService,
                                                   final CatalogSyncRunRepository catalogSyncRunRepository,
                                                   final RedissonLockClient redissonLockClient,
                                                   final Environment env,
                                                   @Qualifier("catalogSyncExecutor") final Executor catalogSyncExecutor) {
        return new IgdbGameSyncService(igdbApiReader, igdbAccessTokenService, iteratorRetryTemplate, catalogUpsertService,
                catalogSyncRunRepository, redissonLockClient, env, catalogSyncExecutor);
    }

    @Bean
    public IdGenerationService idGenerationService() {
        return new IdGenerationService();
    }

    @Bean
    public GameStatsService gameStatsService(final StringRedisTemplate stringRedisTemplate,
                                             final GameRepository gameRepository) {
        return new GameStatsService(stringRedisTemplate, gameRepository);
    }

    @Bean
    public ControllerUtils controllerUtils(final JwtUtils jwtUtils,
                                           final WishlistRepository wishlistRepository,
                                           final UserRepository userRepository) {
        return new ControllerUtils(jwtUtils, wishlistRepository, userRepository);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizeEnum() {
        return builder -> builder.featuresToEnable(DeserializationFeature.READ_ENUMS_USING_TO_STRING,
                SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    }
}
