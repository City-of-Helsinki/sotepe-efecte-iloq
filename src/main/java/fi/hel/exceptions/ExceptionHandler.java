package fi.hel.exceptions;

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
            .bean("redis", "del({{app.redis.prefix.auditExceptionInProgress}})")
            .stop()
        ;
    }

}