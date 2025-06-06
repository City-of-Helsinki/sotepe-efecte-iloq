package fi.hel.mappers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteReference;
import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("iLoqKeyMapper")
public class ILoqKeyMapper {

    @Inject
    ResourceInjector ri;

    public Set<String> buildUpdatedILoqSecurityAccesses(Set<String> efecteSecurityAccessEntityIds)
            throws Exception {
        Set<String> iLoqSecurityAccessIds = new HashSet<>();

        for (String id : efecteSecurityAccessEntityIds) {
            iLoqSecurityAccessIds.add(ri.getConfigProvider().getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id));
        }

        return iLoqSecurityAccessIds;
    }

    public ILoqKeyImport buildNewILoqKey(EfecteEntity efecteEntity) throws Exception {
        EfecteReference streetAddressReference = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_STREET_ADDRESS).get(0);

        ILoqKey iLoqKey = new ILoqKey();
        setDefaultFields(iLoqKey);
        iLoqKey.setFnKeyId(ri.getHelper().createGUID());
        iLoqKey.setDescription(streetAddressReference.getName());
        iLoqKey.setInfoText(efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID));
        iLoqKey.setExpireDate(convertToISO8601(efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_VALIDITY_DATE)));
        iLoqKey.setRealEstateId(
                ri.getConfigProvider().getILoqRealEstateIdByEfecteAddressEntityId(streetAddressReference.getId()));

        ILoqKeyImport iLoqKeyImport = new ILoqKeyImport();
        iLoqKeyImport.setSecurityAccessIds(resolveILoqSecurityAccessIds(efecteEntity));
        iLoqKeyImport.setZoneIds(resolveILoqZoneIds(iLoqKeyImport.getSecurityAccessIds()));

        String iLoqPersonId;
        if (hasKeyHolder(efecteEntity)) {
            iLoqPersonId = resolveILoqPersonId(efecteEntity, iLoqKeyImport.getZoneIds());
        } else {
            iLoqPersonId = resolveILoqPersonIdForOutsider(efecteEntity, iLoqKeyImport.getZoneIds());
        }

        iLoqKey.setPersonId(iLoqPersonId);
        iLoqKeyImport.setKey(iLoqKey);

        return iLoqKeyImport;
    }

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    public ILoqKeyImport buildUpdatedILoqKey(EfecteEntity efecteEntity) throws Exception {
        // We need to retrieve the current iLOQ key first to preserve values that are not being updated
        ILoqKeyResponse iLoqKeyResponse = getILoqKey(
                efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_EXTERNAL_ID));

        return mapILoqKeyResponse(efecteEntity, iLoqKeyResponse, false);
    }

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    public ILoqKeyImport buildUpdatedILoqKey(EfecteEntity efecteEntity, ILoqKeyResponse iLoqKeyResponse)
            throws Exception {
        return mapILoqKeyResponse(efecteEntity, iLoqKeyResponse, false);
    }

    public ILoqKeyImport buildUpdatedILoqKey(EfecteEntity efecteEntity, ILoqKeyResponse iLoqKeyResponse,
            boolean isPassive)
            throws Exception {
        return mapILoqKeyResponse(efecteEntity, iLoqKeyResponse, isPassive);
    }

    private ILoqKeyImport mapILoqKeyResponse(EfecteEntity efecteEntity, ILoqKeyResponse iLoqKeyResponse,
            boolean isPassive)
            throws Exception {
        ILoqKey updatedILoqKey = new ILoqKey(iLoqKeyResponse.getFnKeyId());
        updatedILoqKey.setDescription(iLoqKeyResponse.getDescription());

        updatedILoqKey.setInfoText(
                isPassive
                        ? efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID) + " - Passiivinen"
                        : efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID));

        updatedILoqKey.setPersonId(iLoqKeyResponse.getPersonId());
        updatedILoqKey.setRealEstateId(iLoqKeyResponse.getRealEstateId());
        updatedILoqKey.setExpireDate(
                convertToISO8601(efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_VALIDITY_DATE)));
        updatedILoqKey.setRomId(iLoqKeyResponse.getRomId());
        updatedILoqKey.setStamp(iLoqKeyResponse.getStamp());
        updatedILoqKey.setTagKey(iLoqKeyResponse.getTagKey());
        updatedILoqKey.setState(iLoqKeyResponse.getState());

        ILoqKeyImport iLoqKeyImport = new ILoqKeyImport();
        iLoqKeyImport.setKey(updatedILoqKey);

        return iLoqKeyImport;
    }

    private ILoqKeyResponse getILoqKey(String iLoqKeyId) throws Exception {
        Exchange ex = new ExchangeBuilder(ri.getContext())
                .withProperty("iLoqKeyId", iLoqKeyId)
                .build();

        ILoqKeyResponse iLoqKey = ri.getTemplate().send(ri.getGetILoqKeyEndpointUri(), ex).getIn()
                .getBody(ILoqKeyResponse.class);

        if (ex.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw new Exception(
                    "ILoqKeyMapper: Fetching the iLOQ key failed: " + ex.getException().getMessage());
        }

        return iLoqKey;
    }

    private void setDefaultFields(ILoqKey iLoqNewKey) {
        iLoqNewKey.setKeyTypeMask(264); // S5 key type
        iLoqNewKey.setManufacturingInfo("");
        iLoqNewKey.setOnlineAccessCode("");
        iLoqNewKey.setRomId("");
        iLoqNewKey.setStamp("");
        iLoqNewKey.setTagKey("");
    }

    private String resolveILoqPersonId(EfecteEntity efecteEntity, List<String> zoneIds) throws Exception {
        EfecteReference keyHolderReference = efecteEntity.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER).get(0);
        String iLoqPersonId = ri.getILoqPersonResolver().resolveILoqPersonId(keyHolderReference.getId());

        if (iLoqPersonId == null) {
            handleIdResolutionError(efecteEntity, iLoqPersonId);

            iLoqPersonId = ri.getILoqPersonProcessor().createILoqPerson(efecteEntity, zoneIds);
        }

        return iLoqPersonId;
    }

    private String resolveILoqPersonIdForOutsider(EfecteEntity efecteEntity, List<String> zoneIds) throws Exception {
        String outsiderName = efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);
        String outsiderEmail = efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);
        String iLoqPersonId = ri.getILoqPersonResolver().resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);

        if (iLoqPersonId == null) {
            handleIdResolutionError(efecteEntity, iLoqPersonId);

            iLoqPersonId = ri.getILoqPersonProcessor().createILoqPerson(efecteEntity, zoneIds);
        }

        return iLoqPersonId;
    }

    private void handleIdResolutionError(EfecteEntity efecteEntity, String iLoqPersonId)
            throws InterruptedException, Exception {
        String auditMessage = ri.getRedis().get(ri.getAuditMessagePrefix());

        if (auditMessage != null) {
            String entityId = efecteEntity.getId();
            String efecteId = efecteEntity.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID);

            ri.getRedis().del(ri.getAuditMessagePrefix());
            ri.getAuditExceptionProcessor().throwAuditException(
                    EnumDirection.EFECTE, EnumDirection.ILOQ, entityId,
                    efecteId, iLoqPersonId, auditMessage);
        }
    }

    private List<String> resolveILoqSecurityAccessIds(EfecteEntity efecteEntity) throws Exception {
        List<EfecteReference> securityAccessReferences = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS);
        List<String> iLoqSecurityAccessIds = new ArrayList<>();

        for (EfecteReference securityAccessReference : securityAccessReferences) {
            String iLoqSecurityAccessId = ri.getConfigProvider()
                    .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(securityAccessReference.getId());

            iLoqSecurityAccessIds.add(iLoqSecurityAccessId);
        }

        return iLoqSecurityAccessIds;
    }

    private List<String> resolveILoqZoneIds(List<String> iLoqSecurityAccessIds) throws Exception {
        HashSet<String> iLoqZoneIds = new HashSet<>();

        for (String securityAccessId : iLoqSecurityAccessIds) {
            String iLoqZoneId = ri.getConfigProvider().getILoqZoneIdByILoqSecurityAccessId(securityAccessId);

            iLoqZoneIds.add(iLoqZoneId);
        }

        return new ArrayList<>(iLoqZoneIds);
    }

    private boolean hasKeyHolder(EfecteEntity key) throws Exception {
        try {
            key.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String convertToISO8601(String inputDateTime) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(inputDateTime, inputFormatter);

        return localDateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }
}
