package fi.hel.processors;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EfecteEntitySetImport;
import fi.hel.models.EfecteReference;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteKeyState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("efecteKeyProcessor")
public class EfecteKeyProcessor {

    @Inject
    ResourceInjector ri;

    private String currentILoqRealEstateId;
    private String currentEfecteAddress;
    private List<EfecteEntity> efecteKeys;

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    public boolean isValidated(Exchange ex) throws Exception {
        EfecteEntity efecteKey = ex.getProperty("efecteKey", EfecteEntity.class);

        if (hasInvalidKeyType(efecteKey)) {
            return false;
        }

        if (!hasValidKeyState(efecteKey)) {
            return false;
        }

        if (hasInvalidStreetAddress(efecteKey)) {
            return false;
        }

        if (includesInvalidSecurityAccess(efecteKey)) {
            return false;
        }

        return true;
    }

    public void updatePreviousEfecteKeyValue(Exchange ex) throws Exception {
        PreviousEfecteKey previousEfecteKey = ex.getProperty("newPreviousEfecteKey", PreviousEfecteKey.class);
        previousEfecteKey.setState(EnumEfecteKeyState.AKTIIVINEN.getName());
    }

    private boolean hasInvalidKeyType(EfecteEntity efecteEntity) throws Exception {
        String efeceKeyType = efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_TYPE);

