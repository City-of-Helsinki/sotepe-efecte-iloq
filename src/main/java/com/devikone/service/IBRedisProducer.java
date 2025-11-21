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

            // Enable Sentinel if REDIS_USE_SENTINEL is true
            if (Boolean.parseBoolean(env.get("REDIS_USE_SENTINEL"))) {
                // SENTINEL MODE - only configure sentinel parameters
                redis.setUseSentinel(true);
                redis.setSentinelPassword(env.get("REDIS_PASSWORD"));
                redis.setSentinelMaster(env.get("REDIS_SENTINEL_MASTER"));
                String sentinelHost = env.get("REDIS_SENTINEL_HOST");
                String sentinelPort = env.get("REDIS_SENTINEL_PORT");
                redis.addSentinelNode(sentinelHost + ":" + sentinelPort);
            } else {
                // DIRECT CONNECTION MODE - configure direct redis connection
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
            }
        }
        return redis;
    }

    private JedisPoolConfig createJedisPoolConfig() {
        return Redis.buildPoolConfig(
                8, // maxTotal - 8 connections is plenty for non-master pod
                4, // maxIdle - Keep 4 idle connections max
                1, // minIdle - Keep 1 idle connections min
                false, // testOnBorrow - DISABLE validation
                false, // testOnReturn - DISABLE validation
                false, // testWhileIdle - DISABLE validation
                Duration.ofSeconds(60), // minEvictableIdleTime - Evict after 60 seconds idle (before Redis 90s timeout)
                Duration.ofMinutes(5), // timeBetweenEvictionRuns - Don't run frequently since testWhileIdle is false
                3, // numTestsPerEvictionRun
                true // blockWhenExhausted
        );
    }
}

/*

Redis configurations:

timeout 90 - Redis server closes idle connections after 90 seconds
tcp-keepalive 30 - Redis sends TCP keepalive probes every 30 seconds

JedisPool configurations:

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
Revised recommendation: 60 seconds
Why: Your Redis server is configured to close idle connections after 90 seconds (timeout 90 in your redis.conf). If we set the eviction time to 5 minutes (300 seconds), idle connections would remain in the pool after Redis has already closed them, which would cause connection errors.
By keeping the eviction time at 60 seconds, we ensure connections are removed from the pool before the Redis server closes them (60s < 90s). Combined with minIdle: 1, you'll maintain one fresh connection while avoiding errors caused by stale connections that exceed Redis's timeout.
This setting works together with testWhileIdle: false to prevent the validation errors while maintaining good performance.

5. poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofMinutes(2).toMillis())
What it does: Sets how frequently the evictor thread runs to check for and remove stale connections
Previous setting: 30 seconds
Revised recommendation: 30 seconds (not 2 minutes)
Why: With testWhileIdle: false, we're no longer encountering validation errors during eviction runs. However, we still need the eviction thread to run regularly to remove idle connections before the Redis server closes them at 90 seconds.
Here's the new reasoning:
Redis closes connections after 90 seconds of inactivity
We've set minEvictableIdleDuration to 60 seconds
If a connection becomes idle just after an eviction run completes, we need another run to occur before that connection reaches 90 seconds of idleness
By keeping the eviction interval at 30 seconds, we ensure that idle connections are removed from the pool before Redis has a chance to close them, avoiding unexpected connection failures.
This approach prioritizes reliable connections over reduced eviction frequency. Since we've disabled connection testing during eviction (testWhileIdle: false), the frequency of eviction runs no longer impacts error logging.

 */