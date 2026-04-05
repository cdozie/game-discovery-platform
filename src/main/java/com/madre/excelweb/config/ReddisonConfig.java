package com.madre.excelweb.config;

import com.madre.excelweb.service.RedissonLockClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ReddisonConfig {

    @Bean
    public RedissonLockClient redissonLockClient(final StringRedisTemplate stringRedisTemplate,
                                                 final Environment env) {
        return new RedissonLockClient(stringRedisTemplate, env);
    }
}
