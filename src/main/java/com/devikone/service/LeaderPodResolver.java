package com.devikone.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.devikone.transports.Redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("leaderPodResolver")
public class LeaderPodResolver {

    @Inject
    Redis redis;
    @ConfigProperty(name = "QUARKUS_PROFILE")
    String environment;
    @ConfigProperty(name = "LEADER_POD_KEY_EXPIRATION_SECONDS")
    long keyExpirationSeconds;

    public boolean isLeaderPod() throws Exception {
        String result = redis.setIfNotExists(
                "efecte-iloq-synchronization-integration:leader-pod-key:" + environment, "leader",
                keyExpirationSeconds);

        if ("OK".equals(result)) {
            return true;
        }

        return false;
    }
}
