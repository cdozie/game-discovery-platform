package com.madre.excelweb.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.rest.ControllerUtils;
import com.madre.excelweb.security.jwt.AuthEntryPointJwt;
import com.madre.excelweb.security.jwt.JwtUtils;
import com.madre.excelweb.security.service.UserDetailsServiceImpl;
import com.madre.excelweb.service.IdGenerationService;
import com.madre.excelweb.service.LoginService;
import com.madre.excelweb.service.RedissonLockClient;
import com.madre.excelweb.service.api.PsnApiIterator;
import com.madre.excelweb.service.api.PsnApiReader;
import com.madre.excelweb.service.api.PsnGameSyncService;
import com.madre.excelweb.service.redis.GameStatsService;
import com.madre.excelweb.service.redis.PendingUserService;
import com.madre.excelweb.service.redis.VerificationTokenService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WorkerConfiguration {

    public static final String PSN_BASE_URL = "https://store.playstation.com/store/api/chihiro/00_09_000/container/US/en/999/";

    private static final int DEFAULT_ITERATOR_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_ITERATOR_RETRY_BACKOFF = 1000;

    private static final String ITERATOR_RETRY_ATTEMPTS = "web.app.retry.iterator-attempts";
    private static final String ITERATOR_RETRY_BACKOFF = "web.app.retry.iterator-backoff";

    @Bean
    public LoginService loginService(final UserRepository userRepository) {
        return new LoginService(userRepository);
    }

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils();
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService(UserRepository userRepository) {
        return new UserDetailsServiceImpl(userRepository);
    }

    @Bean
    public AuthEntryPointJwt authEntryPointJwt() {
        return new AuthEntryPointJwt();
    }


    @Bean
    public RedisTemplate<String, AppUser> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, AppUser> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<AppUser> serializer = new Jackson2JsonRedisSerializer<>(AppUser.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        return template;

    }

    @Bean
    public RedisTemplate<String, EmailVerificationToken> emailVerificationTokenRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, EmailVerificationToken> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<EmailVerificationToken> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, EmailVerificationToken.class);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        return template;

    }

    @Bean
    public PendingUserService pendingUserService(final RedisTemplate<String, AppUser> redisTemplate) {
        return new PendingUserService(redisTemplate);
    }

    @Bean
    public VerificationTokenService verificationTokenService(final RedisTemplate<String, EmailVerificationToken> redisTemplate) {
        return new VerificationTokenService(redisTemplate);
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

    @Bean
    public PsnApiReader apiReader(final WebClient psnWebClient) {
        return new PsnApiReader(psnWebClient);
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

    @Bean
    public PsnGameSyncService psnGameSyncService(final PsnApiIterator psnApiIterator,
                                                 final GameRepository gameRepository,
                                                 final RedissonLockClient redissonLockClient,
                                                 final Environment env,
                                                 final IdGenerationService idGenerationService) {
        return new PsnGameSyncService(psnApiIterator, gameRepository, redissonLockClient, env, idGenerationService);
    }

    @Bean
    public IdGenerationService idGenerationService() {
        return new IdGenerationService();
    }

    @Bean
    public GameStatsService gameStatsService(final RedissonClient redissonClient,
                                             final GameRepository gameRepository) {
        return new GameStatsService(redissonClient, gameRepository);
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
