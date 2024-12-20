package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.models.CustomerConfiguration;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("unchecked")
public class ConfigProviderTest {

    @Inject
    ResourceInjector ri;
    @Inject
    ConfigProvider configProvider;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;

    private String testCustomerCode1 = "customer-code-1";
    private String testCustomerPassword1 = "customer-password-1";
    private String testCustomerCode2 = "customer-code-2";
    private String testEfecteAddressName1 = "Testikatu 1, 00510, Helsinki";
    private String testEfecteAddressEntityId1 = "testikatu-1-entity-id";
    private String testEfecteAddressEfecteId = "testikatu-1-efecte-id";
    private String testEfecteAddressEntityId2 = "testikatu-2-entity-id";
    private String testILoqRealEstateName = "Testikatu 1";
    private String testILoqRealEstateId = "testikatu-1-id";
    private String testILoqZoneName = "Testikadun vyöhyke";
    private String testILoqZoneId = "testivyohyke_1";
    private String testEfecteSecurityAccessName1 = "Ulko-ovi";
    private String testEfecteSecurityAccessEntityId1 = "ulko-ovi-entity-id";
    private String testEfecteSecurityAccessEfecteId1 = "ulko-ovi-efecte-id";
    private String testILoqSecurityAccessName1 = "Firman etuovi";
    private String testILoqSecurityAccessId1 = "firman-etuovi-id";
    private String testEfecteSecurityAccessName2 = "Toimisto";
    private String testEfecteSecurityAccessEntityId2 = "toimisto-entity-id";
    private String testEfecteSecurityAccessEfecteId2 = "toimisto-efecte-id";
    private String testEfecteSecurityAccessEntityId3 = "varaston-ovi-entity-id";
    private String testILoqSecurityAccessName2 = "Työntekijöiden toimisto";
    private String testILoqSecurityAccessId2 = "tyontekijoiden-toimisto-id";

    @BeforeEach
    void setup() throws Exception {
        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn(testCustomerCode1);
    }

