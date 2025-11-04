package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.exceptions.AuditException;
import fi.hel.models.AuditExceptionRecord;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqPerson;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.enumerations.EnumDirection;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;

@QuarkusTest
public class AuditExceptionProcessorTest {

    @Inject
    ResourceInjector ri;
    @InjectSpy
    AuditExceptionProcessor auditExceptionProcessor;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;

    @ConfigProperty(name = "AUDIT_EXCEPTION_EXPIRATION_SECONDS")
    long auditExceptionExpirationTime;

    @Test
    @DisplayName("setAuditRecord")
    void testShouldSaveAnAuditRecord() throws Exception {
        EnumDirection from = EnumDirection.ILOQ;
        EnumDirection to = EnumDirection.EFECTE;
        String entityId = "12345";
        String efecteId = "KEY-000123";
        String iLoqId = "abc-123";
        String message = "foobar";
        String realEstateId = "xyz-456";
        String realEstateName = "Malmin perhekeskus";
        String iLoqPersonFirstName = "Matti";
        String iLoqPersonLastName = "Meikäläinen";
        String iLoqPersonId = "eta-136";
        String iLoqSecurityAccessName1 = "kulkualue 1";
        String iLoqSecurityAccessName2 = "kulkualue 2";
        String iLoqSecurityAccessId1 = "iii-987";
        String iLoqSecurityAccessId2 = "eee-789";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqId);
        enrichedILoqKey.setRealEstateId(realEstateId);
        enrichedILoqKey.setRealEstateName(realEstateName);
        ILoqPerson iLoqPerson = new ILoqPerson(iLoqPersonFirstName, iLoqPersonLastName, iLoqPersonId);
        enrichedILoqKey.setPerson(iLoqPerson);
        ILoqSecurityAccess iLoqSecurityAccess1 = new ILoqSecurityAccess(iLoqSecurityAccessName1, realEstateId,
                iLoqSecurityAccessId1);
        ILoqSecurityAccess iLoqSecurityAccess2 = new ILoqSecurityAccess(iLoqSecurityAccessName2, realEstateId,
                iLoqSecurityAccessId2);
        enrichedILoqKey.setSecurityAccesses(Set.of(iLoqSecurityAccess1, iLoqSecurityAccess2));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        String expectedPrefix = ri.getAuditRecordKeyPrefix() + realEstateId + ":" + iLoqId;

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verifyNoInteractions(redis);

        try {
            auditExceptionProcessor.throwAuditException(from, to, entityId, efecteId, iLoqId, message);
        } catch (Exception e) {
            auditExceptionProcessor.setAuditRecord(ex);
        }

        verify(redis).set(keyCaptor.capture(), valueCaptor.capture());

        String key = keyCaptor.getValue();
        String value = valueCaptor.getValue();
        AuditExceptionRecord auditExceptionRecord = testUtils.writeAsPojo(value, AuditExceptionRecord.class);

        assertThat(key).isEqualTo(expectedPrefix);
        assertThat(auditExceptionRecord.getFrom()).isEqualTo(from);
        assertThat(auditExceptionRecord.getTo()).isEqualTo(to);
        assertThat(auditExceptionRecord.getEntityId()).isEqualTo(entityId);
        assertThat(auditExceptionRecord.getEfecteId()).isEqualTo(efecteId);
        assertThat(auditExceptionRecord.getILoqId()).isEqualTo(iLoqId);
        assertThat(auditExceptionRecord.getMessage()).isEqualTo(message);
        assertThat(auditExceptionRecord.getILoqKey().getRealEstateId()).isEqualTo(realEstateId);
        assertThat(auditExceptionRecord.getILoqKey().getRealEstateName()).isEqualTo(realEstateName);
        assertThat(auditExceptionRecord.getILoqKey().getPerson().getFirstName()).isEqualTo(iLoqPersonFirstName);
        assertThat(auditExceptionRecord.getILoqKey().getPerson().getLastName()).isEqualTo(iLoqPersonLastName);
        assertThat(auditExceptionRecord.getILoqKey().getPerson().getPersonId()).isEqualTo(iLoqPersonId);

