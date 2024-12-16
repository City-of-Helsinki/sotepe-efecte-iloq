package fi.hel.routes;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.jacksonxml.JacksonXMLDataFormat;

import fi.hel.models.EfecteEntitySet;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EfecteRouteBuilder extends EndpointRouteBuilder {

    @Inject
    ResourceInjector ri;

    @Override
    public void configure() throws Exception {

        from("direct:setEfecteAuthorization")
            .routeId("direct:setEfecteAuthorization")
            .setHeader("Authorization", simple("Basic ${bean:helper?method=encodeToBase64({{EFECTE.USERNAME}}:{{EFECTE.PASSWORD}})}"))
        ;

        from("direct:processEfecteRequest")
            .routeId("direct:processEfecteRequest")
            .log("{{app.name}} :: processEfecteRequest :: Handling Efecte payload (Entity type: ${header.efecteEntityType}, Operation: ${header.efecteOperation}, EfecteID: ${header.efecteId})")
            .setHeaders(
                    Exchange.HTTP_METHOD, constant("POST"),
                    Exchange.HTTP_PATH, simple("${header.efectePath}"),
                    Exchange.HTTP_QUERY, simple("${header.efecteQuery}"))
            .setBody(simple("${header.efectePayload}"))
            .marshal().jacksonXml()
            .to("{{app.routes.efecte.sendEfecteRequest}}")
            .log("{{app.name}} :: processEfecteRequest :: Successfully ${header.efecteOperation}d the Efecte ${header.efecteEntityType}")
        ;

        from("direct:sendEfecteRequest")
            .routeId("direct:sendEfecteRequest")
            .to("{{app.routes.efecte.setEfecteAuthorization}}")
            .to(https("{{EFECTE_BASE_URL}}")
                .sslContextParameters(ri.getConfigProvider().getSSLContextParameters())
                .x509HostnameVerifier(ri.getConfigProvider().getHostnameVerifier()))
            .choice()
                .when(sendingRequestFailed())
                    .choice()
                        .when(requestContainsErrors())
                            .setProperty("auditMessage", simple("The Efecte request was invalid. Server response: ${body}"))
                        .when(updatingEntityFailed())
                            .setProperty("auditMessage", simple("Updating Efecte ${header.efecteEntityType} failed. Server response: ${body}"))
                        .when(creatingEntityFailed())
                            .setProperty("auditMessage", simple("Creating Efecte ${header.efecteEntityType} failed. Server response: ${body}"))
                    .endChoice()
                    .bean(ri.getAuditExceptionProcessor(),
                                "throwAuditException(" + EnumDirection.ILOQ + ", " + EnumDirection.EFECTE + ", ${header.entityId}, ${header.efecteId}, ${header.iLoqId}, ${header.auditMessage})")
            .end()
            .removeHeaders("*")
        ;

        from("direct:getEfecteEntity")
            .routeId("direct:getEfecteEntity")
            .log("{{app.name}} :: getEfecteEntity :: Getting Efecte ${header.efecteEntityType} with query '${header.efecteQuery}'")
            .setHeaders(
                    Exchange.HTTP_METHOD, constant("GET"),
                    Exchange.HTTP_PATH, simple("/search.ws"),
                    Exchange.HTTP_QUERY, simple("query=${header.efecteQuery}"))
            .to("{{app.routes.efecte.sendEfecteRequest}}")
            .to("{{app.routes.efecte.convertToEfecteEntity}}")
        ;

        from("direct:convertToEfecteEntity")
            .routeId("direct:convertToEfecteEntity")
            .unmarshal(new JacksonXMLDataFormat(EfecteEntitySet.class))
            .setBody(simple("${body.entities}"))
            .choice()
                .when(simple("${body.size} > 0"))
                    .log("{{app.name}} :: convertToEfecteEntity :: Found ${body.size} entities")
                .otherwise()
                    .log("{{app.name}} :: convertToEfecteEntity :: No entities found")
                    .setBody(constant(new ArrayList<>()))
            .end()
        ;
    }

    private Predicate sendingRequestFailed() {
        return PredicateBuilder.or(
            requestContainsErrors(),
            updatingEntityFailed(),
            creatingEntityFailed()
        );
    }

    private Predicate requestContainsErrors() {
        return body().contains("<error>");
    }

    private Predicate updatingEntityFailed() {
        return PredicateBuilder.and(
            exchangeProperty("efecteOperation").isEqualTo("update"),
            body().contains("<entities-updated>0</entities-updated>"));
    }

    private Predicate creatingEntityFailed() {
        return PredicateBuilder.and(
            exchangeProperty("efecteOperation").isEqualTo("create"),
            body().contains("<entities-created>0</entities-created>"));
    }

}
