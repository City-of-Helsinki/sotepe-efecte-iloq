package com.devikone.service;

import com.devikone.transports.Redis;

import java.util.Map;

import jakarta.inject.Singleton;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.enterprise.context.ApplicationScoped;

@Singleton
public class IBRedisProducer {

    private String redisHost = "localhost";
    private String redisPassword = "";
    private Redis redis;

    @Produces
    @ApplicationScoped
    @Named("redis")
    public Redis createRedis() {
        if (redis == null) {
            redis = new Redis();
            Map<String, String> env = System.getenv();
            if (env.get("REDIS_HOST") != null) {
                redisHost = env.get("REDIS_HOST");
            }
            if (env.get("REDIS_PASSWORD") != null) {
                redisPassword = env.get("REDIS_PASSWORD");
            }

            redis.setRedisHost(redisHost);
            redis.setRedisPassword(redisPassword);
        }

        return redis;
    }
}