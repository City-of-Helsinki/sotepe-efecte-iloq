package com.devikone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.service.LeaderResolver;
import com.devikone.transports.Redis;

import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class LeaderResolverTest {

    @Inject
    ResourceInjector ri;
    @Inject
    LeaderResolver leaderResolver;
    @InjectMock
    Redis redis;
    @ConfigProperty(name = "QUARKUS_PROFILE")
    String environment;
    @ConfigProperty(name = "LEADER_POD_KEY_EXPIRATION_MILLISECONDS")
    long podKeyExpirationMilliseconds;
    @ConfigProperty(name = "LEADER_ROUTE_KEY_EXPIRATION_MILLISECONDS")
    long routeKeyExpirationMilliseconds;

    @Test
    @DisplayName("isLeaderPod")
    void testShouldReturnTrueForTheLeaderPod() throws Exception {
        when(redis.setIfNotExists(ri.getLeaderPodKeyPrefix() + environment, "leader-pod",
                podKeyExpirationMilliseconds)).thenReturn("OK");

        verifyNoInteractions(redis);

        boolean result = leaderResolver.isLeaderPod();

        verify(redis).setIfNotExists(ri.getLeaderPodKeyPrefix() + environment, "leader-pod",
                podKeyExpirationMilliseconds);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isLeaderPod")
    void testShouldReturnFalseForNonLeaderPod() throws Exception {
        when(redis.setIfNotExists(ri.getLeaderPodKeyPrefix() + environment, "leader-pod",
                podKeyExpirationMilliseconds)).thenReturn(null);

        verifyNoInteractions(redis);

        boolean result = leaderResolver.isLeaderPod();

        verify(redis).setIfNotExists(ri.getLeaderPodKeyPrefix() + environment, "leader-pod",
                podKeyExpirationMilliseconds);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isLeaderRoute")
    void testShouldReturnTrueForTheLeaderRoute() throws Exception {
        when(redis.setIfNotExists(ri.getLeaderRouteKeyPrefix() + environment, "leader-route",
                routeKeyExpirationMilliseconds)).thenReturn("OK");

        verifyNoInteractions(redis);

        boolean result = leaderResolver.isLeaderRoute();

        verify(redis).setIfNotExists(ri.getLeaderRouteKeyPrefix() + environment, "leader-route",
                routeKeyExpirationMilliseconds);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isLeaderRoute")
    void testShouldReturnFalseForNonLeaderRoute() throws Exception {
        when(redis.setIfNotExists(ri.getLeaderRouteKeyPrefix() + environment, "leader-route",
                routeKeyExpirationMilliseconds)).thenReturn(null);

        verifyNoInteractions(redis);

        boolean result = leaderResolver.isLeaderRoute();

        verify(redis).setIfNotExists(ri.getLeaderRouteKeyPrefix() + environment, "leader-route",
                routeKeyExpirationMilliseconds);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("releaseLeaderRoute")
    void testShouldReleaseTheLeaderRouteKey() throws Exception {
        verifyNoInteractions(redis);

        leaderResolver.releaseLeaderRoute();

        verify(redis).del(ri.getLeaderRouteKeyPrefix() + environment);
    }
}
