package com.devikone.test_utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import com.devikone.transports.Redis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestUtils {

    @Inject
    CamelContext camelContext;

    public String readFile(String fileName) throws URISyntaxException, IOException {
        java.net.URI fileUri = this.getClass().getResource(fileName).toURI();
        String result = String.join("\n", Files.readAllLines(
                Paths.get(fileUri), Charset.defaultCharset()));
        return result;
    }

    public void restoreRedis(Redis redis) throws InterruptedException {
        List<String> keys = redis.getAllKeys("*");
        for (String key : keys) {
            redis.del(key);
        }
    }

    public void restoreRedis(Redis redis, String prefix) throws InterruptedException {
        List<String> keys = redis.getAllKeys(prefix);
        for (String key : keys) {
            redis.del(key);
        }
    }

    public Exchange createExchange() {
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = new ExchangeBuilder(camelContext).build();

        return exchange;
    }

    public Exchange createExchange(Object body) {
        CamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = new ExchangeBuilder(camelContext).withBody(body).build();

        return exchange;
    }

    public String createDatetimeNow() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
        String pattern = "yyyy-MM-dd HH:mm:ss";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    public String createDatetimeNow(String pattern) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    public String createDatePlusDays(String date, Integer daysToAdd) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate resultDate = parsedDate.plusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return formattedResult;
    }

    public String createDatePlusDays(String date, Integer daysToAdd, String pattern) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern));
        LocalDate resultDate = parsedDate.plusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern(pattern));

        return formattedResult;
    }

    public String createDateMinusDays(String date, Integer daysToAdd) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate resultDate = parsedDate.minusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return formattedResult;
    }

    public String createDateMinusDays(String date, Integer daysToAdd, String pattern) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern));
        LocalDate resultDate = parsedDate.minusDays(daysToAdd);
        String formattedResult = resultDate.format(DateTimeFormatter.ofPattern(pattern));

        return formattedResult;
    }

    public void addMockEndpointTo(String routeId, String mockEndpoint) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, false,
                builder -> builder
                        .weaveAddLast()
                        .to(mockEndpoint)
                        .id("mockEndpoint"));
    }

    public void addMockEndpointTo(String routeId, String mockEndpoint, Boolean logXML) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, logXML,
                builder -> builder
                        .weaveAddLast()
                        .to(mockEndpoint)
                        .id("mockEndpoint"));
    }

    public void removeMockEndpointFrom(String routeId) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, false,
                builder -> builder
                        .weaveById("mockEndpoint")
                        .remove());
    }

    public void removeMockEndpointFrom(String routeId, Boolean logXML) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, logXML,
                builder -> builder
                        .weaveById("mockEndpoint")
                        .remove());
    }

    public void addMockEndpointsTo(String mockEndpoint, String... routeIds) throws Exception {
        for (String routeId : routeIds) {
            this.addMockEndpointTo(routeId, mockEndpoint, false);
        }
    }

    public void addMockEndpointsTo(String mockEndpoint, Boolean logXML, String... routeIds) throws Exception {
        for (String routeId : routeIds) {
            this.addMockEndpointTo(routeId, mockEndpoint, logXML);
        }
    }

    public void setFakeResponse(String routeId, String httpEndpoint, String fakeResponse) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, false,
                builder -> builder
                        .weaveByToUri(httpEndpoint)
                        .replace()
                        .setBody().simple(fakeResponse)
                        .id("fakeResponse"));
    }

    public void restoreRoute(String routeId, String httpEndpoint) throws Exception {
        AdviceWith.adviceWith(camelContext, routeId, false,
                builder -> builder
                        .weaveById("fakeResponse")
                        .replace()
                        .to(httpEndpoint));
    }

    public void addFakeExceptionHandler() throws Exception {
        RouteConfigurationBuilder fakeExceptionHandler = new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration()
                        .onException(Exception.class)
                        .log("fake exception handler")
                        .log("message: ${exception.message}")
                        .handled(true)
                        .to("mock:exceptionHandlerMock");
            }
        };

        camelContext.addRoutes(fakeExceptionHandler);
    }

    public void addFakeExceptionHandler(String mockEndpoint) throws Exception {
        RouteConfigurationBuilder fakeExceptionHandler = new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration()
                        .onException(Exception.class)
                        .log("fake exception handler")
                        .log("message: ${exception.message}")
                        .handled(true)
                        .to(mockEndpoint);
            }
        };

        camelContext.addRoutes(fakeExceptionHandler);
    }

    public void addFakeExceptionHandler(String mockEndpoint, String routeConfigurationId) throws Exception {
        RouteConfigurationBuilder fakeExceptionHandler = new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration(routeConfigurationId)
                        .onException(Exception.class)
                        .log("fake exception handler, configurationId: " + routeConfigurationId)
                        .log("message: ${exception.message}")
                        .handled(true)
                        .to(mockEndpoint);
            }
        };

        camelContext.addRoutes(fakeExceptionHandler);
    }

    public String minifyJson(String prettyPrintJson) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(prettyPrintJson);
        String minifiedJson = objectMapper.writeValueAsString(jsonNode);

        return minifiedJson;
    }

    public String writeAsJson(Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public String writeAsJson(Object obj, Boolean prettyPrint) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (prettyPrint) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        return objectMapper.writeValueAsString(obj);
    }

    public String writeAsXml(Object obj) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();

        return xmlMapper.writeValueAsString(obj);
    }

    public String writeAsXml(Object obj, boolean prettyPrint) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return xmlMapper.writeValueAsString(obj);
    }

    public <T> T writeAsPojo(String json, Class<T> valueType) throws JsonProcessingException {
        return new ObjectMapper().readValue(json, valueType);
    }

    public void increaseCounter(Exchange ex) {
        Integer counter = ex.getProperty("counter", Integer.class);
        counter++;
        ex.setProperty("counter", counter);
    }
}
