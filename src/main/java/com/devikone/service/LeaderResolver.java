package com.devikone.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.devikone.transports.Redis;

import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("leaderResolver")
public class LeaderResolver {

    @Inject
    Redis redis;
    @Inject
    ResourceInjector ri;
    @ConfigProperty(name = "QUARKUS_PROFILE")
    String environment;
    @ConfigProperty(name = "LEADER_POD_KEY_EXPIRATION_MILLISECONDS")
    long podKeyExpirationMilliseconds;
    @ConfigProperty(name = "LEADER_ROUTE_KEY_EXPIRATION_MILLISECONDS")
    long routeKeyExpirationMilliseconds;

    public boolean isLeaderPod() throws Exception {
        String result = redis.setIfNotExists(
                ri.getLeaderPodKeyPrefix() + environment, "leader-pod",
                podKeyExpirationMilliseconds);

        if ("OK".equals(result)) {
            return true;
        }

        return false;
    }

    public boolean isLeaderRoute() throws Exception {
        String result = redis.setIfNotExists(
                ri.getLeaderRouteKeyPrefix() + environment, "leader-route",
                routeKeyExpirationMilliseconds);

        if ("OK".equals(result)) {
            return true;
        }

        return false;
    }

    public void releaseLeaderRoute() throws Exception {
        redis.del(ri.getLeaderRouteKeyPrefix() + environment);
    }
}
