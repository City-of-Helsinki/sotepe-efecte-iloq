package com.devikone.service;

import com.devikone.transports.Redis;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Singleton;
import redis.clients.jedis.JedisPoolConfig;
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
            redis = new Redis(createJedisPoolConfig());
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
                redis.setSentinelPassword(env.get("REDIS_PASSWORD"));
                redis.setSentinelMaster(env.get("REDIS_SENTINEL_MASTER"));
                String sentinelHost = env.get("REDIS_SENTINEL_HOST");
                String sentinelPort = env.get("REDIS_SENTINEL_PORT");
                redis.addSentinelNode(sentinelHost + ":" + sentinelPort);
            }
        }

        return redis;
    }

    private JedisPoolConfig createJedisPoolConfig() {
        return Redis.buildPoolConfig(
                8, // maxTotal - 8 max connections
                4, // maxIdle - 4 max idle connections
                1, // minIdle - 1 min idle connection
                true, // testOnBorrow - test when borrowing
                true, // testOnReturn - test when returning
                true, // testWhileIdle - test idle connections
                Duration.ofMinutes(5), // minEvictableIdleTime - 5 minutes
                Duration.ofMinutes(2), // timeBetweenEvictionRuns - 2 minutes
                3, // numTestsPerEvictionRun - test 3 per run
                true // blockWhenExhausted - block when exhausted
        );
    }
}

/*

1. poolConfig.setMaxTotal(8)

What it does: Sets the maximum total number of Redis connections the pool can contain (both active and idle)
Previous setting: 128 connections
Recommended: 8 connections
Why: Your non-master pod only makes infrequent Redis calls (hourly jobs and ~10 webhook calls). Even with concurrent requests, 8 simultaneous connections should be more than enough. 128 is excessive and wastes resources.

2. poolConfig.setMaxIdle(4)

What it does: Sets the maximum number of "idle" connections that can be kept in the pool
Previous setting: 128 idle connections
Recommended: 4 idle connections
Why: When Redis operations complete, connections return to the "idle" state. Your Previous setting keeps up to 128 idle connections in memory. Since your non-master pod's Redis usage is infrequent, 4 idle connections should be plenty to handle your hourly schedule and webhook calls.

3. poolConfig.setMinIdle(1)

What it does: Sets the minimum number of idle connections to maintain in the pool at all times
Previous setting: 16 idle connections
Recommended: 1 idle connection
Why: Your code is currently maintaining at least 16 idle connections even when there's no activity. This is wasteful for a non-master pod. Keeping just 1 idle connection ensures quick response when needed while minimizing resources.

4. poolConfig.setMinEvictableIdleTimeMillis(Duration.ofMinutes(5).toMillis())

What it does: Sets how long an idle connection can remain unused before it becomes eligible for eviction (removal)
Previous setting: 60 seconds
Recommended: 5 minutes (300 seconds)
Why: With your current 60-second setting, connections are evicted quickly, but new ones are continuously created due to your high MinIdle (16). By extending this to 5 minutes and lowering MinIdle to 1, you'll have fewer connection creation/destruction cycles between your hourly jobs.

5. poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofMinutes(2).toMillis())

What it does: Sets how frequently the evictor thread runs to check for and remove stale connections
Previous setting: 30 seconds
Recommended: 2 minutes (120 seconds)
Why: This is the primary setting causing your log spam. Every 30 seconds, the pool checks all idle connections with a Redis PING. By reducing this to every 2 minutes, you'll have 75% fewer validation attempts and thus 75% fewer error logs.

 */