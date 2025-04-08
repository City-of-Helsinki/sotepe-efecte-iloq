package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opentest4j.AssertionFailedError;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqPerson;
import fi.hel.models.ILoqPersonImport;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.processors.AuditExceptionProcessor;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(ILoqRouteBuilderTest.class)
@SuppressWarnings("unchecked")
public class ILoqRouteBuilderTest extends CamelQuarkusTestSupport {

    @Inject
    MockEndpointInjector mocked;
    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;

    @InjectMock
    Redis redis;
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;

    @ConfigProperty(name = "ILOQ_GET_URL_ADDRESS")
    String iLoqGetUrlAddress;
    @ConfigProperty(name = "ILOQ_USERNAME")
    String iLoqUsername;

    private String configureILoqSessionEndpoint = "direct:configureILoqSession";
    private String getILoqUriEndpoint = "direct:getILoqUri";
    private String createILoqSessionEndpoint = "direct:createILoqSession";
    private String getILoqLockGroupsEndpoint = "direct:getILoqLockGroups";
    private String setILoqLockGroupEndpoint = "direct:setILoqLockGroup";
    private String setILoqHeadersEndpoint = "direct:setILoqHeaders";
    private String killILoqSessionEndpoint = "direct:killILoqSession";
    private String listILoqPersonsEndpoint = "direct:listILoqPersons";
    private String getILoqPersonEndpoint = "direct:getILoqPerson";
    private String createILoqPersonEndpoint = "direct:createILoqPerson";
    private String listILoqKeysEndpoint = "direct:listILoqKeys";
    private String getILoqKeySecurityAccessesEndpoint = "direct:getILoqKeySecurityAccesses";
    private String updateILoqKeySecurityAccessesEndpoint = "direct:updateILoqKeySecurityAccesses";
    private String getILoqPersonByExternalIdEndpoint = "direct:getILoqPersonByExternalId";
    private String processILoqKeyEndpoint = "direct:processILoqKey";
    private String getILoqKeyEndpoint = "direct:getILoqKey";
    private String updateMainZoneEndpoint = "direct:updateMainZone";
    private String canOrderKeyEndpoint = "direct:canOrderKey";
    private String orderKeyEndpoint = "direct:orderKey";

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
        testUtils.addFakeExceptionHandler("mock:defaultExceptionHandler");
        testUtils.addMockEndpointsTo(mockEndpoint,
                getILoqUriEndpoint,
                createILoqSessionEndpoint,
                getILoqLockGroupsEndpoint,
                setILoqLockGroupEndpoint,
                setILoqHeadersEndpoint,
                listILoqPersonsEndpoint,
                getILoqPersonEndpoint,
                createILoqPersonEndpoint,
                listILoqKeysEndpoint,
                getILoqKeySecurityAccessesEndpoint,
                killILoqSessionEndpoint,
                getILoqPersonByExternalIdEndpoint,
                updateILoqKeySecurityAccessesEndpoint,
                getILoqKeyEndpoint,
                processILoqKeyEndpoint);
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();
        mock = getMockEndpoint(mockEndpoint);
    }

    ////////////////////
    // Any iLOQ route //
    ////////////////////

    @Test
    @DisplayName("any iLOQ route")
    void testShouldSetTheHttpMethodAsGET() throws Exception {
        List<String> endpoints = List.of(
                listILoqKeysEndpoint,
                getILoqKeySecurityAccessesEndpoint,
                listILoqPersonsEndpoint,
                getILoqPersonEndpoint,
                getILoqLockGroupsEndpoint,
                killILoqSessionEndpoint,
                getILoqPersonByExternalIdEndpoint,
                getILoqKeyEndpoint,
                canOrderKeyEndpoint);

        for (String endpointUri : endpoints) {
            Exchange ex = testUtils.createExchange(null);
            mocked.getOldhost().expectedMessageCount(1);
            mocked.getOldhost().expectedHeaderReceived("CamelHttpMethod", "GET");

            template.send(endpointUri, ex);

            mocked.getOldhost().assertIsSatisfied();
            mocked.getOldhost().reset();
        }
    }

    @Test
    @DisplayName("any iLOQ route")
    void testShouldSetTheHttpMethodAsPOST() throws Exception {
        List<String> endpoints = List.of(
                getILoqUriEndpoint,
                setILoqLockGroupEndpoint,
                createILoqPersonEndpoint,
                orderKeyEndpoint);

        for (String endpointUri : endpoints) {
            Exchange ex = testUtils.createExchange(null);
            mocked.getOldhost().expectedMessageCount(1);
            mocked.getOldhost().expectedHeaderReceived("CamelHttpMethod", "POST");

            template.send(endpointUri, ex);

            mocked.getOldhost().assertIsSatisfied();
            mocked.getOldhost().reset();
        }
    }

    @Test
    @DisplayName("any iLOQ route")
    void testShouldSetTheHttpMethodAsPUT() throws Exception {
        List<String> endpoints = List.of(
                updateMainZoneEndpoint);

        for (String endpointUri : endpoints) {
            Exchange ex = testUtils.createExchange(null);
            mocked.getOldhost().expectedMessageCount(1);
            mocked.getOldhost().expectedHeaderReceived("CamelHttpMethod", "PUT");

            template.send(endpointUri, ex);

            mocked.getOldhost().assertIsSatisfied();
            mocked.getOldhost().reset();
        }
    }

    @Test
    @DisplayName("any iLOQ route")
    void testShouldSetThePayloadAsNull() throws Exception {
        String expectedPayload = null;
        List<String> endpoints = List.of(
                getILoqLockGroupsEndpoint,
                getILoqPersonEndpoint,
                orderKeyEndpoint);

        for (String endpointUri : endpoints) {
            Exchange ex = testUtils.createExchange("foobar");

            mocked.getOldhost().expectedMessageCount(1);
            mocked.getOldhost().expectedBodiesReceived(expectedPayload);

            template.send(endpointUri, ex);

            mocked.getOldhost().assertIsSatisfied();
            mocked.getOldhost().reset();
        }
    }

    @Test
    @DisplayName("any iLOQ route")
    void testShouldSetTheHttpPath() throws Exception {
        List<Map<String, String>> endpoints = List.of(
                Map.of("endpoint", createILoqSessionEndpoint,
                        "path", "/CreateSession"),
                Map.of("endpoint", getILoqLockGroupsEndpoint,
                        "path", "/LockGroups"),
                Map.of("endpoint", setILoqLockGroupEndpoint,
                        "path", "/SetLockgroup"),
                Map.of("endpoint", listILoqKeysEndpoint,
                        "path", "/Keys"),
                Map.of("endpoint", listILoqPersonsEndpoint,
                        "path", "/Persons"),
                Map.of("endpoint", createILoqPersonEndpoint,
                        "path", "/Persons"),
                Map.of("endpoint", killILoqSessionEndpoint,
                        "path", "/KillSession"),
                Map.of("endpoint", getILoqPersonByExternalIdEndpoint,
                        "path", "/Persons/GetByExternalPersonIds"),
                Map.of("endpoint", processILoqKeyEndpoint,
                        "path", "/Keys"));

        for (Map<String, String> endpoint : endpoints) {
            Exchange ex = testUtils.createExchange(null);
            mocked.getOldhost().expectedMessageCount(1);
            mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, endpoint.get("path"));

            template.send(endpoint.get("endpoint"), ex);

            mocked.getOldhost().assertIsSatisfied();
            mocked.getOldhost().reset();
        }
    }

    //////////////////////////////////
    // Session configuration routes //
    //////////////////////////////////

    @Test
    @DisplayName("direct:getILoqUri")
    void testShouldSetTheContentType_GetILoqUri() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived("Content-Type", "application/json");

        template.send(getILoqUriEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqUri")
    void testShouldSetTheGetUrlAddress() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived("CamelHttpUri", iLoqGetUrlAddress);

        template.send(getILoqUriEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqUri")
    void testShouldSetThePayload_GetILoqUri() throws Exception {
        String customerCode = "ILOQ_12345";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("customerCode", customerCode);

        String expectedPayload = """
                {
                    "CustomerCode": "%s"
                }
                """.formatted(customerCode).trim();

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedBodiesReceived(expectedPayload);

        template.send(getILoqUriEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqUri")
    void testShouldSaveTheModifiedILoqBaseUrlToRedis() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String responseBody = "\"www.foobar.com\"";
        String expectedBody = "www.foobar.com/api/v2";

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(responseBody));

        mocked.getSaveILoqBaseUrlToRedis().expectedMessageCount(1);
        mocked.getSaveILoqBaseUrlToRedis().expectedBodiesReceived(expectedBody);

        template.send(getILoqUriEndpoint, ex);

        mocked.getSaveILoqBaseUrlToRedis().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:createILoqSession")
    void testShouldSetTheHttpUri_CreateILoqSession() throws Exception {
        String expectedHttpUri = "www.foobar.com";
        Exchange ex = testUtils.createExchange(expectedHttpUri);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived("CamelHttpUri", expectedHttpUri);

        template.send(createILoqSessionEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:createILoqSession")
    void testShouldSetThePayload_CreateILoqSession() throws Exception {
        String customerCode = "ILOQ_12345";
        String customerCodePassword = "super_secret";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("customerCode", customerCode);
        ex.setProperty("customerCodePassword", customerCodePassword);

        String expectedPayload = """
                {
                    "CustomerCode": "%s",
                    "Username": "%s",
                    "Password": "%s"
                }
                """.formatted(
                customerCode,
                iLoqUsername,
                customerCodePassword)
                .trim();

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedBodiesReceived(expectedPayload);

        template.send(createILoqSessionEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:createILoqSession")
    void testShouldSaveTheReceivedILoqSessionIdToRedis() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        String expectedSessionId = "a5f0f07b-f5db-4663-960d-0547319b8322";

        String fakeResponse = """
                {
                    "Result": 0,
                    "SessionID": "%s"
                }
                """.formatted(expectedSessionId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        mocked.getSaveILoqSessionIdToRedis().expectedMessageCount(1);
        mocked.getSaveILoqSessionIdToRedis().expectedBodiesReceived(expectedSessionId);

        template.send(createILoqSessionEndpoint, ex);

        mocked.getSaveILoqSessionIdToRedis().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqLockGroups")
    void testShouldSetTheSessionIdHeader_GetILoqLockGroups() throws Exception {
        String expectedSessionId = "a5f0f07b-f5db-4663-960d-0547319b8322";
        Exchange ex = testUtils.createExchange(expectedSessionId);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived("SessionId", expectedSessionId);

        template.send(getILoqLockGroupsEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqLockGroups")
    void testShouldSetTheLockGroupIdAsTheOutBody() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedLockGroupId = "5661ca63-39aa-4a09-9b37-a87e20958de1";
        String fakeResponse = """
                [
                    {
                        "LockGroup_ID": "%s",
                        "Irrelevant": "fields"
                    }
                ]
                """.formatted(expectedLockGroupId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived(expectedLockGroupId);

        template.send(getILoqLockGroupsEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:setILoqLockGroup")
    void testShouldSetThePayload_SetILoqLockGroup() throws Exception {
        String lockGroupId = "a5f0f07b-f5db-4663-960d-0547319b8322";
        Exchange ex = testUtils.createExchange(lockGroupId);

        String expectedPayload = """
                {
                    "LockGroup_ID": "%s"
                }
                """.formatted(lockGroupId).trim();

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedBodiesReceived(expectedPayload);

        template.send(setILoqLockGroupEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:setILoqLockGroup")
    void testShouldSetTheOutBodyAsNull() throws Exception {
        String expectedBody = null;
        Exchange ex = testUtils.createExchange("foobar");

        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived(expectedBody);

        template.send(setILoqLockGroupEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:configureILoqSession")
    void testShouldConfigureILogSessionInCorrectOrderWhenThereIsNoOngoingSession() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("counter", 1);

        mocked.getGetILoqCredentials().whenAnyExchangeReceived(exchange -> increaseCounter(exchange));
        mocked.getGetILoqUri().whenAnyExchangeReceived(exchange -> increaseCounter(exchange));
        mocked.getCreateILoqSession().whenAnyExchangeReceived(exchange -> increaseCounter(exchange));
        mocked.getGetILoqLockGroups().whenAnyExchangeReceived(exchange -> increaseCounter(exchange));
        mocked.getSetILoqLockGroup().whenAnyExchangeReceived(exchange -> increaseCounter(exchange));

        mocked.getGetILoqCredentials().expectedMessageCount(1);
        mocked.getGetILoqCredentials().expectedPropertyReceived("counter", 1);
        mocked.getGetILoqUri().expectedMessageCount(1);
        mocked.getGetILoqUri().expectedPropertyReceived("counter", 2);
        mocked.getCreateILoqSession().expectedMessageCount(1);
        mocked.getCreateILoqSession().expectedPropertyReceived("counter", 3);
        mocked.getGetILoqLockGroups().expectedMessageCount(1);
        mocked.getGetILoqLockGroups().expectedPropertyReceived("counter", 4);
        mocked.getSetILoqLockGroup().expectedMessageCount(1);
        mocked.getSetILoqLockGroup().expectedPropertyReceived("counter", 5);
        mocked.getSaveILoqSessionStatusToRedis().expectedMessageCount(1);
        mocked.getSaveILoqSessionStatusToRedis().expectedPropertyReceived("counter", 6);

        template.send(configureILoqSessionEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getGetILoqUri(),
                mocked.getCreateILoqSession(),
                mocked.getGetILoqLockGroups(),
                mocked.getSetILoqLockGroup(),
                mocked.getSaveILoqSessionStatusToRedis());
    }

    @Test
    @DisplayName("direct:configureILoqSession")
    void testShouldConfigureILogSessionWhenThereIsAnInvalidOngoingSession() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getILoqCurrentCustomerCodeHasChangedPrefix())).thenReturn("true");

        mocked.getGetILoqUri().expectedMessageCount(1);
        mocked.getCreateILoqSession().expectedMessageCount(1);
        mocked.getGetILoqLockGroups().expectedMessageCount(1);
        mocked.getSetILoqLockGroup().expectedMessageCount(1);

        template.send(configureILoqSessionEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getGetILoqUri(),
                mocked.getCreateILoqSession(),
                mocked.getGetILoqLockGroups(),
                mocked.getSetILoqLockGroup());
    }

    @Test
    @DisplayName("direct:configureILoqSession")
    void testShouldNotConfigureILogSessionWhenThereIsAValidOngoingSession() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getILoqCurrentCustomerCodeHasChangedPrefix())).thenReturn("false");

        mocked.getGetILoqUri().expectedMessageCount(0);
        mocked.getCreateILoqSession().expectedMessageCount(0);
        mocked.getGetILoqLockGroups().expectedMessageCount(0);
        mocked.getSetILoqLockGroup().expectedMessageCount(0);

        template.send(configureILoqSessionEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getGetILoqUri(),
                mocked.getCreateILoqSession(),
                mocked.getGetILoqLockGroups(),
                mocked.getSetILoqLockGroup());
    }

    @Test
    @DisplayName("direct:configureILoqSession")
    void testShouldSetTheHeadersWhenAValidSessionIsAvailable() throws Exception {
        String validCustomerCode = "foo";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("customerCode", validCustomerCode);

        when(redis.get(ri.getILoqCurrentCustomerCodeHasChangedPrefix())).thenReturn("false");

        mocked.getSetILoqHeaders().expectedMessageCount(1);

        template.send(configureILoqSessionEndpoint, ex);

        mocked.getSetILoqHeaders().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:setILoqHeaders")
    void testShouldSetTheHttpUri_SetILoqHeaders() throws Exception {
        String expectedHttpUri = "www.foobar.com/something/something";
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getILoqCurrentBaseUrlPrefix())).thenReturn(expectedHttpUri);

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived("CamelHttpUri", expectedHttpUri);

        template.send(setILoqHeadersEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:setILoqHeaders")
    void testShouldSetTheSessionIdHeader_SetILoqHeaders() throws Exception {
        String expectedSessionId = "a5f0f07b-f5db-4663-960d-0547319b8322";
        Exchange ex = testUtils.createExchange(null);

        when(redis.get(ri.getILoqCurrentSessionIdPrefix())).thenReturn(expectedSessionId);

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived("SessionId", expectedSessionId);

        template.send(setILoqHeadersEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:setILoqHeaders")
    void testShouldSetTheContentType_SetILoqHeaders() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived("Content-Type", "application/json");

        template.send(setILoqHeadersEndpoint, ex);

        mock.assertIsSatisfied();
    }

    ///////////////////////////
    // Entity related routes //
    ///////////////////////////

    @Test
    @DisplayName("all entity related routes")
    void testShouldConfigureTheILoqSession() throws Exception {
        List<String> entityRoutes = List.of(
                listILoqKeysEndpoint,
                getILoqKeySecurityAccessesEndpoint,
                listILoqPersonsEndpoint,
                getILoqPersonEndpoint,
                createILoqPersonEndpoint,
                killILoqSessionEndpoint,
                updateILoqKeySecurityAccessesEndpoint,
                getILoqPersonByExternalIdEndpoint,
                getILoqKeyEndpoint,
                processILoqKeyEndpoint,
                updateMainZoneEndpoint,
                canOrderKeyEndpoint,
                orderKeyEndpoint);

        for (String route : entityRoutes) {
            Exchange ex = testUtils.createExchange(null);
            mocked.getConfigureILoqSession().whenAnyExchangeReceived(exchange -> exchange.setProperty("foo", "bar"));

            mocked.getConfigureILoqSession().expectedMessageCount(1);
            mocked.getOldhost().expectedMessageCount(1);
            mocked.getOldhost().expectedPropertyReceived("foo", "bar");

            template.send(route, ex);

            MockEndpoint.assertIsSatisfied(
                    mocked.getConfigureILoqSession(),
                    mocked.getOldhost());
            MockEndpoint.resetMocks(context);
        }
    }

    @Test
    @DisplayName("direct:listILoqKeys")
    void testShouldTransformTheResponseToAListOfILoqKeyResponses() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedFNKeyId = "e2157912-cc39-4ebe-afa8-d439d1afb1ca";
        Integer expectedState = 123456;

        String fakeResponse = """
                [
                    {
                        "FNKey_ID": "%s",
                        "State": "%s"
                    }
                ]
                """.formatted(
                expectedFNKeyId,
                expectedState);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        template.send(listILoqKeysEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(List.class);

        List<Object> list = (List<Object>) response;

        for (Object item : list) {
            assertThat(item).isInstanceOf(ILoqKeyResponse.class);
        }

        ILoqKeyResponse iLoqKeyResponse = (ILoqKeyResponse) list.get(0);

        assertThat(iLoqKeyResponse.getFnKeyId()).isEqualTo(expectedFNKeyId);
        assertThat(iLoqKeyResponse.getState()).isEqualTo(expectedState);
    }

    @Test
    @DisplayName("direct:listILoqKeys")
    void testShouldRemoveHeadersAfterwards_ListILoqKeys() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("foo", "bar");

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("[]"));

        mock.expectedMessageCount(1);
        mock.expectedNoHeaderReceived();

        template.send(listILoqKeysEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqKeySecurityAccesses")
    void testShouldSetTheHttpPath_GetILoqKeySecurityAccesses() throws Exception {
        String keyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", keyId);

        String expectedHttpPath = "/Keys/" + keyId + "/SecurityAccesses";

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(getILoqKeySecurityAccessesEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqKeySecurityAccesses")
    void testShouldSetTheHttpQuery_GetILoqKeySecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedHttpQuery = "mode=1";

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_QUERY, expectedHttpQuery);

        template.send(getILoqKeySecurityAccessesEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqKeySecurityAccesses")
    void testShouldTransformTheResponseToAListOfILoqSecurityAccesses_GetILoqKeySecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedName = "102 Kuntoittava arviointi";
        String expectedRealEstateId = "123456";
        String expectedSecurityAccessId = "63613";
        String expectedZoneId = "54721";

        String fakeResponse = """
                {
                    "SecurityAccesses": [
                        {
                            "Name": "%s",
                            "RealEstate_ID": "%s",
                            "SecurityAccess_ID": "%s",
                            "Zone_ID": "%s"
                        }
                    ]
                }
                """.formatted(
                expectedName,
                expectedRealEstateId,
                expectedSecurityAccessId,
                expectedZoneId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        template.send(getILoqKeySecurityAccessesEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(List.class);

        List<Object> list = (List<Object>) response;

        for (Object item : list) {
            assertThat(item).isInstanceOf(ILoqSecurityAccess.class);
        }

        ILoqSecurityAccess iLoqSecurityAccess = (ILoqSecurityAccess) list.get(0);

        assertThat(iLoqSecurityAccess.getName()).isEqualTo(expectedName);
        assertThat(iLoqSecurityAccess.getRealEstateId()).isEqualTo(expectedRealEstateId);
    }

    @Test
    @DisplayName("direct:getILoqKeySecurityAccesses")
    void testShouldRemoveHeadersAfterwards_GetILoqKeySecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("foo", "bar");

        String fakeResponse = """
                {
                    "SecurityAccesses": [
                        {
                            "Name": "irrelevant",
                            "RealEstate_ID": "irrelevant",
                            "SecurityAccess_ID": "irrelevant",
                            "Zone_ID": "irrelevant"
                        }
                    ]
                }
                """;

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        mock.expectedMessageCount(1);
        mock.expectedNoHeaderReceived();

        template.send(getILoqKeySecurityAccessesEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:listILoqPersons")
    void testShouldTransformTheResponseToAListOfILoqPersons_ListILoqPersons() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedAddress = "Kotikatu 1";
        String expectedExternalPersonId = "123456";
        String expectedFirstName = "John";
        String expectedLastName = "Smith";
        String expectedPersonId = "6485ffbd-c9ed-44db-a3e0-ffe7386b0383";

        String fakeBody = """
                [
                    {
                        "Address": "%s",
                        "FirstName": "%s",
                        "LastName": "%s",
                        "Person_ID": "%s",
                        "ExternalPersonId": "%s"
                    }
                ]
                """.formatted(
                expectedAddress,
                expectedFirstName,
                expectedLastName,
                expectedPersonId,
                expectedExternalPersonId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeBody));

        template.send(listILoqPersonsEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(List.class);

        List<Object> list = (List<Object>) response;

        for (Object item : list) {
            assertThat(item).isInstanceOf(ILoqPerson.class);
        }

        ILoqPerson iLoqPerson = (ILoqPerson) list.get(0);

        assertThat(iLoqPerson.getAddress()).isEqualTo(expectedAddress);
        assertThat(iLoqPerson.getFirstName()).isEqualTo(expectedFirstName);
        assertThat(iLoqPerson.getLastName()).isEqualTo(expectedLastName);
        assertThat(iLoqPerson.getPersonId()).isEqualTo(expectedPersonId);
        assertThat(iLoqPerson.getExternalPersonId()).isEqualTo(expectedExternalPersonId);
    }

    @Test
    @DisplayName("direct:getILoqPerson")
    void testShouldSetTheHttpPath_GetILoqPerson() throws Exception {
        String personId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqPersonId", personId);

        String expectedHttpPath = "/Persons/" + personId;

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(getILoqPersonEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqPerson")
    void testShouldTransformTheResponseToAnILoqPerson() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedAddress = "Kotikatu 1";
        String expectedExternalPersonId = "123456";
        String expectedFirstName = "John";
        String expectedLastName = "Smith";
        String expectedPersonId = "6485ffbd-c9ed-44db-a3e0-ffe7386b0383";

        String fakeBody = """
                {
                    "Address": "%s",
                    "FirstName": "%s",
                    "LastName": "%s",
                    "Person_ID": "%s",
                    "ExternalPersonId": "%s"
                }
                """.formatted(
                expectedAddress,
                expectedFirstName,
                expectedLastName,
                expectedPersonId,
                expectedExternalPersonId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeBody));

        template.send(getILoqPersonEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(ILoqPerson.class);

        ILoqPerson iLoqPerson = (ILoqPerson) response;

        assertThat(iLoqPerson.getAddress()).isEqualTo(expectedAddress);
        assertThat(iLoqPerson.getFirstName()).isEqualTo(expectedFirstName);
        assertThat(iLoqPerson.getLastName()).isEqualTo(expectedLastName);
        assertThat(iLoqPerson.getPersonId()).isEqualTo(expectedPersonId);
        assertThat(iLoqPerson.getExternalPersonId()).isEqualTo(expectedExternalPersonId);
    }

    @Test
    @DisplayName("direct:createILoqPerson")
    void testShouldSetTheBodyFromThePropertyValue() throws Exception {
        ILoqPersonImport iLoqPersonImport = new ILoqPersonImport();
        ILoqPerson iLoqPerson = new ILoqPerson("John", "smith", "personId");
        iLoqPersonImport.setPerson(iLoqPerson);

        String expectedPayload = testUtils.writeAsJson(iLoqPersonImport);

        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("newILoqPerson", iLoqPersonImport);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedBodiesReceived(expectedPayload);

        template.send(createILoqPersonEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:createILoqPerson")
    void testShouldTransformTheResponseToAPersonId() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        String expectedPersonId = "6485ffbd-c9ed-44db-a3e0-ffe7386b0383";
        String fakeBody = """
                {
                    "PersonIds": [
                        "%s"
                    ]
                }
                """.formatted(expectedPersonId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeBody));

        template.send(createILoqPersonEndpoint, ex);

        String response = ex.getIn().getBody(String.class);

        assertThat(response).isEqualTo(expectedPersonId);
    }

    @Test
    @DisplayName("direct:processILoqKey")
    void testShouldSetThePayload() throws Exception {
        ILoqKeyImport iLoqKeyImport = new ILoqKeyImport();
        ILoqKey iLoqKey = new ILoqKey();
        iLoqKeyImport.setKey(iLoqKey);
        iLoqKey.setRealEstateId("foo");
        iLoqKey.setDescription("bar");
        iLoqKeyImport.setSecurityAccessIds(List.of("1", "2"));
        iLoqKeyImport.setZoneIds(List.of("3"));

        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqPayload", iLoqKeyImport);

        String expectedPayload = testUtils.writeAsJson(iLoqKeyImport);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedBodiesReceived(expectedPayload);

        template.send(processILoqKeyEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processILoqKey")
    void testShouldSetTheHttpMethod() throws Exception {
        ILoqKeyImport iLoqKeyImport = new ILoqKeyImport();
        String expectedHttpMethod = "foo";

        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqPayload", iLoqKeyImport);
        ex.setProperty("method", expectedHttpMethod);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_METHOD, expectedHttpMethod);

        template.send(processILoqKeyEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processILoqKey")
    void testShouldRemoveHeadersAfterwards_ProcessILoqKey() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("foo", "bar");

        mock.expectedMessageCount(1);
        mock.expectedNoHeaderReceived();

        template.send(processILoqKeyEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processILoqKey")
    void testShouldThrowAnAuditExceptionWhenProcessingAnILoqKeyFails() throws Exception {
        String expectedOperation = "create";
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqKeyId = "abc-123";
        EnumDirection expectedFrom = EnumDirection.ILOQ;
        EnumDirection expectedTo = EnumDirection.EFECTE;
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("operation", expectedOperation);
        ex.setProperty("efecteKeyEntityId", expectedEntityId);
        ex.setProperty("efecteKeyEfecteId", expectedEfecteId);
        ex.setProperty("iLoqKeyId", expectedILoqKeyId);
        ex.setProperty("from", expectedFrom);
        ex.setProperty("to", expectedTo);

        String fakeResponse = "foobar";

        String expectedAuditMessage = "Processing an iLOQ key failed (operation: " + expectedOperation
                + "). Server response: "
                + fakeResponse;

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> {
            throw new HttpOperationFailedException(
                    "http://irrelevant.uri", 500, null, null, null, fakeResponse);
        });

        verifyNoInteractions(auditExceptionProcessor);

        template.send(processILoqKeyEndpoint, ex);

        verify(auditExceptionProcessor).throwAuditException(
                expectedFrom, expectedTo, expectedEntityId,
                expectedEfecteId, expectedILoqKeyId, expectedAuditMessage);
    }

    @Test
    @DisplayName("direct:updateILoqKeySecurityAccesses")
    void testShouldSetTheUpdatedSecurityAccessesBodyFromAPropertyValue() throws Exception {
        String iLoqSecurityAccessId1 = "c835beff-65cc-45b3-a8cf-5f00afd1f8de";
        String iLoqSecurityAccessId2 = "805f6b94-ceca-44dd-8431-a9711028b4ee";
        Set<String> newILoqSecurityAccessIds = Set.of(iLoqSecurityAccessId1, iLoqSecurityAccessId2);
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("newILoqSecurityAccessIds", newILoqSecurityAccessIds);

        mocked.getOldhost().expectedMessageCount(1);

        template.send(updateILoqKeySecurityAccessesEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();

        String payload = ex.getIn().getBody(String.class);

        String expectedValue1 = """
                {
                    "SecurityAccessIds": ["%s","%s"]
                }
                """.formatted(iLoqSecurityAccessId1, iLoqSecurityAccessId2).trim();
        String expectedValue2 = """
                {
                    "SecurityAccessIds": ["%s","%s"]
                }
                """.formatted(iLoqSecurityAccessId2, iLoqSecurityAccessId1).trim();

        try {
            assertThat(payload).isEqualTo(expectedValue1);
        } catch (AssertionFailedError e) {
            assertThat(payload).isEqualTo(expectedValue2);
        }
    }

    @Test
    @DisplayName("direct:updateILoqKeySecurityAccesses")
    void testShouldSetTheHttpPath_UpdateILoqKeySecurityAccesses() throws Exception {
        String keyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", keyId);

        String expectedHttpPath = "/Keys/" + keyId + "/SecurityAccesses";

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(updateILoqKeySecurityAccessesEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:updateILoqKeySecurityAccesses")
    void testShouldSetTheHttpMethod_UpdateILoqKeySecurityAccesses() throws Exception {
        String keyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", keyId);

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");

        template.send(updateILoqKeySecurityAccessesEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:updateILoqKeySecurityAccesses")
    void testShouldThrowAnAuditExceptionWhenUpdatingSecurityAccessesFails() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqKeyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("efecteKeyEntityId", expectedEntityId);
        ex.setProperty("efecteKeyEfecteId", expectedEfecteId);
        ex.setProperty("iLoqKeyId", expectedILoqKeyId);

        String fakeResponse = "foobar";

        String expectedAuditMessage = "Updating iLOQ security accesses failed. Server response: " + fakeResponse;

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> {
            throw new HttpOperationFailedException(
                    "http://irrelevant.uri", 500, null, null, null, fakeResponse);
        });

        verifyNoInteractions(auditExceptionProcessor);

        template.send(updateILoqKeySecurityAccessesEndpoint, ex);

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.EFECTE, EnumDirection.ILOQ, expectedEntityId,
                expectedEfecteId, expectedILoqKeyId, expectedAuditMessage);
    }

    @Test
    @DisplayName("direct:updateILoqKeySecurityAccesses")
    void testShouldRemoveHeadersAfterwards_UpdateILoqKeySecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("foo", "bar");

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("[]"));

        mock.expectedMessageCount(1);
        mock.expectedNoHeaderReceived();

        template.send(updateILoqKeySecurityAccessesEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqPersonByExternalId")
    void testShouldSetTheHttpQuery_GetILoqPersonByExternalId() throws Exception {
        String externalPersonId = "12345";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("externalPersonId", externalPersonId);

        String expectedHttpQuery = "externalPersonIds=" + externalPersonId;

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_QUERY, expectedHttpQuery);

        template.send(getILoqPersonByExternalIdEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqPersonByExternalId")
    void testShouldTransformTheResponseToAListOfILoqPersons_GetILoqPersonByExternalId() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedAddress = "Kotikatu 1";
        String expectedExternalPersonId = "123456";
        String expectedFirstName = "John";
        String expectedLastName = "Smith";
        String expectedPersonId = "6485ffbd-c9ed-44db-a3e0-ffe7386b0383";

        String fakeBody = """
                [
                    {
                        "Address": "%s",
                        "FirstName": "%s",
                        "LastName": "%s",
                        "Person_ID": "%s",
                        "ExternalPersonId": "%s"
                    }
                ]
                """.formatted(
                expectedAddress,
                expectedFirstName,
                expectedLastName,
                expectedPersonId,
                expectedExternalPersonId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeBody));

        template.send(getILoqPersonByExternalIdEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(List.class);

        List<Object> list = (List<Object>) response;

        for (Object item : list) {
            assertThat(item).isInstanceOf(ILoqPerson.class);
        }

        ILoqPerson iLoqPerson = (ILoqPerson) list.get(0);

        assertThat(iLoqPerson.getAddress()).isEqualTo(expectedAddress);
        assertThat(iLoqPerson.getFirstName()).isEqualTo(expectedFirstName);
        assertThat(iLoqPerson.getLastName()).isEqualTo(expectedLastName);
        assertThat(iLoqPerson.getPersonId()).isEqualTo(expectedPersonId);
        assertThat(iLoqPerson.getExternalPersonId()).isEqualTo(expectedExternalPersonId);
    }

    @Test
    @DisplayName("direct:getILoqPersonByExternalId")
    void testShouldReturnAnEmptyListWhenNoMatchIsFound() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String expectedExternalPersonId = "123456";

        String fakeBody = """
                {
                    "Code": "ValidationFailed",
                    "Message": "Invalid value (%s) for parameter externalPersonIds."
                }
                """.formatted(expectedExternalPersonId);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> {
            exchange.setException(new HttpOperationFailedException(
                    "irrelevant", 400, "irrelevant", "irrelevant", null, fakeBody));
        });

        template.send(getILoqPersonByExternalIdEndpoint, ex);

        List<ILoqPerson> response = ex.getIn().getBody(List.class);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("direct:getILoqPersonByExternalId")
    void testShouldHandleTheExceptionLocally() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        String fakeBody = """
                {
                    "Code": "ValidationFailed",
                    "Message": "Invalid value (irrelevant) for parameter externalPersonIds."
                }
                """;

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> {
            exchange.setException(new HttpOperationFailedException(
                    "irrelevant", 400, "irrelevant", "irrelevant", null, fakeBody));
        });

        mock.expectedMessageCount(1);

        template.send(getILoqPersonByExternalIdEndpoint, ex);

        mock.assertIsSatisfied();

        Object caughtException = ex.getProperty(Exchange.EXCEPTION_CAUGHT);

        assertThat(caughtException).isNull();
    }

    @Test
    @DisplayName("direct:getILoqKey")
    void testShouldSetTheHttpPath_GetILoqKey() throws Exception {
        String iLoqKeyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", iLoqKeyId);

        String expectedHttpPath = "/Keys/" + iLoqKeyId;

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(getILoqKeyEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:getILoqKey")
    void testShouldTransformTheResponseToAnILoqKeyResponse() throws Exception {
        String expectedFnKeyId = "1";
        String expectedDescription = "key description";
        String expectedPersonId = "2";
        String expectedRealEstateId = "3";
        Integer expcetedState = 1;
        String fakeResponse = """
                {
                    "Description": "%s",
                    "FNKey_ID": "%s",
                    "Person_ID": "%s",
                    "RealEstate_ID": "%s",
                    "State": %s
                }
                    """.formatted(
                expectedDescription,
                expectedFnKeyId,
                expectedPersonId,
                expectedRealEstateId,
                expcetedState);

        Exchange ex = testUtils.createExchange(null);

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        template.send(getILoqKeyEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(ILoqKeyResponse.class);

        ILoqKeyResponse iLoqKeyResponse = (ILoqKeyResponse) response;

        assertThat(iLoqKeyResponse.getDescription()).isEqualTo(expectedDescription);
        assertThat(iLoqKeyResponse.getFnKeyId()).isEqualTo(expectedFnKeyId);
        assertThat(iLoqKeyResponse.getPersonId()).isEqualTo(expectedPersonId);
        assertThat(iLoqKeyResponse.getRealEstateId()).isEqualTo(expectedRealEstateId);
        assertThat(iLoqKeyResponse.getState()).isEqualTo(expcetedState);
    }

    @Test
    @DisplayName("direct:updateMainZone")
    void testShouldSetTheHttpPath_UpdateMainZone() throws Exception {
        String keyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", keyId);

        String expectedHttpPath = "/Keys/" + keyId + "/UpdateMainZone";

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(updateMainZoneEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:updateMainZone")
    void testShouldSetThePayload_UpdateMainZone() throws Exception {
        String mainZoneId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("mainZoneId", mainZoneId);

        String expectedPayload = """
                {
                    "Zone_ID": "%s"
                }
                """.formatted(mainZoneId).trim();

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedBodiesReceived(expectedPayload);

        template.send(updateMainZoneEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:canOrderKey")
    void testShouldSetTheHttpPath_CanOrderKey() throws Exception {
        String keyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", keyId);

        String expectedHttpPath = "/Keys/" + keyId + "/CanOrder";

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(canOrderKeyEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:canOrderKey")
    void testShouldSetThePropertyValueAsTrueWhenAKeyCanBeOrdered() throws Exception {
        Exchange ex = testUtils.createExchange("foo");

        String fakeResponse = "0";

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        template.send(canOrderKeyEndpoint, ex);

        boolean canOrder = ex.getProperty("canOrder", boolean.class);

        assertThat(canOrder).isTrue();
    }

    @Test
    @DisplayName("direct:canOrderKey")
    void testShouldSetThePropertyValueAsFalseWhenAKeyCanNotBeOrdered() throws Exception {
        Exchange ex = testUtils.createExchange("foo");

        String fakeResponse = "1";

        mocked.getOldhost().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        template.send(canOrderKeyEndpoint, ex);

        boolean canOrder = ex.getProperty("canOrder", boolean.class);

        assertThat(canOrder).isFalse();
    }

    @Test
    @DisplayName("direct:orderKey")
    void testShouldSetTheHttpPath_OrderKey() throws Exception {
        String keyId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("iLoqKeyId", keyId);

        String expectedHttpPath = "/Keys/" + keyId + "/Order";

        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(orderKeyEndpoint, ex);

        mocked.getOldhost().assertIsSatisfied();
    }

    private void increaseCounter(Exchange ex) {
        Integer counter = ex.getProperty("counter", Integer.class);
        counter++;
        ex.setProperty("counter", counter);
    }

}
