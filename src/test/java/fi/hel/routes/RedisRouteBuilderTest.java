package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;

import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.processors.Helper;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(RedisRouteBuilderTest.class)
@QuarkusTest
public class RedisRouteBuilderTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @InjectMock
    Helper helper;

    @ConfigProperty(name = "EFECTE_INITIAL_MAX_UPDATED")
    String initialMaxUpdated;
    @ConfigProperty(name = "DELETED_KEY_EXPIRATION_TIME")
    Long deletedKeyExpirationTime;

    private String getMaxUpdatedEndpoint = "direct:getMaxUpdated";
    private String createNewMaxUpdatedEndpoint = "direct:createNewMaxUpdated";
    private String setMaxUpdatedEndpoint = "direct:setMaxUpdated";
    private String getILoqCredentialsEndpoint = "direct:getILoqCredentials";
    private String saveILoqBaseUrlToRedisEndpoint = "direct:saveILoqBaseUrlToRedis";
    private String saveILoqSessionIdToRedisEndpoint = "direct:saveILoqSessionIdToRedis";
    private String removeCurrentILoqSessionRelatedKeysEndpoint = "direct:removeCurrentILoqSessionRelatedKeys";
    private String saveMappedKeysEndpoint = "direct:saveMappedKeys";
    private String savePreviousKeyInfosEndpoint = "direct:savePreviousKeyInfos";
    private String deleteKeyEndpoint = "direct:deleteKey";
    private String removeTempKeysEndpoint = "direct:removeTempKeys";

    private String mockEndpoint = "mock:mockEndpoint";
    private MockEndpoint mock;

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    protected void doAfterConstruct() throws Exception {
        testUtils.addMockEndpointsTo(mockEndpoint,
                getMaxUpdatedEndpoint,
                setMaxUpdatedEndpoint,
                createNewMaxUpdatedEndpoint,
                getILoqCredentialsEndpoint,
                removeCurrentILoqSessionRelatedKeysEndpoint);
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();
        mock = getMockEndpoint(mockEndpoint);
    }

    @Test
    @DisplayName("direct:getMaxUpdated")
    void testShouldSetTheMaxUpdatedPropertyValue() throws Exception {
        String expectedMaxUpdated = "irrelevant timestamp";
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getMaxUpdatedPrefix())).thenReturn(expectedMaxUpdated);

        mock.expectedMessageCount(1);
        mock.expectedPropertyReceived("maxUpdated", expectedMaxUpdated);

        template.send(getMaxUpdatedEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getMaxUpdated")
    void testShouldInitializeTheMaxUpdatedPropertyValueWhenItIsNotFoundAtRedis() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getMaxUpdatedPrefix())).thenReturn(null);

        mock.expectedMessageCount(1);
        mock.expectedPropertyReceived("maxUpdated", initialMaxUpdated);

        template.send(getMaxUpdatedEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:createNewMaxModified")
    void testShouldSetANewMaxModifiedTimestampAsAPropertyValue() throws Exception {
        String expectedMaxUpdated = testUtils.createDatetimeNow("dd.MM.yyyy HH:mm");
        Exchange ex = testUtils.createExchange(null);

        mock.expectedMessageCount(1);
        mock.expectedPropertyReceived("newMaxUpdated", expectedMaxUpdated);

        template.send(createNewMaxUpdatedEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:setMaxUpdated")
    void testShouldSetMaxUpdated() throws Exception {
        String expectedNewMaxUpdated = "07.05.2024 16:40";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("newMaxUpdated", expectedNewMaxUpdated);

        verifyNoInteractions(redis);

        template.send(setMaxUpdatedEndpoint, ex);

        verify(redis).set(ri.getMaxUpdatedPrefix(), expectedNewMaxUpdated);
    }

    @Test
    @DisplayName("direct:getILoqCredentials")
    void testShouldSetTheCustomerCodeRelatedPropertyValues() throws Exception {
        String expectedCustomerCode = "FOO123";
        String expectedCustomerCodePassword = "super_secret";
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn(expectedCustomerCode);
        when(redis.get(ri.getILoqCurrentCustomerCodePasswordPrefix())).thenReturn(expectedCustomerCodePassword);

        verifyNoInteractions(redis);
        mock.expectedMessageCount(1);
        mock.expectedPropertyReceived("customerCode", expectedCustomerCode);
        mock.expectedPropertyReceived("customerCodePassword", expectedCustomerCodePassword);

        template.send(getILoqCredentialsEndpoint, ex);

        verify(redis).get(ri.getILoqCurrentCustomerCodePrefix());
        verify(redis).get(ri.getILoqCurrentCustomerCodePasswordPrefix());
        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:saveILoqBaseUrlToRedis")
    void testShouldSaveTheILoqBaseUrlToRedis() throws Exception {
        String expectedILoqBaseUrl = "www.foobar.com";
        Exchange ex = testUtils.createExchange(expectedILoqBaseUrl);

        verifyNoInteractions(redis);

        template.send(saveILoqBaseUrlToRedisEndpoint, ex);

        verify(redis).set(ri.getILoqBaseUrlPrefix(), expectedILoqBaseUrl);
    }

    @Test
    @DisplayName("direct:saveILoqSessionIdToRedis")
    void testShouldSaveTheILoqSessionIdToRedis() throws Exception {
        String expectedSessionId = "a5f0f07b-f5db-4663-960d-0547319b8322";
        Exchange ex = testUtils.createExchange(expectedSessionId);

        verifyNoInteractions(redis);

        template.send(saveILoqSessionIdToRedisEndpoint, ex);

        verify(redis).set(ri.getILoqSessionIdPrefix(), expectedSessionId);
    }

    @Test
    @DisplayName("direct:removeCurrentILoqSessionRelatedKeys")
    void testShouldRemoveILoqSessionRelatedValuesFromRedis() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        verifyNoInteractions(redis);

        template.send(removeCurrentILoqSessionRelatedKeysEndpoint, ex);

        verify(redis).del(ri.getILoqBaseUrlPrefix());
        verify(redis).del(ri.getILoqSessionIdPrefix());
        verify(redis).del(ri.getILoqCurrentCustomerCodePrefix());
        verify(redis).del(ri.getILoqCurrentCustomerCodeHasChangedPrefix());
        verify(redis).del(ri.getILoqCurrentCustomerCodePasswordPrefix());
    }

    @Test
    @DisplayName("direct:saveMappedKeys")
    void testShouldSaveTheMappedIdsForEfecteAndILoqKeys() throws Exception {
        String entityId = "12345";
        String efecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKeyEntityId", entityId);
        ex.setProperty("efecteKeyEfecteId", efecteId);
        ex.setProperty("iLoqKeyId", expectedILoqId);

        EfecteEntityIdentifier entityIdentifier = new EfecteEntityIdentifier(entityId, efecteId);
        String expectedEntityIdentifier = testUtils.writeAsJson(entityIdentifier);

        String expectedEfectePrefix = ri.getMappedKeyEfectePrefix() + efecteId;
        String expectedILoqPrefix = ri.getMappedKeyILoqPrefix() + expectedILoqId;

        when(helper.writeAsJson(entityIdentifier)).thenReturn(expectedEntityIdentifier);

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        template.send(saveMappedKeysEndpoint, ex);

        verify(redis).set(expectedEfectePrefix, expectedILoqId);
        verify(redis).set(expectedILoqPrefix, expectedEntityIdentifier);
        verify(helper).writeAsJson(entityIdentifier);
    }

    @Test
    @DisplayName("direct:savePreviousKeyInfos")
    void testShouldSaveThePreviousKeyInfos() throws Exception {
        String efecteId = "KEY-00123";
        String iLoqId = "abc-123";
        String iLoqSecurityAccessId1 = "11";
        String iLoqSecurityAccessId2 = "22";
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey("Aktiivinen", Set.of(
                iLoqSecurityAccessId1, iLoqSecurityAccessId2));
        Set<String> expectedNewILoqSecurityAccessIds = Set.of("11", "22");
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKeyEfecteId", efecteId);
        ex.setProperty("iLoqKeyId", iLoqId);
        ex.setProperty("newPreviousEfecteKey", previousEfecteKey);
        ex.setProperty("newILoqSecurityAccessIds", expectedNewILoqSecurityAccessIds);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        String expectedPreviousEfecteKey = testUtils.writeAsJson(previousEfecteKey);
        String expectedEfectePrefix = ri.getPreviousKeyEfectePrefix() + efecteId;
        String expectedILoqPrefix = ri.getPreviousKeyILoqPrefix() + iLoqId;

        when(helper.writeAsJson(previousEfecteKey)).thenReturn(expectedPreviousEfecteKey);

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        template.send(savePreviousKeyInfosEndpoint, ex);

        verify(redis).set(expectedEfectePrefix, expectedPreviousEfecteKey);
        verify(redis).del(expectedILoqPrefix);
        verify(redis).addSet(keyCaptor.capture(), valueCaptor.capture(), valueCaptor.capture());
        verify(helper).writeAsJson(previousEfecteKey);

        assertThat(keyCaptor.getValue()).isEqualTo(expectedILoqPrefix);
        assertThat(valueCaptor.getAllValues()).containsExactlyInAnyOrder(iLoqSecurityAccessId1, iLoqSecurityAccessId2);
    }

    @Test
    @DisplayName("direct:savePreviousKeyInfos")
    void testShouldNotSavePreviousILoqKeySecurityAccessesWhenTheyAreNotUpdated() throws Exception {
        String efecteId = "KEY-00123";
        String iLoqId = "abc-123";
        String iLoqSecurityAccessId1 = "11";
        String iLoqSecurityAccessId2 = "22";
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey("Aktiivinen", Set.of(
                iLoqSecurityAccessId1, iLoqSecurityAccessId2));
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKeyEfecteId", efecteId);
        ex.setProperty("iLoqKeyId", iLoqId);
        ex.setProperty("newPreviousEfecteKey", previousEfecteKey);
        ex.setProperty("newILoqSecurityAccessIds", null);

        String expectedPreviousEfecteKey = testUtils.writeAsJson(previousEfecteKey);
        String expectedEfectePrefix = ri.getPreviousKeyEfectePrefix() + efecteId;
        String expectedILoqPrefix = ri.getPreviousKeyILoqPrefix() + iLoqId;

        when(helper.writeAsJson(previousEfecteKey)).thenReturn(expectedPreviousEfecteKey);

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        template.send(savePreviousKeyInfosEndpoint, ex);

        verify(redis).set(expectedEfectePrefix, expectedPreviousEfecteKey);
        verify(redis, times(0)).del(expectedILoqPrefix);
        verify(redis, times(0)).addSet(any(), any(), any());
        verify(helper).writeAsJson(previousEfecteKey);
    }

    @Test
    @DisplayName("direct:deleteKey")
    void testShouldEmptyTheCachedKeysRelatedToADisabledKey() throws Exception {
        String efecteId = "KEY-00123";
        String iLoqId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKeyEfecteId", efecteId);
        ex.setProperty("iLoqKeyId", iLoqId);

        String timestamp = testUtils.createDatetimeNow("yyyy-MM-dd_HH:mm");
        String expectedDeletedPrefix = ri.getTempDeletedKeyPrefix() + timestamp + ":" + efecteId;
        String expectedPreviousEfecteKeyPrefix = ri.getPreviousKeyEfectePrefix() + efecteId;
        String expectedPreviousILoqKeyPrefix = ri.getPreviousKeyILoqPrefix() + iLoqId;
        String expectedMappedEfecteKeyPrefix = ri.getMappedKeyEfectePrefix() + efecteId;
        String expectedMappedILoqKeyPrefix = ri.getMappedKeyILoqPrefix() + iLoqId;

        verifyNoInteractions(redis);

        template.send(deleteKeyEndpoint, ex);

        verify(redis).setex(expectedDeletedPrefix, iLoqId, deletedKeyExpirationTime);
        verify(redis).del(expectedPreviousEfecteKeyPrefix);
        verify(redis).del(expectedPreviousILoqKeyPrefix);
        verify(redis).del(expectedMappedEfecteKeyPrefix);
        verify(redis).del(expectedMappedILoqKeyPrefix);
    }

    @Test
    @DisplayName("direct:removeTempKeys")
    void testShouldRemoveAnyTemporaryRedisKeys() throws Exception {
        Exchange ex = testUtils.createExchange();

        String key1 = "foo:bar:1";
        String key2 = "foo:bar:2";
        String key3 = "foo:bar:3";
        List<String> expectedKeys = List.of(key1, key2, key3);

        String expectedPrefix = ri.getTempEfectePersonPrefix() + "*";

        when(redis.getAllKeys(expectedPrefix)).thenReturn(expectedKeys);

        verifyNoInteractions(redis);

        template.send(removeTempKeysEndpoint, ex);

        verify(redis).getAllKeys(expectedPrefix);
        verify(redis).del(key1);
        verify(redis).del(key2);
        verify(redis).del(key3);
    }
}
