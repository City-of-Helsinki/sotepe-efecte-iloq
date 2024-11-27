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
    @ConfigProperty(name = "LEADER_POD_KEY_EXPIRATION_SECONDS")
    long keyExpirationSeconds;

    public boolean isLeaderPod() throws Exception {
        String result = redis.setIfNotExists("efecte-webhook-controller-leader-pod-key", "leader",
                keyExpirationSeconds);

        if ("OK".equals(result)) {
            return true;
        }

        return false;
    }
}
