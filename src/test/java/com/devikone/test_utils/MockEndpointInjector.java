package com.devikone.test_utils;

import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MockEndpointInjector {

    ////////////////////
    // MOCK ENDPOINTS //
    ////////////////////

    @EndpointInject("{{app.routes.controller.efecte.handler}}")
    MockEndpoint efecteKeyCardsHandler;
    @EndpointInject("{{app.routes.controller.efecte.cleanup}}")
    MockEndpoint efecteControllerCleanup;
    @EndpointInject("{{app.routes.controller.iLoq.enrichKeyWithSecurityAccesses}}")
    MockEndpoint enrichKeyWithSecurityAccesses;

    @EndpointInject("{{app.routes.efecte.setEfecteAuthorization}}")
    MockEndpoint setEfecteAuthorization;
    @EndpointInject("{{app.routes.efecte.sendEfecteRequest}}")
    MockEndpoint sendEfecteRequest;
    @EndpointInject("{{app.routes.efecte.getEfecteEntity}}")
    MockEndpoint getEfecteEntity;
    @EndpointInject("{{app.routes.efecte.convertToEfecteEntity}}")
    MockEndpoint convertToEfecteEntity;
    @EndpointInject("{{app.routes.efecte.processEfecteRequest}}")
    MockEndpoint processEfecteRequest;

    @EndpointInject("{{app.routes.redis.getMaxUpdated}}")
    MockEndpoint getMaxUpdated;
    @EndpointInject("{{app.routes.redis.createNewMaxUpdated}}")
    MockEndpoint createNewMaxUpdated;
    @EndpointInject("{{app.routes.redis.setMaxUpdated}}")
    MockEndpoint setMaxUpdated;
    @EndpointInject("{{app.routes.redis.getILoqCredentials}}")
    MockEndpoint getILoqCredentials;
    @EndpointInject("{{app.routes.redis.saveILoqBaseUrlToRedis}}")
    MockEndpoint saveILoqBaseUrlToRedis;
    @EndpointInject("{{app.routes.redis.saveILoqSessionIdToRedis}}")
    MockEndpoint saveILoqSessionIdToRedis;
    @EndpointInject("{{app.routes.redis.saveILoqSessionStatusToRedis}}")
    MockEndpoint saveILoqSessionStatusToRedis;
    @EndpointInject("{{app.routes.redis.removeCurrentILoqSessionRelatedKeys}}")
    MockEndpoint removeCurrentILoqSessionRelatedKeys;
    @EndpointInject("{{app.routes.redis.saveMappedKeys}}")
    MockEndpoint saveMappedKeys;
    @EndpointInject("{{app.routes.redis.savePreviousKeyInfos}}")
    MockEndpoint savePreviousKeyInfos;
    @EndpointInject("{{app.routes.redis.deleteKey}}")
    MockEndpoint deleteKey;
    @EndpointInject("{{app.routes.redis.removeTempKeys}}")
    MockEndpoint removeTempKeys;

    @EndpointInject("{{app.routes.iLoq.configureILoqSession}}")
    MockEndpoint configureILoqSession;
    @EndpointInject("{{app.routes.iLoq.getILoqUri}}")
    MockEndpoint getILoqUri;
    @EndpointInject("{{app.routes.iLoq.createILoqSession}}")
    MockEndpoint createILoqSession;
    @EndpointInject("{{app.routes.iLoq.getILoqLockGroups}}")
    MockEndpoint getILoqLockGroups;
    @EndpointInject("{{app.routes.iLoq.setILoqLockGroup}}")
    MockEndpoint setILoqLockGroup;
    @EndpointInject("{{app.routes.iLoq.setILoqHeaders}}")
    MockEndpoint setILoqHeaders;
    @EndpointInject("{{app.routes.iLoq.killILoqSession}}")
    MockEndpoint killILoqSession;
    @EndpointInject("{{app.routes.iLoq.createILoqPerson}}")
    MockEndpoint createILoqPerson;
    @EndpointInject("{{app.routes.iLoq.processILoqKey}}")
    MockEndpoint processILoqKey;
    @EndpointInject("{{app.routes.iLoq.listILoqKeys}}")
    MockEndpoint listILoqKeys;
    @EndpointInject("{{app.routes.iLoq.getILoqKeySecurityAccesses}}")
    MockEndpoint getILoqKeySecurityAccesses;
    @EndpointInject("{{app.routes.iLoq.listILoqPersons}}")
    MockEndpoint listILoqPersons;
    @EndpointInject("{{app.routes.iLoq.getILoqKey}}")
    MockEndpoint getILoqKey;
    @EndpointInject("{{app.routes.iLoq.getILoqPerson}}")
    MockEndpoint getILoqPerson;
    @EndpointInject("{{app.routes.iLoq.getILoqPersonByExternalId}}")
    MockEndpoint getILoqPersonByExternalId;
    @EndpointInject("{{app.routes.iLoq.updateILoqKeySecurityAccesses}}")
    MockEndpoint updateILoqKeySecurityAccesses;
    @EndpointInject("{{app.routes.iLoq.updateMainZone}}")
    MockEndpoint updateMainZone;

    @EndpointInject("{{app.endpoints.oldhost}}")
    MockEndpoint oldhost;

    public MockEndpoint getSetEfecteAuthorization() {
        return this.setEfecteAuthorization;
    }

    public MockEndpoint getSendEfecteRequest() {
        return this.sendEfecteRequest;
    }

    public MockEndpoint getGetILoqCredentials() {
        return this.getILoqCredentials;
    }

    public MockEndpoint getSaveILoqBaseUrlToRedis() {
        return this.saveILoqBaseUrlToRedis;
    }

    public MockEndpoint getSaveILoqSessionIdToRedis() {
        return this.saveILoqSessionIdToRedis;
    }

    public MockEndpoint getRemoveCurrentILoqSessionRelatedKeys() {
        return this.removeCurrentILoqSessionRelatedKeys;
    }

    public MockEndpoint getConfigureILoqSession() {
        return this.configureILoqSession;
    }

    public MockEndpoint getGetILoqUri() {
        return this.getILoqUri;
    }

    public MockEndpoint getCreateILoqSession() {
        return this.createILoqSession;
    }

    public MockEndpoint getGetILoqLockGroups() {
        return this.getILoqLockGroups;
    }

    public MockEndpoint getSetILoqLockGroup() {
        return this.setILoqLockGroup;
    }

    public MockEndpoint getSetILoqHeaders() {
        return this.setILoqHeaders;
    }

    public MockEndpoint getOldhost() {
        return this.oldhost;
    }

    public MockEndpoint getCreateILoqPerson() {
        return this.createILoqPerson;
    }

    public MockEndpoint getListILoqPersons() {
        return this.listILoqPersons;
    }

    public MockEndpoint getSaveILoqSessionStatusToRedis() {
        return this.saveILoqSessionStatusToRedis;
    }

    public MockEndpoint getListILoqKeys() {
        return this.listILoqKeys;
    }

    public MockEndpoint getEnrichKeyWithSecurityAccesses() {
        return this.enrichKeyWithSecurityAccesses;
    }

    public MockEndpoint getGetILoqKeySecurityAccesses() {
        return this.getILoqKeySecurityAccesses;
    }

    public MockEndpoint getGetILoqPerson() {
        return this.getILoqPerson;
    }

    public MockEndpoint getGetEfecteEntity() {
        return this.getEfecteEntity;
    }

    public MockEndpoint getProcessILoqKey() {
        return this.processILoqKey;
    }

    public MockEndpoint getUpdateILoqKeySecurityAccesses() {
        return this.updateILoqKeySecurityAccesses;
    }

    public MockEndpoint getSavePreviousKeyInfos() {
        return this.savePreviousKeyInfos;
    }

    public MockEndpoint getConvertToEfecteEntity() {
        return this.convertToEfecteEntity;
    }

    public MockEndpoint getProcessEfecteRequest() {
        return this.processEfecteRequest;
    }

    public MockEndpoint getSaveMappedKeys() {
        return this.saveMappedKeys;
    }

    public MockEndpoint getEfecteKeyCardsHandler() {
        return this.efecteKeyCardsHandler;
    }

    public MockEndpoint getKillILoqSession() {
        return this.killILoqSession;
    }

    public MockEndpoint getGetMaxUpdated() {
        return this.getMaxUpdated;
    }

    public MockEndpoint getCreateNewMaxUpdated() {
        return this.createNewMaxUpdated;
    }

    public MockEndpoint getSetMaxUpdated() {
        return this.setMaxUpdated;
    }

    public MockEndpoint getGetILoqPersonByExternalId() {
        return this.getILoqPersonByExternalId;
    }

    public MockEndpoint getGetILoqKey() {
        return this.getILoqKey;
    }

    public MockEndpoint getDeleteKey() {
        return this.deleteKey;
    }

    public MockEndpoint getRemoveTempKeys() {
        return this.removeTempKeys;
    }

    public MockEndpoint getEfecteControllerCleanup() {
        return this.efecteControllerCleanup;
    }

    public MockEndpoint getUpdateMainZone() {
        return this.updateMainZone;
    }

}
