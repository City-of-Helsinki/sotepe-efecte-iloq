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
                    .log("{{app.name}} :: HTTP ERROR OCCURRED :: ${exception.message}")
                    .log("{{app.name}} :: Http-response: ${exchangeProperty[CamelExceptionCaught].getResponseBody()}")
                .otherwise()
                    .log("{{app.name}} :: ERROR OCCURRED :: ${exception.message}")
            .end()
            .stop()
        ;

        routeConfiguration()
            .precondition(this.useExceptionHandling ? "true" : "{{app.configuration.useExceptionHandling}}")
            .onException(AuditException.class)
            .handled(true)
            .log("{{app.name}} :: AUDIT EXCEPTION OCCURRED :: ${exception.message}")
            .bean("redis", "del({{app.redis.prefix.auditExceptionInProgress}})")
            .stop()
        ;
    }

}