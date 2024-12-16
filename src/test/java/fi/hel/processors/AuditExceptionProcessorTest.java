package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.exceptions.AuditException;
import fi.hel.models.AuditExceptionRecord;
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

    @ConfigProperty(name = "AUDIT_EXCEPTION_EXPIRATION_TIME")
    long auditExceptionExpirationTime;

    @Test
    @DisplayName("setAuditException")
    void testShouldSaveAndReturnTheCreatedAuditExceptionRecordJson() throws Exception {
        EnumDirection from = EnumDirection.ILOQ;
        EnumDirection to = EnumDirection.EFECTE;
        String entityId = "12345";
        String efecteId = "KEY-000123";
        String iLoqId = "abc-123";
        String message = "foobar";

        String timestamp = testUtils.createDatetimeNow("yyyy-MM-dd'T'HH:mm:ss");
        String expectedPrefix = ri.getAuditExceptionPrefix() + timestamp + ":" + efecteId;

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> expirationTimeCaptor = ArgumentCaptor.forClass(Long.class);

        verifyNoInteractions(redis);

        String result = auditExceptionProcessor.setAuditException(from, to, entityId, efecteId, iLoqId, message);

        verify(redis).setex(keyCaptor.capture(), valueCaptor.capture(), expirationTimeCaptor.capture());

        String key = keyCaptor.getValue();
        String value = valueCaptor.getValue();
        long expirationTime = expirationTimeCaptor.getValue();
        AuditExceptionRecord auditExceptionRecord = testUtils.writeAsPojo(value, AuditExceptionRecord.class);

        assertThat(key).isEqualTo(expectedPrefix);
        assertThat(expirationTime).isEqualTo(auditExceptionExpirationTime);
        assertThat(auditExceptionRecord.getTimestamp()).isEqualTo(timestamp);
        assertThat(auditExceptionRecord.getFrom()).isEqualTo(from);
        assertThat(auditExceptionRecord.getTo()).isEqualTo(to);
        assertThat(auditExceptionRecord.getEntityId()).isEqualTo(entityId);
        assertThat(auditExceptionRecord.getEfecteId()).isEqualTo(efecteId);
        assertThat(auditExceptionRecord.getILoqId()).isEqualTo(iLoqId);
        assertThat(auditExceptionRecord.getMessage()).isEqualTo(message);

        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("setAuditException")
    void testShoulSetTheAuditExceptionInProcessKeyWhenProcessingAnAuditException() throws Exception {
        EnumDirection from = EnumDirection.ILOQ;
        EnumDirection to = EnumDirection.EFECTE;
        String entityId = "12345";
        String efecteId = "KEY-000123";
        String iLoqId = "abc-123";
        String message = "foobar";

        String expectedPrefix = ri.getAuditExceptionInProgressPrefix();

        verifyNoInteractions(redis);

        auditExceptionProcessor.setAuditException(from, to, entityId, efecteId, iLoqId, message);

        verify(redis).set(expectedPrefix, "true");
    }

    @Test
    @DisplayName("setAuditException")
    void testShouldNotSaveMoreThanOneAuditExceptionForTheSameErrorScenario() throws Exception {
        EnumDirection from = EnumDirection.ILOQ;
        EnumDirection to = EnumDirection.EFECTE;
        String entityId = "12345";
        String efecteId = "KEY-000123";
        String iLoqId = "abc-123";
        String message = "foobar";

        when(redis.exists(ri.getAuditExceptionInProgressPrefix())).thenReturn(true);

        verifyNoInteractions(redis);

        auditExceptionProcessor.setAuditException(from, to, entityId, efecteId, iLoqId, message);

        verify(redis, times(0)).setex(any(), any(), any());
    }

    @Test
    @DisplayName("throwAuditException")
    void testShouldSaveTheCreatedAuditExceptionRecordJsonToRedisAndThrowAnAuditException() throws Exception {
        EnumDirection from = EnumDirection.ILOQ;
        EnumDirection to = EnumDirection.EFECTE;
        String entityId = "12345";
        String efecteId = "KEY-000123";
        String iLoqId = "abc-123";
        String message = "foobar";

        String expectedExceptionMessage = "audit exception record json";

        when(auditExceptionProcessor.setAuditException(from, to, entityId, efecteId, iLoqId, message))
                .thenReturn(expectedExceptionMessage);

        try {
            auditExceptionProcessor.throwAuditException(from, to, entityId, efecteId, iLoqId, message);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(AuditException.class);
            assertThat(e.getMessage()).isEqualTo(expectedExceptionMessage);
        }
    }

}
