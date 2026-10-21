package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.models.AuditExceptionRecord;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqPerson;
import fi.hel.models.enumerations.EnumDirection;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class AuditExceptionProcessorILoqPersonTest extends CamelQuarkusTestSupport {

    @Inject
    AuditExceptionProcessor auditExceptionProcessor;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @InjectMock
    Redis redis;

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @BeforeEach
    void setup() {
        mocked.getGetILoqPerson().reset();
    }

    private Exchange createExchange(ILoqPerson person) {
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey("key-1");
        enrichedILoqKey.setRealEstateId("re-1");
        enrichedILoqKey.setPerson(person);
        enrichedILoqKey.setSecurityAccesses(Set.of());

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        return ex;
    }

    private AuditExceptionRecord saveAuditRecord(Exchange ex) throws Exception {
        try {
            auditExceptionProcessor.throwAuditException(
                    EnumDirection.ILOQ, EnumDirection.EFECTE, null, null, "key-1", "message");
        } catch (Exception e) {
            auditExceptionProcessor.setAuditRecord(ex);
        }

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(redis).setex(anyString(), valueCaptor.capture(), anyLong());

        return testUtils.writeAsPojo(valueCaptor.getValue(), AuditExceptionRecord.class);
    }

    @Test
    @DisplayName("setAuditRecord - only the iLOQ person id is known")
    void testShouldReadTheILoqPersonWhenOnlyThePersonIdIsKnown() throws Exception {
        String iLoqPersonId = "c1d24a2e";
        ILoqPerson iLoqPerson = new ILoqPerson("Umid \"Tobi\"", "Usan", iLoqPersonId);
        iLoqPerson.setCompanyName("Kavi");

        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(iLoqPerson));
        mocked.getGetILoqPerson().expectedMessageCount(1);
        mocked.getGetILoqPerson().expectedPropertyReceived("iLoqPersonId", iLoqPersonId);

        AuditExceptionRecord record = saveAuditRecord(createExchange(new ILoqPerson(iLoqPersonId)));

        mocked.getGetILoqPerson().assertIsSatisfied();
        assertThat(record.getILoqKey().getPerson().getPersonId()).isEqualTo(iLoqPersonId);
        assertThat(record.getILoqKey().getPerson().getFirstName()).isEqualTo("Umid \"Tobi\"");
        assertThat(record.getILoqKey().getPerson().getLastName()).isEqualTo("Usan");
        assertThat(record.getILoqKey().getPerson().getCompanyName()).isEqualTo("Kavi");
    }

    @Test
    @DisplayName("setAuditRecord - the iLOQ person details are already known")
    void testShouldNotReadTheILoqPersonWhenThePersonDetailsAreKnown() throws Exception {
        mocked.getGetILoqPerson().expectedMessageCount(0);

        AuditExceptionRecord record = saveAuditRecord(createExchange(new ILoqPerson("John", "Doe", "p1")));

        mocked.getGetILoqPerson().assertIsSatisfied();
        assertThat(record.getILoqKey().getPerson().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("setAuditRecord - reading the iLOQ person fails")
    void testShouldSaveTheAuditRecordEvenIfReadingTheILoqPersonFails() throws Exception {
        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> {
            throw new Exception("iLOQ is not available");
        });

        AuditExceptionRecord record = saveAuditRecord(createExchange(new ILoqPerson("p1")));

        assertThat(record.getILoqKey().getPerson().getPersonId()).isEqualTo("p1");
        assertThat(record.getILoqKey().getPerson().getFirstName()).isNull();
    }
}
