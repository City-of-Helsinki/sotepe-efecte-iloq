package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntitySet;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.AuditExceptionProcessor;
import fi.hel.processors.Helper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(EfecteRouteBuilderTest.class)
@SuppressWarnings("unchecked")
public class EfecteRouteBuilderTest extends CamelQuarkusTestSupport {

    @Inject
    MockEndpointInjector mocked;
    @Inject
    TestUtils testUtils;

    @InjectMock
    Helper helper;
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;

    @ConfigProperty(name = "EFECTE_BASE_URL")
    String efecteUrl;
    @ConfigProperty(name = "EFECTE_USERNAME")
    String efecteUsername;
    @ConfigProperty(name = "EFECTE_PASSWORD")
    String efectePassword;

    private String setEfecteAuthorizationEndpoint = "direct:setEfecteAuthorization";
    private String sendEfecteRequestEndpoint = "direct:sendEfecteRequest";
    private String getEfecteEntityEndpoint = "direct:getEfecteEntity";
    private String convertToEfecteEntityEndpoint = "direct:convertToEfecteEntity";
    private String processEfecteRequestEndpoint = "direct:processEfecteRequest";

    private String mockEndpoint = "mock:mockEndpoint";
    private MockEndpoint mock;
    private String httpMockEndpoint = "mock:httpMockEndpoint";
    private MockEndpoint sendTrustedHTTPRequestMock;

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
        testUtils.addFakeExceptionHandler();
        testUtils.addMockEndpointsTo(mockEndpoint,
                setEfecteAuthorizationEndpoint,
                sendEfecteRequestEndpoint);
        mockHTTPEndpoint();
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();
        mock = getMockEndpoint(mockEndpoint);
        sendTrustedHTTPRequestMock = getMockEndpoint(httpMockEndpoint);
    }

    @Test
    @DisplayName("direct:setEfecteHeaders")
    void testShouldSetTheAuthentication() throws Exception {
        String expectedCredentials = efecteUsername + ":" + efectePassword;
        String base64EncodedCredentials = "dXNlcm5hbWU6cGFzc3dvcmQ=";
        String expectedAuthorization = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

        Exchange ex = testUtils.createExchange(null);

        when(helper.encodeToBase64(expectedCredentials)).thenReturn(base64EncodedCredentials);

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived("Authorization", expectedAuthorization);
        verifyNoInteractions(helper);

        template.send(setEfecteAuthorizationEndpoint, ex);

        mock.assertIsSatisfied();
        verify(helper).encodeToBase64(expectedCredentials);
    }

    @Test
    @DisplayName("Efecte routes")
    void testShouldSetTheHttpMethodAndPathHeaders() throws Exception {
        List<Map<String, String>> endpoints = List.of(
                Map.of("endpoint", getEfecteEntityEndpoint,
                        "path", "/search.ws",
                        "method", "GET"));

        for (Map<String, String> endpoint : endpoints) {
            Exchange ex = testUtils.createExchange(null);
            mocked.getSendEfecteRequest().expectedMessageCount(1);
            mocked.getSendEfecteRequest().expectedHeaderReceived(Exchange.HTTP_METHOD, endpoint.get("method"));
            mocked.getSendEfecteRequest().expectedHeaderReceived(Exchange.HTTP_PATH, endpoint.get("path"));

            template.send(endpoint.get("endpoint"), ex);

            mocked.getSendEfecteRequest().assertIsSatisfied();
            mocked.getSendEfecteRequest().reset();
        }
    }

    @Test
    @DisplayName("Efecte routes")
    void testShouldConvertTheReturnedXMLToAnEfecteEntity() throws Exception {
        List<String> endpoints = List.of(
                getEfecteEntityEndpoint);

        for (String endpoint : endpoints) {
            Exchange ex = testUtils.createExchange(null);

            mocked.getSendEfecteRequest().whenAnyExchangeReceived(exchange -> exchange.setProperty("foo", "bar"));

            mocked.getConvertToEfecteEntity().expectedMessageCount(1);
            mocked.getConvertToEfecteEntity().expectedPropertyReceived("foo", "bar");

            template.send(endpoint, ex);

            mocked.getConvertToEfecteEntity().assertIsSatisfied();
            mocked.getConvertToEfecteEntity().reset();
        }
    }

    @Test
    @DisplayName("direct:convertToEfecteEntity")
    void testShouldReturnAnEmptyListWhenNoEntitiesAreFound() throws Exception {
        String xmlBody = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <result/>
                    """;

        Exchange ex = testUtils.createExchange(xmlBody);

        template.send(convertToEfecteEntityEndpoint, ex);

        Object result = ex.getIn().getBody();

        assertThat(result).isInstanceOf(List.class);
        assertThat((List<Object>) result).isEmpty();
    }

    @Test
    @DisplayName("direct:convertToEfecteEntity")
    void testShouldConvertTheXMLBodyToAListOfEfecteEntities() throws Exception {
        String expectedEntityId = "123";
        String expectedEntityName = "Seniorikeskuksen Henkilökunta - Smith John";
        String expectedAttributeId1 = "1";
        String expectedAttributeName1 = "Katuosoite";
        String expectedAttributeCode1 = "avain_katuosoite";
        String expectedReferenceId1 = "11";
        String expectedReferenceName1 = "Kotikatu 1, 20700, Turku";
        String expectedAttributeId2 = "2";
        String expectedAttributeName2 = "Kulkualue";
        String expectedAttributeCode2 = "avain_kulkualue";
        String expectedReferenceId2_1 = "2521231";
        String expectedReferenceName2_1 = "6 - Geriatrian poliklinikka";
        String expectedReferenceId2_2 = "2383928";
        String expectedReferenceName2_2 = "Lääkehuone";
        String expectedAttributeId3 = "3";
        String expectedAttributeName3 = "Luotu";
        String expectedAttributeCode3 = "created";
        String expectedValue = "03.07.2023 08:55";

        String xmlBody = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <entityset>
                    <entity id="%s" name="%s">
                        <template id="322" name="Avain" code="avain"/>
                        <group code="avaimet"/>
                        <attribute id="%s" name="%s" code="%s">
                            <reference id="%s" name="%s"/>
                        </attribute>
                        <attribute id="%s" name="%s" code="%s">
                            <reference id="%s" name="%s"/>
                            <reference id="%s" name="%s"/>
                        </attribute>
                        <attribute id="%s" name="%s" code="%s">
                            <value>%s</value>
                        </attribute>
                    </entity>
                </entityset>
                    """.formatted(
                expectedEntityId,
                expectedEntityName,
                expectedAttributeId1,
                expectedAttributeName1,
                expectedAttributeCode1,
                expectedReferenceId1,
                expectedReferenceName1,
                expectedAttributeId2,
                expectedAttributeName2,
                expectedAttributeCode2,
                expectedReferenceId2_1,
                expectedReferenceName2_1,
                expectedReferenceId2_2,
                expectedReferenceName2_2,
                expectedAttributeId3,
                expectedAttributeName3,
                expectedAttributeCode3,
                expectedValue);

        Exchange ex = testUtils.createExchange(xmlBody);

        template.send(convertToEfecteEntityEndpoint, ex);

        Object response = ex.getIn().getBody();

        assertThat(response).isInstanceOf(List.class);

        List<Object> list = (List<Object>) response;

        for (Object item : list) {
            assertThat(item).isInstanceOf(EfecteEntity.class);
        }

        EfecteEntity efecteEntity = (EfecteEntity) list.get(0);

        assertThat(efecteEntity.getId()).isEqualTo(expectedEntityId);
        assertThat(efecteEntity.getName()).isEqualTo(expectedEntityName);
        assertThat(efecteEntity.getAttributes().get(0).getId()).isEqualTo(expectedAttributeId1);
        assertThat(efecteEntity.getAttributes().get(0).getName()).isEqualTo(expectedAttributeName1);
        assertThat(efecteEntity.getAttributes().get(0).getCode()).isEqualTo(expectedAttributeCode1);
        assertThat(efecteEntity.getAttributes().get(0).getReferences().get(0).getId())
                .isEqualTo(expectedReferenceId1);
        assertThat(efecteEntity.getAttributes().get(0).getReferences().get(0).getName())
                .isEqualTo(expectedReferenceName1);
        assertThat(efecteEntity.getAttributes().get(1).getId()).isEqualTo(expectedAttributeId2);
        assertThat(efecteEntity.getAttributes().get(1).getName()).isEqualTo(expectedAttributeName2);
        assertThat(efecteEntity.getAttributes().get(1).getCode()).isEqualTo(expectedAttributeCode2);
        assertThat(efecteEntity.getAttributes().get(1).getReferences().get(0).getId())
                .isEqualTo(expectedReferenceId2_1);
        assertThat(efecteEntity.getAttributes().get(1).getReferences().get(0).getName())
                .isEqualTo(expectedReferenceName2_1);
        assertThat(efecteEntity.getAttributes().get(1).getReferences().get(1).getId())
                .isEqualTo(expectedReferenceId2_2);
        assertThat(efecteEntity.getAttributes().get(1).getReferences().get(1).getName())
                .isEqualTo(expectedReferenceName2_2);
        assertThat(efecteEntity.getAttributes().get(2).getId()).isEqualTo(expectedAttributeId3);
        assertThat(efecteEntity.getAttributes().get(2).getName()).isEqualTo(expectedAttributeName3);
        assertThat(efecteEntity.getAttributes().get(2).getCode()).isEqualTo(expectedAttributeCode3);
        assertThat(efecteEntity.getAttributes().get(2).getValue())
                .isEqualTo(expectedValue);
    }

    @Test
    @DisplayName("direct:sendEfecteRequest")
    void testShouldSetTheEfecteAuthorization() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        mocked.getSetEfecteAuthorization().whenAnyExchangeReceived(exchange -> exchange.setProperty("foo", "bar"));

        mocked.getSetEfecteAuthorization().expectedMessageCount(1);
        sendTrustedHTTPRequestMock.expectedMessageCount(1);
        sendTrustedHTTPRequestMock.expectedPropertyReceived("foo", "bar");

        template.send(sendEfecteRequestEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getSetEfecteAuthorization(),
                sendTrustedHTTPRequestMock);
    }

    @Test
    @DisplayName("direct:sendEfecteRequest")
    void testShouldRemoveHeadersAfterwards() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("foo", "bar");

        mocked.getSetEfecteAuthorization().whenAnyExchangeReceived(exchange -> exchange.setProperty("foo", "bar"));

        mock.expectedMessageCount(1);
        mock.expectedNoHeaderReceived();

        template.send(sendEfecteRequestEndpoint, ex);

        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:sendEfecteRequest")
    void testShouldThrowAnAuditExceptionWhenEfecteRequestWasInvalid() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("entityId", expectedEntityId);
        ex.setProperty("efecteId", expectedEfecteId);
        ex.setProperty("iLoqId", expectedILoqId);

        String fakeResponse = """
                <?xml version="1.0" encoding="utf-8"?>
                <error>
                    <code>efe-3001</code>
                    <description>Invalid search query</description>
                    <details>Unknown field: Entity.ids</details>
                </error>
                    """;

        String expectedAuditMessage = "The Efecte request was invalid. Server response: " + fakeResponse;

        sendTrustedHTTPRequestMock.whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        verifyNoInteractions(auditExceptionProcessor);

        template.send(sendEfecteRequestEndpoint, ex);

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.ILOQ, EnumDirection.EFECTE, expectedEntityId,
                expectedEfecteId, expectedILoqId, expectedAuditMessage);
    }

    @Test
    @DisplayName("direct:sendEfecteRequest")
    void testShouldThrowAnAuditExceptionWhenUpdatingAnEfecteEntityFails() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        String efecteEntityType = "key";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("entityId", expectedEntityId);
        ex.setProperty("efecteId", expectedEfecteId);
        ex.setProperty("iLoqId", expectedILoqId);
        ex.setProperty("efecteOperation", "update");
        ex.setProperty("efecteEntityType", efecteEntityType);

        String fakeResponse = """
                <entity-import-report>
                    <irrelevant-1>fields</irrelevant-1>
                    <entities-updated>0</entities-updated>
                    <irrelevant-2>fields</irrelevant-2>
                </entity-import-report>
                                """;
        String expectedAuditMessage = "Updating Efecte " + efecteEntityType + " failed. Server response: "
                + fakeResponse;

        sendTrustedHTTPRequestMock.whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        verifyNoInteractions(auditExceptionProcessor);

        template.send(sendEfecteRequestEndpoint, ex);

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.ILOQ, EnumDirection.EFECTE, expectedEntityId,
                expectedEfecteId, expectedILoqId, expectedAuditMessage);
    }

    @Test
    @DisplayName("direct:sendEfecteRequest")
    void testShouldThrowAnAuditExceptionWhenCreatingAnEfecteEntityFails() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        String efecteEntityType = "key";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("entityId", expectedEntityId);
        ex.setProperty("efecteId", expectedEfecteId);
        ex.setProperty("iLoqId", expectedILoqId);
        ex.setProperty("efecteOperation", "create");
        ex.setProperty("efecteEntityType", efecteEntityType);

        String fakeResponse = """
                <entity-import-report>
                    <irrelevant-1>fields</irrelevant-1>
                    <entities-created>0</entities-created>
                    <irrelevant-2>fields</irrelevant-2>
                </entity-import-report>
                                """;
        String expectedAuditMessage = "Creating Efecte " + efecteEntityType + " failed. Server response: "
                + fakeResponse;

        sendTrustedHTTPRequestMock.whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        verifyNoInteractions(auditExceptionProcessor);

        template.send(sendEfecteRequestEndpoint, ex);

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.ILOQ, EnumDirection.EFECTE, expectedEntityId,
                expectedEfecteId, expectedILoqId, expectedAuditMessage);
    }

    @Test
    @DisplayName("direct:getEfecteEntity")
    void testShouldSetTheHttpQuery_GetEfecteEntity() throws Exception {
        String httpQuery = "foobar";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("efecteQuery", httpQuery);

        String expectedHttpQuery = "query=" + httpQuery;

        mocked.getSendEfecteRequest().expectedMessageCount(1);
        mocked.getSendEfecteRequest().expectedHeaderReceived(Exchange.HTTP_QUERY, expectedHttpQuery);

        template.send(getEfecteEntityEndpoint, ex);

        mocked.getSendEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processEfecteRequest")
    void testShouldSetTheHttpMethod_ProcessEfecteRequest() throws Exception {
        String expectedHttpMethod = "POST";
        Exchange ex = testUtils.createExchange(null);

        mocked.getSendEfecteRequest().expectedMessageCount(1);
        mocked.getSendEfecteRequest().expectedHeaderReceived(Exchange.HTTP_METHOD, expectedHttpMethod);

        template.send(processEfecteRequestEndpoint, ex);

        mocked.getSendEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processEfecteRequest")
    void testShouldSetTheHttpPath_ProcessEfecteRequest() throws Exception {
        String expectedHttpPath = "foobar";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("efectePath", expectedHttpPath);

        mocked.getSendEfecteRequest().expectedMessageCount(1);
        mocked.getSendEfecteRequest().expectedHeaderReceived(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(processEfecteRequestEndpoint, ex);

        mocked.getSendEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processEfecteRequest")
    void testShouldSetTheHttpQuery_ProcessEfecteRequest() throws Exception {
        String expectedHttpQuery = "foobar";
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("efecteQuery", expectedHttpQuery);

        mocked.getSendEfecteRequest().expectedMessageCount(1);
        mocked.getSendEfecteRequest().expectedHeaderReceived(Exchange.HTTP_QUERY, expectedHttpQuery);

        template.send(processEfecteRequestEndpoint, ex);

        mocked.getSendEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:processEfecteRequest")
    void testShouldConvertThePayloadToXml() throws Exception {
        String efecteId = "KEY-001234";
        EfecteEntitySet efectePayload = new EfecteEntitySet(List.of(
                new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.KEY).build()));

        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("efecteEntityType", "key");
        ex.setProperty("efecteOperation", "update");
        ex.setProperty("efecteId", efecteId);
        ex.setProperty("efectePayload", efectePayload);

        String expectedXmlBody = testUtils.writeAsXml(efectePayload);

        mocked.getSendEfecteRequest().expectedMessageCount(1);
        mocked.getSendEfecteRequest().expectedBodiesReceived(expectedXmlBody);

        template.send(processEfecteRequestEndpoint, ex);

        mocked.getSendEfecteRequest().assertIsSatisfied();
    }

    private void mockHTTPEndpoint() throws Exception {
        AdviceWith.adviceWith(sendEfecteRequestEndpoint, context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("https://*")
                        .skipSendToOriginalEndpoint()
                        .to(httpMockEndpoint);
            }
        });
    }
}
