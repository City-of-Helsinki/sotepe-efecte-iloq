package com.devikone.rest;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class RestConfigurationRoute extends RouteBuilder {

    @ConfigProperty(name = "REST_API_HOST", defaultValue = "0.0.0.0")
    String REST_API_HOST = "0.0.0.0";

    @ConfigProperty(name = "REST_API_PORT", defaultValue = "8080")
    String REST_API_PORT = "8080";

    @Override
    public void configure() throws Exception {
        restConfiguration()
                .component("platform-http")
                .host(REST_API_HOST)
                .port(REST_API_PORT)
                .enableCORS(true)
                .corsAllowCredentials(true)
                .corsHeaderProperty("Access-Control-Allow-Origin", "*")
                .corsHeaderProperty("Access-Control-Allow-Headers",
                        "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization, *")
                .inlineRoutes(false);
    }
}