    @Test
    @DisplayName("initCustomerConfigurations")
    void testShouldInitTheCustomerConfigurationsFromAnENV() throws Exception {
        Object result = configProvider.getCustomerConfigurations();

        assertThat(result).isInstanceOf(List.class);

        List<Object> list = (List<Object>) result;

        for (Object item : list) {
            assertThat(item).isInstanceOfAny(CustomerConfiguration.class);
        }

        CustomerConfiguration customerConfiguration = (CustomerConfiguration) list.get(0);

        assertThat(customerConfiguration.getCustomerCode()).isEqualTo(testCustomerCode1);
        assertThat(customerConfiguration.getCustomerCodePassword()).isEqualTo(testCustomerPassword1);

        assertThat(customerConfiguration.getRealEstates().get(0)
                .getEfecte().getName()).isEqualTo(testEfecteAddressName1);
        assertThat(customerConfiguration.getRealEstates().get(0)
                .getEfecte().getEntityId()).isEqualTo(testEfecteAddressEntityId1);
        assertThat(customerConfiguration.getRealEstates().get(0)
                .getEfecte().getEfecteId()).isEqualTo(testEfecteAddressEfecteId);
        assertThat(customerConfiguration.getRealEstates().get(0)
                .getILoq().getName()).isEqualTo(testILoqRealEstateName);
        assertThat(customerConfiguration.getRealEstates().get(0)
                .getILoq().getId()).isEqualTo(testILoqRealEstateId);

        assertThat(customerConfiguration.getZones().get(0).getName()).isEqualTo(testILoqZoneName);
        assertThat(customerConfiguration.getZones().get(0).getId()).isEqualTo(testILoqZoneId);

        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(0).getEfecte().getName()).isEqualTo(testEfecteSecurityAccessName1);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(0).getEfecte().getEntityId()).isEqualTo(testEfecteSecurityAccessEntityId1);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(0).getEfecte().getEfecteId()).isEqualTo(testEfecteSecurityAccessEfecteId1);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(0).getILoq().getName()).isEqualTo(testILoqSecurityAccessName1);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(0).getILoq().getId()).isEqualTo(testILoqSecurityAccessId1);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(1).getEfecte().getName()).isEqualTo(testEfecteSecurityAccessName2);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(1).getEfecte().getEntityId()).isEqualTo(testEfecteSecurityAccessEntityId2);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(1).getEfecte().getEfecteId()).isEqualTo(testEfecteSecurityAccessEfecteId2);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(1).getILoq().getName()).isEqualTo(testILoqSecurityAccessName2);
        assertThat(customerConfiguration.getZones().get(0).getSecurityAccesses()
                .get(1).getILoq().getId()).isEqualTo(testILoqSecurityAccessId2);
    }

    @Test
    @DisplayName("getConfiguredCustomerCodes")
    void testShouldReturnAListOfConfiguredCustomerCodes() throws Exception {
        List<String> result = configProvider.getConfiguredCustomerCodes();

        assertThat(result).containsExactlyInAnyOrder(testCustomerCode1, testCustomerCode2);
    }

    @Test
    @DisplayName("getConfiguredILoqRealEstateIds")
    void testShouldReturnAListOfConfiguredRealEstateIdsForACustomerCode() throws Exception {

        List<String> result = configProvider.getConfiguredILoqRealEstateIds();

        assertThat(result).containsExactly(testILoqRealEstateId);
    }

    @Test
    @DisplayName("isValidEfecteAddress")
    void testShouldReturnTrueForValidEfecteAddresses() throws Exception {
        boolean result1 = configProvider.isValidEfecteAddress(testEfecteAddressEntityId1);
        boolean result2 = configProvider.isValidEfecteAddress(testEfecteAddressEntityId2);

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    @DisplayName("isValidEfecteAddress")
    void testShouldReturnFalseForInvalidEfecteAddress() throws Exception {
        boolean result = configProvider.isValidEfecteAddress("invalid Efecte address");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getILoqCustomerCodeByEfecteAddress")
    void testShouldReturnTheCorrespondingILoqCustomerCodeForAValidEfecteAddress() throws Exception {
        String customerCode = configProvider.getILoqCustomerCodeByEfecteAddress(testEfecteAddressEntityId1);

        assertThat(customerCode).isEqualTo(testCustomerCode1);
    }

    @Test
    @DisplayName("getILoqCustomerCodeByEfecteAddress")
    void testShouldThrowAnExceptionWhenCustomerCodeIsNotFoundForTheEfecteAddress() throws Exception {
        String invalidEfecteAddress = "invalid efecte address";
        String expectedExceptionMessage = "ConfigProvider: No iLOQ customer code configured for Efecte address '"
                + invalidEfecteAddress + "'";

        assertThatThrownBy(() -> configProvider.getILoqCustomerCodeByEfecteAddress(invalidEfecteAddress))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("saveCurrentCredentialsToRedis")
    void testShouldSaveTheCurrentCredentialsToRedis() throws Exception {

        verifyNoInteractions(redis);

        configProvider.saveCurrentCredentialsToRedis(testCustomerCode1);

        verify(redis).set(ri.getILoqCurrentCustomerCodePrefix(), testCustomerCode1);
        verify(redis).set(ri.getILoqCurrentCustomerCodePasswordPrefix(), testCustomerPassword1);
    }

    @Test
    @DisplayName("saveCurrentCredentialsToRedis")
    void testShouldThrowAnExceptionWhenACustomerConfigurationIsNotFoundForTheCustomerCode() throws Exception {
        String invalidCustomerCode = "invalid_customer_code";
        String expectedExceptionMessage = "ConfigProvider: No iLOQ customer configuration found for customer code '"
                + invalidCustomerCode + "'";

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn(invalidCustomerCode);

        assertThatThrownBy(() -> configProvider.saveCurrentCredentialsToRedis(
                invalidCustomerCode))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getILoqRealEstateIdByEfecteAddress")
    void testShouldReturnTheRealEstateId() throws Exception {

        String result = configProvider.getILoqRealEstateIdByEfecteAddressEntityId(testEfecteAddressEntityId1);

        assertThat(result).isEqualTo(testILoqRealEstateId);
    }

    @Test
    @DisplayName("getILoqRealEstateIdByEfecteAddress")
    void testShouldThrowAnExceptionWhenProvidedRealEstateIsNotConfigured_GetILoqRealEstateIdByEfecteAddress()
            throws Exception {
        String invalidEfecteAddressEntityId = "invalid Efecte address entity id";
        String expectedExceptionMessage = "ConfigProvider: Could not find an iLOQ real estate matching the Efecte address entity id '"
                + invalidEfecteAddressEntityId + "'";

        assertThatThrownBy(
                () -> configProvider.getILoqRealEstateIdByEfecteAddressEntityId(
                        invalidEfecteAddressEntityId))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getILoqRealEstateIdByEfecteAddress")
    void testShouldThrowAnExceptionWhenProvidedCustomerCodeIsNotConfigured_GetILoqRealEstateIdByEfecteAddress()
            throws Exception {
        String invalidCustomerCode = "invalid_customer_code";
        String expectedExceptionMessage = "ConfigProvider: No iLOQ customer configuration found for customer code '"
                + invalidCustomerCode + "'";

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn(invalidCustomerCode);

        assertThatThrownBy(() -> configProvider
                .getILoqRealEstateIdByEfecteAddressEntityId("irrelevant street name"))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getEfecteAddressNameByILoqRealEstateId")
    void testShouldReturnTheEfecteStreetAddressName() throws Exception {
        String result = configProvider.getEfecteAddressNameByILoqRealEstateId(testILoqRealEstateId);

        assertThat(result).isEqualTo(testEfecteAddressName1);
    }

    @Test
    @DisplayName("getEfecteAddressNameByILoqRealEstateId")
    void testShouldThrowAnExceptionWhenProvidedRealEstateIsNotConfigured_GetEfecteAddressNameByILoqRealEstateId()
            throws Exception {
        String invalidRealEstate = "invalid real estate";
        String expectedExceptionMessage = "ConfigProvider: Could not find an Efecte address matching the iLOQ real estate '"
                + invalidRealEstate + "'";

        assertThatThrownBy(() -> configProvider.getEfecteAddressNameByILoqRealEstateId(invalidRealEstate))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getEfecteAddressByILoqRealEstateId")
    void testShouldThrowAnExceptionWhenProvidedCustomerCodeIsNotConfigured_GetEfecteAddressByILoqRealEstateId()
            throws Exception {
        String invalidCustomerCode = "invalid_customer_code";
        String expectedExceptionMessage = "ConfigProvider: No iLOQ customer configuration found for customer code '"
                + invalidCustomerCode + "'";

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn(invalidCustomerCode);

        assertThatThrownBy(() -> configProvider
                .getEfecteAddressNameByILoqRealEstateId("irrelevant real estate name"))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("isValidEfecteSecurityAccess")
    void testShouldReturnTrueForValidEfecteSecurityAccesses() throws Exception {
        boolean result1 = configProvider.isValidEfecteSecurityAccess(testEfecteSecurityAccessEntityId1);
        boolean result2 = configProvider.isValidEfecteSecurityAccess(testEfecteSecurityAccessEntityId2);
        boolean result3 = configProvider.isValidEfecteSecurityAccess(testEfecteSecurityAccessEntityId3);

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(result3).isTrue();
    }

    @Test
    @DisplayName("isValidEfecteSecurityAccess")
    void testShouldReturnFalseForInvalidEfecteSecurityAccess() throws Exception {
        boolean result = configProvider.isValidEfecteSecurityAccess("invalid security access");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidILoqSecurityAccess")
    void testShouldReturnTrueForValidILoqSecurityAccesses() throws Exception {
        boolean result1 = configProvider.isValidILoqSecurityAccess(testILoqSecurityAccessId1);
        boolean result2 = configProvider.isValidILoqSecurityAccess(testILoqSecurityAccessId2);

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    @DisplayName("isValidILoqSecurityAccess")
    void testShouldReturnFalseForInvalidILoqSecurityAccess() throws Exception {
        boolean result = configProvider.isValidILoqSecurityAccess("invalid security access");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getEfecteSecurityAccessEntityIdByILoqSecurityAccessId")
    void testShouldReturnTheMatchingEfecteSecurityAccessEntityIdForAnILoqSecurityAccessId() throws Exception {
        String result = configProvider
                .getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(testILoqSecurityAccessId2);

        assertThat(result).isEqualTo(testEfecteSecurityAccessEntityId2);
    }

    @Test
    @DisplayName("getEfecteSecurityAccessEntityIdByILoqSecurityAccessId")
    void testShouldThrowAnExceptionWhenAMatchingEfecteSecurityAccessEntityIdIsNotFoundForAnILoqSecurityAccess()
            throws Exception {

        String invalidILoqSecurityAccess = "invalid iLOQ security access";
        String expectedExceptionMessage = "ConfigProvider: No Efecte security access found for an iLOQ security access id '"
                + invalidILoqSecurityAccess + "'";

        assertThatThrownBy(
                () -> configProvider.getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(
                        invalidILoqSecurityAccess))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getILoqSecurityAccessIdByEfecteSecurityAccessEntityId")
    void testShouldReturnTheMatchingILoqSecurityAccessIdForAnEfecteSecurityAccessEntityId() throws Exception {
        String result = configProvider
                .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(
                        testEfecteSecurityAccessEntityId2);

        assertThat(result).isEqualTo(testILoqSecurityAccessId2);
    }

    @Test
    @DisplayName("getILoqSecurityAccessIdByEfecteSecurityAccessEntityId")
    void testShouldThrowAnExceptionWhenAMatchingILoqSecurityAccessIsNotFoundForAnEfecteSecurityAccess()
            throws Exception {
        String invalidEfecteSecurityAccess = "invalid Efecte security access";
        String expectedExceptionMessage = "ConfigProvider: No iLOQ security access found for an Efecte security access id '"
                + invalidEfecteSecurityAccess + "'";

        assertThatThrownBy(
                () -> configProvider.getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(
                        invalidEfecteSecurityAccess))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getILoqZoneIdByILoqSecurityAccessId")
    void testShouldReturnTheILoqZoneIdForTheILoqSecurityAccessId() throws Exception {
        String result = configProvider.getILoqZoneIdByILoqSecurityAccessId(testILoqSecurityAccessId1);

        assertThat(result).isEqualTo(testILoqZoneId);
    }

    @Test
    @DisplayName("getILoqZoneIdByILoqSecurityAccessId")
    void testShouldThrowAnExceptionWhenAMatchingILoqZoneIsNotFoundForAnILoqSecurityAccess()
            throws Exception {
        String invalidILoqSecurityAccess = "invalid iLOQ security access";
        String expectedExceptionMessage = "ConfigProvider: No iLOQ zone found for an iLOQ security access id '"
                + invalidILoqSecurityAccess + "'";

        assertThatThrownBy(
                () -> configProvider.getILoqZoneIdByILoqSecurityAccessId(invalidILoqSecurityAccess))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId")
    void testSHouldReturnTheMatchingEfecteSecurityAccessEfecteIdForAnILoqSecurityAccessId() throws Exception {
        String result = configProvider
                .getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(testILoqSecurityAccessId1);

        assertThat(result).isEqualTo(testEfecteSecurityAccessEfecteId1);
    }

    @Test
    @DisplayName("getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId")
    void testShouldThrowAnExceptionWhenAMatchingEfecteSecurityAccessEfecteIdIsNotFoundForAnILoqSecurityAccess()
            throws Exception {

        String invalidILoqSecurityAccess = "invalid iLOQ security access";
        String expectedExceptionMessage = "ConfigProvider: No Efecte security access found for an iLOQ security access id '"
                + invalidILoqSecurityAccess + "'";

        assertThatThrownBy(
                () -> configProvider.getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(
                        invalidILoqSecurityAccess))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getEfecteAddressEfecteIdByILoqRealEstateId")
    void testShouldReturnTheEfecteAddressEfecteId() throws Exception {
        String result = configProvider.getEfecteAddressEfecteIdByILoqRealEstateId(testILoqRealEstateId);

        assertThat(result).isEqualTo(testEfecteAddressEfecteId);
    }

    @Test
    @DisplayName("getEfecteAddressEfecteIdByILoqRealEstateId")
    void testShouldThrowAnExceptionWhenProvidedRealEstateIsNotConfigured_GetEfecteAddressEfecteIdByILoqRealEstateId()
            throws Exception {
        String invalidRealEstate = "invalid real estate";
        String expectedExceptionMessage = "ConfigProvider: Could not find an Efecte address matching the iLOQ real estate '"
                + invalidRealEstate + "'";

        assertThatThrownBy(() -> configProvider.getEfecteAddressEfecteIdByILoqRealEstateId(invalidRealEstate))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("getILoqMainZoneId")
    void testShouldGetTheAgreedILoqMainZoneIdForEfecteAddress() throws Exception {
        String mainZoneId = configProvider.getILoqMainZoneId(testEfecteAddressEntityId1);

        assertThat(mainZoneId).isEqualTo(testILoqZoneId);
    }

    @Test
    @DisplayName("getILoqMainZoneId")
    void testShouldThrowAnExceptionWhenAMainZoneIdCannotBeFound()
            throws Exception {
        String invalidEfecteAddressEntityId = "invalid";
        String expectedExceptionMessage = "ConfigProvider: Could not find a main zone id for the given Efecte address entity id '"
                + invalidEfecteAddressEntityId + "'";

        assertThatThrownBy(() -> configProvider.getILoqMainZoneId(invalidEfecteAddressEntityId))
                .hasMessage(expectedExceptionMessage);
    }
}