        Set<ILoqSecurityAccess> resulSecurityAccesses = auditExceptionRecord.getILoqKey().getSecurityAccesses();

        assertThat(resulSecurityAccesses).contains(iLoqSecurityAccess1, iLoqSecurityAccess2);
    }

    // @Test
    // @DisplayName("setAuditException")
    // void testShouldUseILoqIdInTheKeyWhenEfecteIdIsNotAvailable() throws Exception {
    //     EnumDirection from = EnumDirection.ILOQ;
    //     EnumDirection to = EnumDirection.EFECTE;
    //     String entityId = "12345";
    //     String efecteId = null;
    //     String iLoqId = "abc-123";
    //     String message = "foobar";

    //     String timestamp = testUtils.createDatetimeNow("yyyy-MM-dd'T'HH:mm:ss");
    //     String expectedPrefix = ri.getAuditExceptionPrefix() + timestamp + ":" + iLoqId;

    //     ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    //     ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    //     ArgumentCaptor<Long> expirationTimeCaptor = ArgumentCaptor.forClass(Long.class);

    //     verifyNoInteractions(redis);

    //     auditExceptionProcessor.setAuditRecord(from, to, entityId, efecteId, iLoqId, message);

    //     verify(redis).setex(keyCaptor.capture(), valueCaptor.capture(), expirationTimeCaptor.capture());

    //     String key = keyCaptor.getValue();

    //     assertThat(key).isEqualTo(expectedPrefix);
    // }

    // @Test
    // @DisplayName("setAuditException")
    // void testShoulSetTheAuditExceptionInProcessKeyWhenProcessingAnAuditException() throws Exception {
    //     EnumDirection from = EnumDirection.ILOQ;
    //     EnumDirection to = EnumDirection.EFECTE;
    //     String entityId = "12345";
    //     String efecteId = "KEY-000123";
    //     String iLoqId = "abc-123";
    //     String message = "foobar";

    //     String expectedPrefix = ri.getAuditExceptionInProgressPrefix();

    //     verifyNoInteractions(redis);

    //     auditExceptionProcessor.setAuditRecord(from, to, entityId, efecteId, iLoqId, message);

    //     verify(redis).set(expectedPrefix, "true");
    // }

    // @Test
    // @DisplayName("setAuditException")
    // void testShouldNotSaveMoreThanOneAuditExceptionForTheSameErrorScenario() throws Exception {
    //     EnumDirection from = EnumDirection.ILOQ;
    //     EnumDirection to = EnumDirection.EFECTE;
    //     String entityId = "12345";
    //     String efecteId = "KEY-000123";
    //     String iLoqId = "abc-123";
    //     String message = "foobar";

    //     when(redis.exists(ri.getAuditExceptionInProgressPrefix())).thenReturn(true);

    //     verifyNoInteractions(redis);

    //     auditExceptionProcessor.setAuditRecord(from, to, entityId, efecteId, iLoqId, message);

    //     verify(redis, times(0)).setex(any(), any(), any());
    // }

    // @Test
    // @DisplayName("throwAuditException")
    // void testShouldSaveTheCreatedAuditExceptionRecordJsonToRedisAndThrowAnAuditException() throws Exception {
    //     EnumDirection from = EnumDirection.ILOQ;
    //     EnumDirection to = EnumDirection.EFECTE;
    //     String entityId = "12345";
    //     String efecteId = "KEY-000123";
    //     String iLoqId = "abc-123";
    //     String message = "foobar";

    //     String expectedExceptionMessage = "audit exception record json";

    //     when(auditExceptionProcessor.setAuditRecord(from, to, entityId, efecteId, iLoqId, message))
    //             .thenReturn(expectedExceptionMessage);

    //     try {
    //         auditExceptionProcessor.throwAuditException(from, to, entityId, efecteId, iLoqId, message);
    //     } catch (Exception e) {
    //         assertThat(e).isInstanceOf(AuditException.class);
    //         assertThat(e.getMessage()).isEqualTo(expectedExceptionMessage);
    //     }
    // }

}
