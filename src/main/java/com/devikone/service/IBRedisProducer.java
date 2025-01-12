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
    private int redisPort = 6379;
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
            if (env.get("REDIS_PORT") != null) {
                redisPort = Integer.valueOf(env.get("REDIS_PORT"));
            }

            redis.setRedisHost(redisHost);
            redis.setRedisPassword(redisPassword);
            redis.setRedisPort(redisPort);

            // Enable Sentinel if REDIS_USE_SENTINEL is true
            if (Boolean.parseBoolean(env.get("REDIS_USE_SENTINEL"))) {
                redis.setUseSentinel(true);
                redis.setSentinelMaster(env.get("REDIS_SENTINEL_MASTER"));
                String sentinelHost = env.get("REDIS_SENTINEL_HOST");
                String sentinelPort = env.get("REDIS_SENTINEL_PORT");
                redis.addSentinelNode(sentinelHost + ":" + sentinelPort);
            }
        }

        return redis;
    }
}