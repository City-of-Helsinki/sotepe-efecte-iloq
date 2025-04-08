package fi.hel.routes;

import java.util.Set;

import org.apache.camel.builder.RouteBuilder;

import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@SuppressWarnings("unchecked")
public class RedisRouteBuilder extends RouteBuilder {

    @Inject
    ResourceInjector ri;

    @Override
    public void configure() throws Exception {

        from("direct:getMaxUpdated")
            .routeId("direct:getMaxUpdated")
            .bean(ri.getRedis(), "get({{app.redis.prefix.maxUpdated}})")
            .setProperty("maxUpdated").simple("${body}")
            .choice()
                .when(exchangeProperty("maxUpdated").isNull())
                    .setProperty("maxUpdated", constant("{{EFECTE_INITIAL_MAX_UPDATED}}"))
            .end()
        ;

        from("direct:createNewMaxUpdated")
            .routeId("direct:createNewMaxUpdated")
            .setProperty("newMaxUpdated")
                .simple("${date-with-timezone:now:Europe/Helsinki:dd.MM.yyyy HH:mm}")
            .log("{{app.name}} :: createNewMaxUpdated :: created new maxUpdated timestamp '${header.newMaxUpdated}'")
        ;

        from("direct:setMaxUpdated")
            .routeId("direct:setMaxUpdated")
            .log("{{app.name}} :: setMaxUpdated :: setting new maxUpdated '${header.newMaxUpdated}' for the next polling cycle")
            .bean(ri.getRedis(), "set('{{app.redis.prefix.maxUpdated}}', '${header.newMaxUpdated}')")
        ;

        from("direct:getILoqCredentials")
            .routeId("direct:getILoqCredentials")
            .setProperty("customerCode", method(ri.getRedis(), "get({{app.redis.prefix.iLoqCurrentCustomerCode}})"))
            .setProperty("customerCodePassword", method(ri.getRedis(), "get({{app.redis.prefix.iLoqCurrentCustomerCodePassword}})"))
        ;

        from("direct:saveILoqBaseUrlToRedis")
            .routeId("direct:saveILoqBaseUrlToRedis")
            .log("{{app.name}} :: saveILoqBaseUrlToRedis :: Configured base url: ${body}")
            .bean(ri.getRedis(), "set('{{app.redis.prefix.iLoqCurrentBaseUrl}}', '${body}')")
        ;

        from("direct:saveILoqSessionIdToRedis")
            .routeId("direct:saveILoqSessionIdToRedis")
            .log("{{app.name}} :: saveILoqSessionIdToRedis :: Saved new session id to Redis")
            .bean(ri.getRedis(), "set('{{app.redis.prefix.iLoqCurrentSessionId}}', '${body}')")
        ;

        from("direct:saveILoqSessionStatusToRedis")
            .routeId("direct:saveILoqSessionStatusToRedis")
            .bean(ri.getRedis(), "set('{{app.redis.prefix.iLoqCurrentCustomerCodeHasChanged}}', 'false')")
        ;

        from("direct:removeCurrentILoqSessionRelatedKeys")
            .routeId("direct:removeCurrentILoqSessionRelatedKeys")
            .bean(ri.getRedis(), "del({{app.redis.prefix.iLoqCurrentBaseUrl}})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.iLoqCurrentSessionId}})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.iLoqCurrentCustomerCode}})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.iLoqCurrentCustomerCodePassword}})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.iLoqCurrentCustomerCodeHasChanged}})")
        ;

        from("direct:saveMappedKeys")
            .routeId("direct:saveMappedKeys")
            .log("{{app.name}} :: saveMappedKeys :: Saving mapped key ids for Efecte and iLOQ")
            .process(exchange -> {
                String entityId = exchange.getProperty("efecteKeyEntityId", String.class);
                String efecteId = exchange.getProperty("efecteKeyEfecteId", String.class);
                String iLoqId = exchange.getProperty("iLoqKeyId", String.class);

                EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(entityId, efecteId);

                ri.getRedis().set(ri.getMappedKeyEfectePrefix() + efecteId, iLoqId);
                ri.getRedis().set(ri.getMappedKeyILoqPrefix() + iLoqId, ri.getHelper().writeAsJson(efecteEntityIdentifier));
            })
        ;

        from("direct:savePreviousKeyInfos")
            .routeId("direct:savePreviousKeyInfos")
            .log("{{app.name}} :: savePreviousKeyInfos :: Saving new 'previous key infos' for Efecte and iLOQ keys")
            .process(exchange -> {
                String efecteId = exchange.getProperty("efecteKeyEfecteId", String.class);
                String iLoqId = exchange.getProperty("iLoqKeyId", String.class);
                PreviousEfecteKey newPreviousEfecteKey = exchange.getProperty("newPreviousEfecteKey", PreviousEfecteKey.class);
                Set<String> newILoqSecurityAccessIds = exchange.getProperty("newILoqSecurityAccessIds", Set.class);

                ri.getRedis().set(ri.getPreviousKeyEfectePrefix() + efecteId, ri.getHelper().writeAsJson(newPreviousEfecteKey));
                
                if (newILoqSecurityAccessIds != null && !newILoqSecurityAccessIds.isEmpty()) {
                    // Deleting the previous iLOQ key since the value is a Set (full update instead of patching)
                    ri.getRedis().del(ri.getPreviousKeyILoqPrefix() + iLoqId);
                    ri.getRedis().addSet(ri.getPreviousKeyILoqPrefix() + iLoqId, newILoqSecurityAccessIds.toArray(new String[0]));
                }
            })
        ;

        from("direct:deleteKey")
            .routeId("direct:deleteKey")
            .log("{{app.name}} :: deleteKey :: Removing all Redis keys related to the previously disabled iLOQ key")
            .bean(ri.getRedis(), "del({{app.redis.prefix.previousKey.efecte}}${header.efecteKeyEfecteId})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.previousKey.iLoq}}${header.iLoqKeyId})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.mapped.key.efecte}}${header.efecteKeyEfecteId})")
            .bean(ri.getRedis(), "del({{app.redis.prefix.mapped.key.iLoq}}${header.iLoqKeyId})")

            .bean(ri.getRedis(), "setex('{{app.redis.prefix.temp.deleted.key}}${date-with-timezone:now:Europe/Helsinki:yyyy-MM-dd_HH:mm}:${header.efecteKeyEfecteId}', '${header.iLoqKeyId}', {{DELETED_KEY_EXPIRATION_SECONDS}})")
        ;

        from("direct:removeTempKeys")
            .routeId("direct:removeTempKeys")
            .bean(ri.getRedis(), "getAllKeys({{app.redis.prefix.temp.efecte.person}}*)")
            .split(body())
                .bean(ri.getRedis(), "del(${body})")
            .end()
        ;
    }
}
