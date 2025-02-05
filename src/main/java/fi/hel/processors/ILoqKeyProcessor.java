package fi.hel.processors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntitySet;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteKeyState;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("iLoqKeyProcessor")
@SuppressWarnings("unchecked")
public class ILoqKeyProcessor {

    @Inject
    ResourceInjector ri;

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    public void processKey(Exchange ex) throws Exception {
        EfecteEntity efecteKey = ex.getProperty("efecteKey", EfecteEntity.class);
        String efecteEntityId = efecteKey.getId();
        String efecteId = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID);
        String iLoqKeyId = getILoqKeyId(efecteKey);

        ILoqKeyImport iLoqPayload = null;
        boolean shouldCreateILoqKey = false;
        boolean shouldUpdateILoqKey = false;
        boolean shouldDisableILoqKey = false;
        Set<String> newILoqSecurityAccessIds = null;
        PreviousEfecteKey oldPreviousEfecteKey = null;
        PreviousEfecteKey newPreviousEfecteKey = null;
        EfecteEntitySet efectePayload = null;
        String mainZoneId = null;

        Set<String> newEfecteSecurityAccessEntityIds = efecteKey
                .getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS)
                .stream()
                .map(sa -> sa.getId())
                .collect(Collectors.toSet());
        String keyState = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_STATE);
        String validityDate = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_VALIDITY_DATE);

        if (iLoqKeyId == null) {
            if (hasValidState(keyState)) {
                iLoqPayload = ri.getILoqKeyMapper().buildNewILoqKey(efecteKey);
                newILoqSecurityAccessIds = new HashSet<>(iLoqPayload.getSecurityAccessIds());
                iLoqKeyId = iLoqPayload.getKey().getFnKeyId();
                shouldCreateILoqKey = true;
                newPreviousEfecteKey = new PreviousEfecteKey(keyState, newEfecteSecurityAccessEntityIds, validityDate);
                mainZoneId = ri.getConfigProvider().getILoqMainZoneId(
                        efecteKey.getAttributeReferences(EnumEfecteAttribute.KEY_STREET_ADDRESS).get(0).getId());
                efectePayload = new EfecteEntitySet(new EfecteEntityBuilder()
                        .withTemplate(EnumEfecteTemplate.KEY.getCode())
                        .withKeyEfecteId(efecteId)
                        .withExternalId(iLoqKeyId)
                        .withState(EnumEfecteKeyState.AKTIIVINEN)
                        .build());
            } else {
                String auditMessage = "Efecte key state is '" + keyState + "'. Cannot create an iLOQ key.";

                ri.getAuditExceptionProcessor().throwAuditException(
                        EnumDirection.EFECTE, EnumDirection.ILOQ, efecteEntityId,
                        efecteId, iLoqKeyId, auditMessage);
            }
        } else {
            String previousEfecteKeyJson = ri.getRedis().get(ri.getPreviousKeyEfectePrefix() + efecteId);

            if (previousEfecteKeyJson != null) {
                oldPreviousEfecteKey = ri.getHelper().writeAsPojo(
                        previousEfecteKeyJson, PreviousEfecteKey.class);

                if (oldPreviousEfecteKey.getState().equals(EnumEfecteKeyState.AKTIIVINEN.getName())) {
                    if (keyState.equals(EnumEfecteKeyState.AKTIIVINEN.getName())) {
                        if (!oldPreviousEfecteKey.getSecurityAccesses().equals(newEfecteSecurityAccessEntityIds)) {
                            newILoqSecurityAccessIds = ri.getILoqKeyMapper()
                                    .buildUpdatedILoqSecurityAccesses(newEfecteSecurityAccessEntityIds);
                            shouldUpdateILoqKey = true;
                            newPreviousEfecteKey = new PreviousEfecteKey(
                                    keyState, newEfecteSecurityAccessEntityIds, validityDate);
                        }

                        if (oldPreviousEfecteKey.getValidityDate() == null
                                || !oldPreviousEfecteKey.getValidityDate().equals(validityDate)) {
                            iLoqPayload = ri.getILoqKeyMapper().buildUpdatedILoqKey(efecteKey);
                            shouldUpdateILoqKey = true;
                            newPreviousEfecteKey = new PreviousEfecteKey(
                                    keyState, newEfecteSecurityAccessEntityIds, validityDate);
                        }
                    } else if (keyState.equals(EnumEfecteKeyState.PASSIIVINEN.getName())) {
                        shouldDisableILoqKey = true;
                        newILoqSecurityAccessIds = new HashSet<>();
                        newPreviousEfecteKey = new PreviousEfecteKey(keyState, newEfecteSecurityAccessEntityIds);
                        // TODO: kun avain poistetaan iLOQ managerissa, tulee tämä hyödyntää (mutta ei tässä)
                        // efectePayload = new EfecteEntitySet(new EfecteEntityBuilder()
                        //         .withTemplate(EnumEfecteTemplate.KEY.getCode())
                        //         .withKeyEfecteId(efecteId)
                        //         .withExternalId(null)
                        //         .build());
                    }
                } else if (oldPreviousEfecteKey.getState().equals(EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA.getName())) {
                    if (keyState.equals(EnumEfecteKeyState.AKTIIVINEN.getName())) {
                        // The previous Efecte key state is still 'Odottaa aktivointia', which means that updating the key state after creating an iLOQ key has failed. The key state has been manually updated to Efecte.
                        System.out.println(
                                "Efecte key state has been manually updated at Efecte from 'Odottaa aktivointia' to 'Aktiivinen'");
                        newPreviousEfecteKey = new PreviousEfecteKey(keyState, newEfecteSecurityAccessEntityIds);
                    }
                } else if (oldPreviousEfecteKey.getState().equals(EnumEfecteKeyState.PASSIIVINEN.getName())) {
                    System.out.println(
                            "Efecte key has previously been passived. Will skip the handling now and wait for the matching iLOQ key to be removed (manually) before removing the mapped keys from the cycle.");
                }
            } else {
                String auditMessage = "The id has been mapped, but the previous Efecte key info has been deleted from Redis. Deleting also the mapped ids results in creating a new iLOQ key.";

                ri.getAuditExceptionProcessor().throwAuditException(
                        EnumDirection.EFECTE, EnumDirection.ILOQ, efecteEntityId,
                        efecteId, iLoqKeyId, auditMessage);
            }
        }

        ex.setProperty("iLoqPayload", iLoqPayload);
        ex.setProperty("iLoqKeyId", iLoqKeyId);
        ex.setProperty("shouldCreateILoqKey", shouldCreateILoqKey);
        ex.setProperty("shouldUpdateILoqKey", shouldUpdateILoqKey);
        ex.setProperty("shouldDisableILoqKey", shouldDisableILoqKey);
        ex.setProperty("newPreviousEfecteKey", newPreviousEfecteKey);
        ex.setProperty("newILoqSecurityAccessIds", newILoqSecurityAccessIds);
        ex.setProperty("efectePayload", efectePayload);
        ex.setProperty("mainZoneId", mainZoneId);
    }

    public void setCurrentILoqCredentials(Exchange ex) throws Exception {
        EfecteEntity efecteEntity = ex.getProperty("efecteKey", EfecteEntity.class);
        String efecteAddressEntityId = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_STREET_ADDRESS).get(0).getId();

        String previouslySetCustomerCode = ri.getRedis().get(ri.getILoqCurrentCustomerCodePrefix());
        String customerCode = ri.getConfigProvider().getILoqCustomerCodeByEfecteAddress(efecteAddressEntityId);

        if (previouslySetCustomerCode == null || !previouslySetCustomerCode.equals(customerCode)) {
            ri.getRedis().set(ri.getILoqCurrentCustomerCodeHasChangedPrefix(), "true");
            ri.getConfigProvider().saveCurrentCredentialsToRedis(customerCode);
        } else {
            ri.getRedis().set(ri.getILoqCurrentCustomerCodeHasChangedPrefix(), "false");
        }

    }

    private String getILoqKeyId(EfecteEntity efecteKey) throws Exception {
        try {
            return efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_EXTERNAL_ID);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasValidState(String state) {
        return state.equals(EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA.getName());
    }

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    public void getILoqKeysWithVerifiedRealEstate(Exchange ex) throws Exception {
        List<ILoqKeyResponse> allKeys = ex.getIn().getBody(List.class);
        List<String> configuredRealEstateIds = ri.getConfigProvider().getConfiguredILoqRealEstateIds();
        List<ILoqKeyResponse> result = new ArrayList<>();

        for (ILoqKeyResponse key : allKeys) {
            if (configuredRealEstateIds.contains(key.getRealEstateId())) {
                result.add(key);
            }
        }

        result.sort(Comparator.comparing(ILoqKeyResponse::getRealEstateId));

        ex.getIn().setBody(result);
    }

    public boolean hasValidSecurityAccesses(Exchange ex) throws Exception {
        Set<ILoqSecurityAccess> securityAccesses = ex.getIn().getBody(Set.class);

        for (ILoqSecurityAccess iLoqSecurityAccess : securityAccesses) {
            if (!ri.getConfigProvider().isValidILoqSecurityAccess(iLoqSecurityAccess.getSecurityAccessId())) {
                return false;
            }
        }

        return true;
    }

    public void buildEnrichedILoqKey(Exchange ex) throws Exception {
        Set<ILoqSecurityAccess> securityAccesses = ex.getIn().getBody(Set.class);
        ILoqKeyResponse iLoqKeyResponse = ex.getProperty("currentILoqKey", ILoqKeyResponse.class);

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setFnKeyId(iLoqKeyResponse.getFnKeyId());
        enrichedILoqKey.setPersonId(iLoqKeyResponse.getPersonId());
        enrichedILoqKey.setRealEstateId(iLoqKeyResponse.getRealEstateId());
        enrichedILoqKey.setInfoText(iLoqKeyResponse.getInfoText());
        enrichedILoqKey.setState(iLoqKeyResponse.getState());
        enrichedILoqKey.setSecurityAccesses(securityAccesses);

        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
    }

    public boolean isMissingAPerson(Exchange ex) {
        ILoqKeyResponse iLoqKey = ex.getProperty("currentILoqKey", ILoqKeyResponse.class);

        return iLoqKey.getPersonId() == null;
    }

}
