package fi.hel.processors;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.mappers.EfecteKeyMapper;
import fi.hel.mappers.ILoqKeyMapper;
import fi.hel.mappers.ILoqPersonMapper;
import fi.hel.resolvers.EfectePersonResolver;
import fi.hel.resolvers.EfecteKeyResolver;
import fi.hel.resolvers.ILoqPersonResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResourceInjector {

    ///////////
    // BEANS //
    ///////////

    @Inject
    Redis redis;
    @Inject
    CamelContext context;
    @Inject
    ProducerTemplate template;
    @Inject
    ConfigProvider configProvider;
    @Inject
    ILoqPersonProcessor iLoqPersonProcessor;
    @Inject
    ILoqPersonResolver iLoqPersonResolver;
    @Inject
    ILoqKeyMapper iLoqKeyMapper;
    @Inject
    ILoqPersonMapper iLoqPersonMapper;
    @Inject
    EfecteKeyProcessor efecteKeyProcessor;
    @Inject
    EfecteKeyMapper efecteKeyMapper;
    @Inject
    EfecteKeyResolver efecteKeyResolver;
    @Inject
    EfectePersonResolver efectePersonResolver;
    @Inject
    AuditExceptionProcessor auditExceptionProcessor;
    @Inject
    Helper helper;

    ///////////////////////
    // CONFIG PROPERTIES //
    ///////////////////////

    // Redis prefixes:
    @ConfigProperty(name = "app.redis.prefix.maxUpdated")
    String maxUpdatedPrefix;
    @ConfigProperty(name = "app.redis.prefix.iLoqCurrentBaseUrl")
    String iLoqCurrentBaseUrlPrefix;
    @ConfigProperty(name = "app.redis.prefix.iLoqCurrentSessionId")
    String iLoqCurrentSessionIdPrefix;
    @ConfigProperty(name = "app.redis.prefix.iLoqCurrentCustomerCode")
    String iLoqCurrentCustomerCodePrefix;
    @ConfigProperty(name = "app.redis.prefix.iLoqCurrentCustomerCodePassword")
    String iLoqCurrentCustomerCodePasswordPrefix;
    @ConfigProperty(name = "app.redis.prefix.iLoqCurrentCustomerCodeHasChanged")
    String iLoqCurrentCustomerCodeHasChangedPrefix;
    @ConfigProperty(name = "app.redis.prefix.auditMessage")
    String auditMessagePrefix;
    @ConfigProperty(name = "app.redis.prefix.auditException")
    String auditExceptionPrefix;
    @ConfigProperty(name = "app.redis.prefix.auditExceptionInProgress")
    String auditExceptionInProgressPrefix;
    @ConfigProperty(name = "app.redis.prefix.previousKey.efecte")
    String previousKeyEfectePrefix;
    @ConfigProperty(name = "app.redis.prefix.previousKey.iLoq")
    String previousKeyILoqPrefix;
    @ConfigProperty(name = "app.redis.prefix.mapped.key.efecte")
    String mappedKeyEfectePrefix;
    @ConfigProperty(name = "app.redis.prefix.mapped.key.iLoq")
    String mappedKeyILoqPrefix;
    @ConfigProperty(name = "app.redis.prefix.mapped.person.efecte")
    String mappedPersonEfectePrefix;
    @ConfigProperty(name = "app.redis.prefix.mapped.person.iLoq")
    String mappedPersonILoqPrefix;
    @ConfigProperty(name = "app.redis.prefix.temp.efecte.person")
    String tempEfectePersonPrefix;
    @ConfigProperty(name = "app.redis.prefix.temp.deleted.key")
    String tempDeletedKeyPrefix;

    // Routes Redis:
    @ConfigProperty(name = "app.routes.redis.saveILoqBaseUrlToRedis")
    String saveILoqBaseUrlToRedisEndpointUri;
    @ConfigProperty(name = "app.routes.redis.saveILoqSessionIdToRedis")
    String saveILoqSessionIdToRedisEndpointUri;
    @ConfigProperty(name = "app.routes.redis.saveILoqSessionStatusToRedis")
    String saveILoqSessionStatusToRedisEndpointUri;
    @ConfigProperty(name = "app.routes.redis.removeCurrentILoqSessionRelatedKeys")
    String removeCurrentILoqSessionRelatedKeysEndpointUri;

    // Routes Efecte:
    @ConfigProperty(name = "app.routes.efecte.getEfecteEntity")
    String getEfecteEntityEndpointUri;

    // Routes iLOQ:
    @ConfigProperty(name = "app.routes.iLoq.configureILoqSession")
    String configureILoqSessionEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.getILoqUri")
    String getILoqUriEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.createILoqSession")
    String createILoqSessionEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.getILoqLockGroups")
    String getILoqLockGroupsEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.setILoqLockGroup")
    String setILoqLockGroupEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.setILoqHeaders")
    String setILoqHeadersEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.listILoqPersons")
    String listILoqPersonsEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.getILoqKey")
    String getILoqKeyEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.getILoqPerson")
    String getILoqPersonEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.getILoqPersonByExternalId")
    String getILoqPersonByExternalIdEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.processILoqKey")
    String processILoqKeyEndpointUri;
    @ConfigProperty(name = "app.routes.iLoq.createILoqPerson")
    String createILoqPersonEndpointUri;

    // Endpoints:
    @ConfigProperty(name = "app.endpoints.oldhost")
    String oldhostEndpoint;

    ///////////
    // BEANS //
    ///////////

    public Redis getRedis() {
        return this.redis;
    }

    public CamelContext getContext() {
        return this.context;
    }

    public ProducerTemplate getTemplate() {
        return this.template;
    }

    public ConfigProvider getConfigProvider() {
        return this.configProvider;
    }

    public ILoqPersonProcessor getILoqPersonProcessor() {
        return this.iLoqPersonProcessor;
    }

    public ILoqPersonResolver getILoqPersonResolver() {
        return this.iLoqPersonResolver;
    }

    public ILoqKeyMapper getILoqKeyMapper() {
        return this.iLoqKeyMapper;
    }

    public ILoqPersonMapper getILoqPersonMapper() {
        return this.iLoqPersonMapper;
    }

    public Helper getHelper() {
        return this.helper;
    }

    ///////////////////////
    // CONFIG PROPERTIES //
    ///////////////////////

    public String getMaxUpdatedPrefix() {
        return this.maxUpdatedPrefix;
    }

    public String getILoqBaseUrlPrefix() {
        return this.iLoqCurrentBaseUrlPrefix;
    }

    public String getILoqSessionIdPrefix() {
        return this.iLoqCurrentSessionIdPrefix;
    }

    public String getMappedKeyEfectePrefix() {
        return this.mappedKeyEfectePrefix;
    }

    public String getMappedKeyILoqPrefix() {
        return this.mappedKeyILoqPrefix;
    }

    public String getMappedPersonEfectePrefix() {
        return this.mappedPersonEfectePrefix;
    }

    public String getMappedPersonILoqPrefix() {
        return this.mappedPersonILoqPrefix;
    }

    public String getSaveILoqBaseUrlToRedisEndpointUri() {
        return this.saveILoqBaseUrlToRedisEndpointUri;
    }

    public String getSaveILoqSessionIdToRedisEndpointUri() {
        return this.saveILoqSessionIdToRedisEndpointUri;
    }

    public String getRemoveCurrentILoqSessionRelatedKeysEndpointUri() {
        return this.removeCurrentILoqSessionRelatedKeysEndpointUri;
    }

    public String getConfigureILoqSessionEndpointUri() {
        return this.configureILoqSessionEndpointUri;
    }

    public String getGetILoqUriEndpointUri() {
        return this.getILoqUriEndpointUri;
    }

    public String getCreateILoqSessionEndpointUri() {
        return this.createILoqSessionEndpointUri;
    }

    public String getGetILoqLockGroupsEndpointUri() {
        return this.getILoqLockGroupsEndpointUri;
    }

    public String getSetILoqLockGroupEndpointUri() {
        return this.setILoqLockGroupEndpointUri;
    }

    public String getSetILoqHeadersEndpointUri() {
        return this.setILoqHeadersEndpointUri;
    }

    public String getListILoqPersonsEndpointUri() {
        return this.listILoqPersonsEndpointUri;
    }

    public String getCreateILoqPersonEndpointUri() {
        return this.createILoqPersonEndpointUri;
    }

    public String getOldhostEndpoint() {
        return this.oldhostEndpoint;
    }

    public String getILoqCurrentCustomerCodePrefix() {
        return this.iLoqCurrentCustomerCodePrefix;
    }

    public String getILoqCurrentCustomerCodePasswordPrefix() {
        return this.iLoqCurrentCustomerCodePasswordPrefix;
    }

    public String getILoqCurrentBaseUrlPrefix() {
        return this.iLoqCurrentBaseUrlPrefix;
    }

    public String getILoqCurrentSessionIdPrefix() {
        return this.iLoqCurrentSessionIdPrefix;
    }

    public String getILoqCurrentCustomerCodeHasChangedPrefix() {
        return this.iLoqCurrentCustomerCodeHasChangedPrefix;
    }

    public String getSaveILoqSessionStatusToRedisEndpointUri() {
        return this.saveILoqSessionStatusToRedisEndpointUri;
    }

    public EfecteKeyMapper getEfecteKeyMapper() {
        return this.efecteKeyMapper;
    }

    public String getGetILoqPersonEndpointUri() {
        return this.getILoqPersonEndpointUri;
    }

    public String getGetEfecteEntityEndpointUri() {
        return this.getEfecteEntityEndpointUri;
    }

    public EfecteKeyResolver getEfecteKeyResolver() {
        return this.efecteKeyResolver;
    }

    public String getPreviousKeyEfectePrefix() {
        return this.previousKeyEfectePrefix;
    }

    public String getPreviousKeyILoqPrefix() {
        return this.previousKeyILoqPrefix;
    }

    public String getAuditMessagePrefix() {
        return this.auditMessagePrefix;
    }

    public String getAuditExceptionPrefix() {
        return this.auditExceptionPrefix;
    }

    public AuditExceptionProcessor getAuditExceptionProcessor() {
        return this.auditExceptionProcessor;
    }

    public EfectePersonResolver getEfectePersonResolver() {
        return this.efectePersonResolver;
    }

    public String getGetILoqPersonByExternalIdEndpointUri() {
        return this.getILoqPersonByExternalIdEndpointUri;
    }

    public String getTempEfectePersonPrefix() {
        return this.tempEfectePersonPrefix;
    }

    public String getGetILoqKeyEndpointUri() {
        return this.getILoqKeyEndpointUri;
    }

    public String getProcessILoqKeyEndpointUri() {
        return this.processILoqKeyEndpointUri;
    }

    public String getTempDeletedKeyPrefix() {
        return this.tempDeletedKeyPrefix;
    }

    public EfecteKeyProcessor getEfecteKeyProcessor() {
        return this.efecteKeyProcessor;
    }

    public String getAuditExceptionInProgressPrefix() {
        return this.auditExceptionInProgressPrefix;
    }

}
