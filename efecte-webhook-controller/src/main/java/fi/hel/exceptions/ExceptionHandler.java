package fi.hel.exceptions;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteConfigurationBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExceptionHandler extends RouteConfigurationBuilder {

    @Override
    public void configuration() throws Exception {
        routeConfiguration()
            .onException(Exception.class)
            .log("{{app.name}} :: FORWARDING FAILED :: The original event body has been saved to the key '${header.redisKey}'")
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setBody(constant("OK"))
            .stop()
        ;
    }
}