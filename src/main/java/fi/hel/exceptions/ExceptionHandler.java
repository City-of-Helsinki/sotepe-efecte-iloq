package fi.hel.exceptions;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExceptionHandler extends RouteConfigurationBuilder {

    private Boolean useExceptionHandling = false;

    public ExceptionHandler() {
    }

    public ExceptionHandler(Boolean useExceptionHandling) {
        this.useExceptionHandling = useExceptionHandling;
    }

    @Override
    public void configuration() throws Exception {
        routeConfiguration()
            .precondition(this.useExceptionHandling ? "true" : "{{app.configuration.useExceptionHandling}}")
            .onException(HttpOperationFailedException.class)
                .onWhen(hasInvalidILoqSessionId())
                    .continued(true)
                    .bean("redis", "set('{{app.redis.prefix.iLoqCurrentCustomerCodeHasChanged}}', 'true')")
                    .bean("configProvider", "getConfiguredCustomerCodes")
                    .to("{{app.routes.exceptionHandler.saveHeadersAndBody}}")
                    .log("{{app.name}} :: ExceptionHandler :: iLOQ session failed, refreshing session")
                    .to("{{app.routes.iLoq.configureILoqSession}}")
                    .to("{{app.routes.exceptionHandler.restoreHeadersAndBody}}")
                    .log("{{app.name}} :: ExceptionHandler :: Session refreshed, continue processing")
                    .to("{{app.endpoints.oldhost}}")
        ;


        routeConfiguration()
            .precondition(this.useExceptionHandling ? "true" : "{{app.configuration.useExceptionHandling}}")
            .onException(Exception.class)
                .maximumRedeliveries("{{MAXIMUM_REDELIVERIES}}")
                .maximumRedeliveryDelay("{{MAXIMUM_REDELIVERY_DELAY}}")
                .logRetryAttempted(false)
                .handled(true)
                .choice()
                    .when(exchangeProperty("CamelExceptionCaught").isInstanceOf(HttpOperationFailedException.class))
                        .to("sentry-log:{{app.name}} :: HTTP ERROR OCCURRED :: ${exception.message}, response: ${exchangeProperty[CamelExceptionCaught].getResponseBody()}?loggingLevel=ERROR")
                    .otherwise()
                        .to("sentry-log:{{app.name}} :: ERROR OCCURRED :: ${exception.message}?loggingLevel=ERROR")
                .end()
                .stop()
        ;

        routeConfiguration()
            .precondition(this.useExceptionHandling ? "true" : "{{app.configuration.useExceptionHandling}}")
            .onException(AuditException.class)
                .handled(true)
                .to("sentry-log:{{app.name}} :: AUDIT EXCEPTION OCCURRED :: ${exception.message}?loggingLevel=ERROR")
                // AuditRecords are key value pairs stored for the UI
                .bean("auditExceptionProcessor", "setAuditRecord")
                .stop()
        ;


        from("direct:saveHeadersAndBody")
            .routeId("direct:saveHeadersAndBody")
            .setProperty("cachedMethod", simple("${header.CamelHttpMethod}"))
            .setProperty("cachedUri", simple("${header.CamelHttpUri}"))
            .setProperty("cachedContentType", simple("${header.Content-Type}"))
            .setProperty("cachedHttpQuery", simple("${header.CamelHttpQuery}"))
            .setProperty("cachedHttpPath", simple("${header.CamelHttpPath}"))
            .setProperty("cachedBody", simple("${body}"))
        ;

        from("direct:restoreHeadersAndBody")
            .routeId("direct:restoreHeadersAndBody")
            .setHeader(Exchange.HTTP_METHOD, simple("${header.cachedMethod}"))
            .setHeader(Exchange.HTTP_URI, simple("${header.cachedUri}"))
            .setHeader(Exchange.CONTENT_TYPE, simple("${header.cachedContentType}"))
            .setHeader(Exchange.HTTP_QUERY, simple("${header.cachedHttpQuery}"))
            .setHeader(Exchange.HTTP_PATH, simple("${header.cachedHttpPath}"))
            .setBody(simple("${header.cachedBody}"))
        ;
    }

    private Predicate hasInvalidILoqSessionId() {
        return simple("${exchangeProperty[CamelExceptionCaught].getResponseBody()}")
                .contains("Invalid session id");
    }

}