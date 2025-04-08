package fi.hel.routes;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.jackson.ListJacksonDataFormat;
import org.apache.camel.http.base.HttpOperationFailedException;

import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqPerson;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ILoqRouteBuilder extends RouteBuilder {

    @Inject
    ResourceInjector ri;

    @Override
    public void configure() throws Exception {

        //////////////////////////////////
        // Session configuration routes //
        //////////////////////////////////

        from("direct:configureILoqSession")
            .routeId("direct:configureILoqSession")
            .choice()
                .when(hasExistingValidILoqSession())
                    .to("{{app.routes.iLoq.setILoqHeaders}}")
                .otherwise()
                    .log("{{app.name}} :: configureILoqSession : Configuring new iLOQ session")
                    .to("{{app.routes.redis.getILoqCredentials}}")
                    .to("{{app.routes.iLoq.getILoqUri}}")
                    .to("{{app.routes.iLoq.createILoqSession}}")
                    .to("{{app.routes.iLoq.getILoqLockGroups}}")
                    .to("{{app.routes.iLoq.setILoqLockGroup}}")
                    .to("{{app.routes.redis.saveILoqSessionStatusToRedis}}")
            .end()
        ;

        from("direct:getILoqUri")
            .routeId("direct:getILoqUri")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("POST"),
                Exchange.CONTENT_TYPE, constant("application/json"),
                Exchange.HTTP_URI, constant("{{ILOQ_GET_URL_ADDRESS}}")
            )
            .setBody(simple("""
                    {
                        "CustomerCode": "${header.customerCode}"
                    }
                    """))
            .to("{{app.endpoints.oldhost}}")
            .unmarshal(new JacksonDataFormat(String.class))
            .setBody(simple("${body}/api/v2"))
            .to("{{app.routes.redis.saveILoqBaseUrlToRedis}}")
        ;

        from("direct:createILoqSession")
            .routeId("direct:createILoqSession")
            .setHeaders(
                Exchange.HTTP_PATH, constant("/CreateSession"),
                Exchange.HTTP_URI, simple("${body}")
            )
            .setBody(simple("""
                    {
                        "CustomerCode": "${header.customerCode}",
                        "Username": "{{ILOQ.USERNAME}}",
                        "Password": "${header.customerCodePassword}"
                    }
                    """))
            .to("{{app.endpoints.oldhost}}")
            .setBody(jsonpath("SessionID"))
            .to("{{app.routes.redis.saveILoqSessionIdToRedis}}")
        ;

        from("direct:getILoqLockGroups")
            .routeId("direct:getILoqLockGroups")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, constant("/LockGroups"),
                "SessionId", simple("${body}")
            )
            .setBody(simple(null))
            .to("{{app.endpoints.oldhost}}")
            .setBody(jsonpath("$.[0].LockGroup_ID"))
        ;

        from("direct:setILoqLockGroup")
            .routeId("direct:setILoqLockGroup")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("POST"),
                Exchange.HTTP_PATH, constant("/SetLockgroup")
            )
            .setBody(simple("""
                    {
                        "LockGroup_ID": "${body}"
                    }
                    """))
            .to("{{app.endpoints.oldhost}}")
            .setBody(simple(null))
        ;

        from("direct:setILoqHeaders")
            .routeId("direct:setILoqHeaders")
            .setHeaders(
                Exchange.HTTP_URI, method(ri.getRedis(), "get({{app.redis.prefix.iLoqCurrentBaseUrl}})"),
                Exchange.CONTENT_TYPE, constant("application/json"),
                "SessionId", method(ri.getRedis(), "get({{app.redis.prefix.iLoqCurrentSessionId}})")
            )
        ;

        from("direct:killILoqSession")
            .routeId("direct:killILoqSession")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, constant("/KillSession")
            )
            .log("{{app.name}} :: killILoqSession :: Terminating previous iLOQ session")
            .to("{{app.endpoints.oldhost}}")
        ;

        ///////////////////////////
        // Entity related routes //
        ///////////////////////////

        from("direct:listILoqKeys")
            .routeId("direct:listILoqKeys")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: listILoqKeys :: Listing iLOQ keys")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, constant("/Keys")
            )
            .to("{{app.endpoints.oldhost}}")
            .unmarshal(new ListJacksonDataFormat(ILoqKeyResponse.class))
            .convertBodyTo(List.class)
            .removeHeaders("*")
        ;

        from("direct:getILoqKeySecurityAccesses")
            .routeId("direct:getILoqKeySecurityAccesses")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, simple("/Keys/${header.iLoqKeyId}/SecurityAccesses"),
                Exchange.HTTP_QUERY, constant("mode=1")
            )
            .to("{{app.endpoints.oldhost}}")
            .setBody().jsonpath("$.SecurityAccesses")
            .marshal().json()
            .unmarshal(new ListJacksonDataFormat(ILoqSecurityAccess.class))
            .convertBodyTo(List.class)
            .removeHeaders("*")
        ;

        from("direct:listILoqPersons")
            .routeId("direct:listILoqPersons")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: listILoqPersons :: Listing iLOQ persons")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, constant("/Persons")
            )
            .to("{{app.endpoints.oldhost}}")
            .unmarshal(new ListJacksonDataFormat(ILoqPerson.class))
            .convertBodyTo(List.class)
            .log("{{app.name}} :: listILoqPersons :: Found ${body.size} persons")
        ;

        from("direct:getILoqKey")
            .routeId("direct:getILoqKey")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: getILoqKey :: Getting iLOQ key '${header.iLoqKeyId}'")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, simple("/Keys/${header.iLoqKeyId}")
            )
            .to("{{app.endpoints.oldhost}}")
            .unmarshal(new JacksonDataFormat(ILoqKeyResponse.class))
            .convertBodyTo(ILoqKeyResponse.class)
        ;

        from("direct:getILoqPerson")
            .routeId("direct:getILoqPerson")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: getILoqPerson :: Getting iLOQ person '${header.iLoqPersonId}'")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, simple("/Persons/${header.iLoqPersonId}")
            )
            .setBody(simple(null))
            .to("{{app.endpoints.oldhost}}")
            .unmarshal(new JacksonDataFormat(ILoqPerson.class))
            .convertBodyTo(ILoqPerson.class)
        ;

        from("direct:getILoqPersonByExternalId")
            .routeId("direct:getILoqPersonByExternalId")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: getILoqPersonByExternalId :: Getting iLOQ person by Efecte person entity id '${header.externalPersonId}'")
            .setHeaders(
                Exchange.HTTP_METHOD, constant("GET"),
                Exchange.HTTP_PATH, constant("/Persons/GetByExternalPersonIds"),
                Exchange.HTTP_QUERY, simple("externalPersonIds=${header.externalPersonId}")
            )
            .doTry()
                .to("{{app.endpoints.oldhost}}")
                .doCatch(HttpOperationFailedException.class)
                    .choice()
                        .when(resultContainsNoPersons())
                            .log("{{app.name}} :: getILoqPersonByExternalId :: No iLOQ persons found")
                            .setBody(constant("[]"))
                            .removeProperty(Exchange.EXCEPTION_CAUGHT)
                    .end()
                .end()
            .end()
            .unmarshal(new ListJacksonDataFormat(ILoqPerson.class))
            .convertBodyTo(List.class)
        ;

        from("direct:createILoqPerson")
            .routeId("direct:createILoqPerson")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .setBody(simple("${header.newILoqPerson}"))
            .log("{{app.name}} :: createILoqPerson :: Creating a new iLOQ person '${body.getPerson().getFirstName().substring(0, 1)}. ${body.getPerson().getLastName()}'")
            .marshal().json()
            .setHeaders(
                Exchange.HTTP_METHOD, constant("POST"),
                Exchange.HTTP_PATH, constant("/Persons")
            )
            .to("{{app.endpoints.oldhost}}")
            .log("{{app.name}} :: createILoqPerson :: Creating iLOQ person succeeded")
            .setBody(jsonpath("$.PersonIds.[0]"))
        ;

        from("direct:processILoqKey")
            .routeId("direct:processILoqKey")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: processILoqKey :: Processing an iLOQ key, operation: ${header.operation}")
            .setBody(simple("${header.iLoqPayload}"))
            .marshal().json()
            .setHeaders(
                Exchange.HTTP_METHOD, simple("${header.method}"),
                Exchange.HTTP_PATH, constant("/Keys")
            )
            .doTry()
                .to("{{app.endpoints.oldhost}}")
            .endDoTry()
            .doCatch(Exception.class)
                .setProperty("auditMessage", simple("Processing an iLOQ key failed (operation: ${header.operation}). Server response: ${exchangeProperty[CamelExceptionCaught].getResponseBody()}"))
                .bean(ri.getAuditExceptionProcessor(),
                        "throwAuditException(${header.from}, ${header.to}, ${header.efecteKeyEntityId}, ${header.efecteKeyEfecteId}, ${header.iLoqKeyId}, ${header.auditMessage})")
            .end()
            .log("{{app.name}} :: processILoqKey :: Processing succeeded")
            .removeHeaders("*")
        ;

        from("direct:updateILoqKeySecurityAccesses")
            .routeId("direct:updateILoqKeySecurityAccesses")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: updateILoqKeySecurityAccesses :: Updating an iLOQ key '${header.iLoqKeyId}' security accesses")
            // Transform values from [abc-123, xyz-456] to ["abc-123","xyz-456"]
            .setBody(simple("""
                    {
                        "SecurityAccessIds": ${exchangeProperty.newILoqSecurityAccessIds.toString().replaceAll("([^,\\[\\]]+)", "\\"$1\\"").replaceAll("\\s+", "")}
                    }
                    """))
            .setHeaders(
                Exchange.HTTP_METHOD, constant("PUT"),
                Exchange.HTTP_PATH, simple("/Keys/${header.iLoqKeyId}/SecurityAccesses")
            )
            .doTry()
                .to("{{app.endpoints.oldhost}}")
            .endDoTry()
            .doCatch(Exception.class)
                .setProperty("auditMessage", simple("Updating iLOQ security accesses failed. Server response: ${exchangeProperty[CamelExceptionCaught].getResponseBody()}"))
                .bean(ri.getAuditExceptionProcessor(),
                        "throwAuditException(" + EnumDirection.EFECTE + ", " + EnumDirection.ILOQ + ", ${header.efecteKeyEntityId}, ${header.efecteKeyEfecteId}, ${header.iLoqKeyId}, ${header.auditMessage})")
            .end()
            .log("{{app.name}} :: updateILoqKeySecurityAccesses :: Updating succeeded")
            .removeHeaders("*")
        ;

        from("direct:updateMainZone")
            .routeId("direct:updateMainZone")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .log("{{app.name}} :: updateMainZone :: Updating iLOQ key main zone to '${header.mainZoneId}'")
            .setBody(simple("""
                    {
                        "Zone_ID": "${header.mainZoneId}"
                    }
                    """))
            .setHeaders(
                Exchange.HTTP_METHOD, simple("PUT"),
                Exchange.HTTP_PATH, simple("/Keys/${header.iLoqKeyId}/UpdateMainZone")
            )
            .to("{{app.endpoints.oldhost}}")
            .log("{{app.name}} :: updateMainZone :: Updating succeeded")
            .removeHeaders("*")
        ;

        from("direct:canOrderKey")
            .routeId("direct:canOrderKey")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .setHeaders(
                Exchange.HTTP_METHOD, simple("GET"),
                Exchange.HTTP_PATH, simple("/Keys/${header.iLoqKeyId}/CanOrder")
            )
            .to("{{app.endpoints.oldhost}}")
            .choice()
                .when(body().isEqualTo("0"))
                    .setProperty("canOrder", constant(true))
                .otherwise()
                    .choice()
                        .when(body().isEqualTo("1"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key has changes which require iLOQ Manager + token to order.")
                        .when(body().isEqualTo("3"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key can't be ordered because the license limit has been exceeded. Return keys or contact iLOQ to acquire more licenses.")
                        .when(body().isEqualTo("4"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key is in wrong state. Only keys in planning state can be ordered.")
                        .when(body().isEqualTo("5"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key's id is too large.")
                        .when(body().isEqualTo("6"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key has security access outside its zones. This can only occur if the key is a new key.")
                        .when(body().isEqualTo("7"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key has time limit outside its zones. This can only occur if the key is a new key.")
                        .when(body().isEqualTo("8"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key is in block list.")
                        .when(body().isEqualTo("11"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key has too many timelimits defined.")
                        .when(body().isEqualTo("12"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key main zone is not set and is required.")
                        .when(body().isEqualTo("13"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: key doesn't have a person attached to it")
                        .when(body().isEqualTo("14"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: external Key doesn't have TagKey set")
                        .when(body().isEqualTo("-1"))
                            .log("{{app.name}} :: canOrderKey :: Cannot order the key, reason: error occurred during checking")
                    .endChoice()
            .end()
        ;

        from("direct:orderKey")
            .routeId("direct:orderKey")
            .to("{{app.routes.iLoq.configureILoqSession}}")
            .setHeaders(
                Exchange.HTTP_METHOD, simple("POST"),
                Exchange.HTTP_PATH, simple("/Keys/${header.iLoqKeyId}/Order")
            )
            .setBody(simple(null))
            .to("{{app.endpoints.oldhost}}")
        ;
    }

    private Predicate hasExistingValidILoqSession() {
        return method(ri.getRedis(), "get({{app.redis.prefix.iLoqCurrentCustomerCodeHasChanged}})").isEqualTo("false");
    }

    private Predicate resultContainsNoPersons() {
        return simple("${exchangeProperty[CamelExceptionCaught].getResponseBody()}").contains("Invalid value");
    }
}
