package fi.hel.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.hel.models.builders.EfecteAttributeBuilder;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EfecteEntityTest {

    @Test
    @DisplayName("getKeyId")
    void testShouldReturnAttributes() throws Exception {
        String expectedEfecteKeyId = "1234";
        String expectedKeyHolderId = "1357";
        String expectedKeyHolderName = "Smith John";
        String expectedStreetAddressId = "4672";
        String expectedStreetAddressName = "Kotikatu 1";
        String expectedSecurityAccessId = "61369";
        String expectedSecurityAccessName = "1 - Lääkehuone";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withAttributes(
                        buildEfecteAttributeWithValue("2937", expectedEfecteKeyId),
                        buildEfecteAttributeWithReference("2930", expectedKeyHolderId,
                                expectedKeyHolderName),
                        buildEfecteAttributeWithReference("2928", expectedStreetAddressId,
                                expectedStreetAddressName),
                        buildEfecteAttributeWithReference("2929", expectedSecurityAccessId,
                                expectedSecurityAccessName))
                .build();

        String efecteIdValue = efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID);
        EfecteReference keyHolderReference = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER).get(0);
        EfecteReference streetAddressReference = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_STREET_ADDRESS).get(0);
        EfecteReference securityAccessReference = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS).get(0);

        assertThat(efecteIdValue).isEqualTo(expectedEfecteKeyId);

        assertThat(keyHolderReference.getId()).isEqualTo(expectedKeyHolderId);
        assertThat(keyHolderReference.getName()).isEqualTo(expectedKeyHolderName);

        assertThat(streetAddressReference.getId()).isEqualTo(expectedStreetAddressId);
        assertThat(streetAddressReference.getName()).isEqualTo(expectedStreetAddressName);

        assertThat(securityAccessReference.getId()).isEqualTo(expectedSecurityAccessId);
        assertThat(securityAccessReference.getName()).isEqualTo(expectedSecurityAccessName);
    }

    private EfecteAttribute buildEfecteAttributeWithValue(String attributeId, String value) {
        return new EfecteAttributeBuilder()
                .withId(attributeId)
                .withValue(value)
                .build();
    }

    private EfecteAttribute buildEfecteAttributeWithReference(String attributeId, String referenceId,
            String referenceName) {
        return new EfecteAttributeBuilder()
                .withId(attributeId)
                .withReference(referenceId, referenceName)
                .build();
    }
}