        return !efeceKeyType.equals("iLOQ");
    }

    private boolean hasValidKeyState(EfecteEntity efecteEntity) throws Exception {
        List<String> validKeyStates = List.of(
                EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA.getName(),
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                EnumEfecteKeyState.PASSIIVINEN.getName());
        String keyState = efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_STATE);

        return validKeyStates.contains(keyState);
    }

    private boolean hasInvalidStreetAddress(EfecteEntity efecteEntity) throws Exception {
        String efecteAddressEntityId = efecteEntity.getAttributeReferences(EnumEfecteAttribute.KEY_STREET_ADDRESS)
                .get(0)
                .getId();

        return !ri.getConfigProvider().isValidEfecteAddress(efecteAddressEntityId);
    }

    private boolean includesInvalidSecurityAccess(EfecteEntity efecteEntity) throws Exception {
        List<EfecteReference> securityAccesses = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS);

        for (EfecteReference securityAccess : securityAccesses) {
            if (!ri.getConfigProvider().isValidEfecteSecurityAccess(securityAccess.getId())) {
                return true;
            }
        }

        return false;
    }

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    public void buildEfecteKey(Exchange ex) throws Exception {
        ILoqKeyResponse iLoqKeyResponse = ex.getProperty("currentILoqKey", ILoqKeyResponse.class);
        EnrichedILoqKey enrichedILoqKey = ex.getProperty("enrichedILoqKey", EnrichedILoqKey.class);
        String iLoqKeyId = enrichedILoqKey.getFnKeyId();
        String realEstateId = enrichedILoqKey.getRealEstateId();
        Set<String> newILoqSecurityAccessIds = getNewILoqSecurityAccessIds(enrichedILoqKey.getSecurityAccesses());
        PreviousEfecteKey newPreviousEfecteKey = null;

        boolean shouldUpdateEfecteKey = false;
        boolean shouldCreateEfecteKey = false;
        boolean shouldUpdateILoqKey = false;
        EfecteEntitySetImport efectePayload = null;
        ILoqKeyImport iLoqPayload = null;
        String efecteKeyEntityId = null;

        String iLoqKeyInfoText = enrichedILoqKey.getInfoText();
        String efecteKeyEfecteId = null;
        String efecteEntityIdentifierJson = ri.getRedis().get(ri.getMappedKeyILoqPrefix() + iLoqKeyId);
        Set<String> previousILoqKeySecurityAccesses = ri.getRedis().getSet(ri.getPreviousKeyILoqPrefix() + iLoqKeyId);

        if (isMissing(iLoqKeyInfoText) && isMissing(efecteEntityIdentifierJson)) {
            // Key is not previously mapped
            if (!realEstateId.equals(this.currentILoqRealEstateId)) {
                initKeyProcessorVariables(realEstateId);
            }

            EfecteEntity equalEfecteKey = ri.getEfecteKeyResolver().buildEqualEfecteKey(enrichedILoqKey,
                    this.currentEfecteAddress);
            EfecteEntity foundMatchingKey = ri.getEfecteKeyResolver().findMatchingEfecteKey(
                    equalEfecteKey, efecteKeys);

            if (foundMatchingKey == null) {
                System.out.println("DEBUG: EfecteKeyProcessor.buildEfecteKey - no matching key found, building new");
                efectePayload = ri.getEfecteKeyMapper().buildNewEfecteEntitySetImport(enrichedILoqKey);
                shouldCreateEfecteKey = true;
                newPreviousEfecteKey = new PreviousEfecteKey(
                        EnumEfecteKeyState.AKTIIVINEN.getName(),
                        ri.getEfecteKeyResolver()
                                .getNewEfecteSecurityAccessEntityIds(enrichedILoqKey.getSecurityAccesses()),
                        efectePayload.getEntity().getAttributeByType(EnumEfecteAttribute.KEY_VALIDITY_DATE)
                                .getValues().get(0));
            } else {
                String matchingEfecteKeyState = foundMatchingKey.getAttributeValue(EnumEfecteAttribute.KEY_STATE);
                System.out.println("DEBUG: EfecteKeyProcessor.buildEfecteKey - a matching key was found, state: '"
                        + matchingEfecteKeyState + "', updating iLOQ key");

                shouldUpdateILoqKey = true;

                if (matchingEfecteKeyState.equals(EnumEfecteKeyState.PASSIIVINEN.getName())) {
                    iLoqPayload = ri.getILoqKeyMapper().buildUpdatedILoqKey(foundMatchingKey, iLoqKeyResponse, true);
                } else {
                    shouldUpdateEfecteKey = true;
                    saveMappedKeys(iLoqKeyId, foundMatchingKey);
                    efecteKeyEntityId = foundMatchingKey.getId();
                    efecteKeyEfecteId = foundMatchingKey.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID);
                    iLoqPayload = ri.getILoqKeyMapper().buildUpdatedILoqKey(foundMatchingKey, iLoqKeyResponse);
                    efectePayload = ri.getEfecteKeyMapper().buildEfecteEntitySetUpdate(iLoqKeyId,
                            foundMatchingKey.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID));
                    newPreviousEfecteKey = new PreviousEfecteKey(
                            EnumEfecteKeyState.AKTIIVINEN.getName(),
                            ri.getEfecteKeyResolver()
                                    .getNewEfecteSecurityAccessEntityIds(enrichedILoqKey.getSecurityAccesses()),
                            foundMatchingKey.getAttributeValue(EnumEfecteAttribute.KEY_VALIDITY_DATE));
                }
            }
        } else {
            // Key is previously mapped
            if (!Objects.equals(newILoqSecurityAccessIds, previousILoqKeySecurityAccesses)) {
                if (efecteEntityIdentifierJson == null) {
                    if (iLoqKeyInfoText.contains("Passiivinen")) {
                        throw new Exception(
                                "EfecteKeyProcessor.buildEfecteKey: The matching Efecte key has state 'Passiivinen'. This iLOQ key should not be present in this context and should be either returned or it's security accesses emptied.");
                    }
                    throw new Exception(
                            "EfecteKeyProcessor.buildEfecteKey: The previously mapped key (" + iLoqKeyInfoText
                                    + ") is missing the actual Redis mapping key '" + ri.getMappedKeyILoqPrefix()
                                    + iLoqKeyId
                                    + "'. Cannot continue processing.");
                }

                EfecteEntityIdentifier efecteEntityIdentifier = ri.getHelper()
                        .writeAsPojo(efecteEntityIdentifierJson, EfecteEntityIdentifier.class);
                efecteKeyEntityId = efecteEntityIdentifier.getEntityId();
                efecteKeyEfecteId = efecteEntityIdentifier.getEfecteId();

                efectePayload = ri.getEfecteKeyMapper().buildEfecteEntitySetUpdate(enrichedILoqKey, efecteKeyEfecteId);
                shouldUpdateEfecteKey = true;
                newPreviousEfecteKey = new PreviousEfecteKey(
                        EnumEfecteKeyState.AKTIIVINEN.getName(),
                        ri.getEfecteKeyResolver()
                                .getNewEfecteSecurityAccessEntityIds(enrichedILoqKey.getSecurityAccesses()));

                String previousEfecteKeyJson = ri.getRedis().get(ri.getPreviousKeyEfectePrefix() + efecteKeyEfecteId);
                PreviousEfecteKey previousEfecteKey = null;

                if (previousEfecteKeyJson != null) {
                    previousEfecteKey = ri.getHelper().writeAsPojo(previousEfecteKeyJson,
                            PreviousEfecteKey.class);
                    newPreviousEfecteKey.setValidityDate(previousEfecteKey.getValidityDate());
                }

                if (isMissing(iLoqKeyInfoText)) {
                    // The Efecte key has been previously created by the integration (iLOQ -> Efecte) and therefore the 'InfoText' field is not yet populated. 'InfoText' field on iLOQ key might also have been manually deleted.
                    shouldUpdateILoqKey = true;

                    EfecteEntityBuilder efecteEntityBuilder = new EfecteEntityBuilder()
                            .withKeyEfecteId(efecteKeyEfecteId);

                    if (previousEfecteKeyJson != null) {
                        efecteEntityBuilder.withValidityDate(previousEfecteKey.getValidityDate());
                    }

                    iLoqPayload = ri.getILoqKeyMapper().buildUpdatedILoqKey(efecteEntityBuilder.build(),
                            iLoqKeyResponse);
                }
            }
        }

        ex.setProperty("efectePayload", efectePayload);
        ex.setProperty("efecteKeyEntityId", efecteKeyEntityId);
        ex.setProperty("efecteKeyEfecteId", efecteKeyEfecteId);
        ex.setProperty("iLoqPayload", iLoqPayload);
        ex.setProperty("shouldUpdateEfecteKey", shouldUpdateEfecteKey);
        ex.setProperty("shouldCreateEfecteKey", shouldCreateEfecteKey);
        ex.setProperty("shouldUpdateILoqKey", shouldUpdateILoqKey);
        ex.setProperty("newPreviousEfecteKey", newPreviousEfecteKey);
        ex.setProperty("newILoqSecurityAccessIds", newILoqSecurityAccessIds);

    }

    public void resetCache() {
        this.currentILoqRealEstateId = null;
        this.currentEfecteAddress = null;
        this.efecteKeys = null;
    }

    private boolean isMissing(String value) {
        return value == null || value.isEmpty();
    }

    private void initKeyProcessorVariables(String realEstateId) throws Exception {
        this.currentILoqRealEstateId = realEstateId;
        this.currentEfecteAddress = ri.getConfigProvider()
                .getEfecteAddressNameByILoqRealEstateId(realEstateId);
        this.efecteKeys = listUnmappedEfecteKeysByAddress(currentEfecteAddress);
    }

    @SuppressWarnings("unchecked")
    private List<EfecteEntity> listUnmappedEfecteKeysByAddress(String efecteStreetAddress) throws Exception {
        String efecteQuery = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'avain'
                    AND $avain_tyyppi$ = 'iLOQ'
                    AND $avain_katuosoite$ = '%s'
                    AND $avain_external_id$ IS NULL
                """
                .formatted(ri.getHelper().urlEncode(efecteStreetAddress))
                .replaceAll("\\s+", " ")
                .trim();

        Exchange ex = new ExchangeBuilder(ri.getContext())
                .withProperty("efecteEntityType", "key")
                .withProperty("efecteQuery", efecteQuery)
                .build();

        ri.getTemplate().send(ri.getGetEfecteEntityEndpointUri(), ex);

        if (ex.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw new Exception("EfecteKeyProcessor.listUnmappedEfecteKeysByAddress: Fetching all Efecte keys failed");
        }

        return ex.getIn().getBody(List.class);
    }

    private Set<String> getNewILoqSecurityAccessIds(Set<ILoqSecurityAccess> iLoqSecurityAccesses) {
        return iLoqSecurityAccesses.stream()
                .map(sa -> sa.getSecurityAccessId())
                .collect(Collectors.toSet());
    }

    private void saveMappedKeys(String iLoqKeyId, EfecteEntity efecteKey) throws Exception {
        String entityId = efecteKey.getId();
        String efecteId = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(entityId, efecteId);
        String efecteEntityIdentifierJson = ri.getHelper().writeAsJson(efecteEntityIdentifier);

        String efectePrefix = ri.getMappedKeyEfectePrefix() + efecteId;
        String iLoqPrefix = ri.getMappedKeyILoqPrefix() + iLoqKeyId;

        ri.getRedis().set(efectePrefix, iLoqKeyId);
        ri.getRedis().set(iLoqPrefix, efecteEntityIdentifierJson);
    }
}
