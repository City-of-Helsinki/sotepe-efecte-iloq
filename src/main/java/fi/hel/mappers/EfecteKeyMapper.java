package fi.hel.mappers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fi.hel.models.EfecteAttributeImport;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EfecteEntityImport;
import fi.hel.models.EfecteEntitySetImport;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqPerson;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteKeyState;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EfecteKeyMapper {

    @Inject
    ResourceInjector ri;

    public EfecteEntitySetImport buildNewEfecteEntitySetImport(EnrichedILoqKey enrichedILoqKey) throws Exception {
        ILoqPerson iLoqPerson = enrichedILoqKey.getPerson();
        String iLoqPersonId = iLoqPerson.getPersonId();
        String keyHolderEntityIdentifierJson = ri.getRedis().get(ri.getMappedPersonILoqPrefix() + iLoqPersonId);

        if (keyHolderEntityIdentifierJson == null) {
            String auditMessage = "Unable to create a new key card in Efecte. No matching key holder was found for the specified iLOQ person (%s)."
                    .formatted(iLoqPersonId);
            ri.getAuditExceptionProcessor().throwAuditException(
                    EnumDirection.ILOQ, EnumDirection.EFECTE, null, null, enrichedILoqKey.getFnKeyId(),
                    auditMessage);
        }

        List<EfecteAttributeImport> attributeImports = new ArrayList<>();

        attributeImports.add(createStreetAddressAttribute(enrichedILoqKey.getRealEstateId()));
        attributeImports.add(createSecurityAccessAttribute(enrichedILoqKey.getSecurityAccesses()));
        attributeImports.add(createKeyHolderAttribute(keyHolderEntityIdentifierJson));
        attributeImports.add(createExternalIdAttribute(enrichedILoqKey.getFnKeyId()));
        attributeImports.add(createStateAttribute());
        attributeImports.add(createTypeAttribute());
        attributeImports.add(createValidityDateAttribute());

        EfecteEntityImport efecteEntityImport = new EfecteEntityImport(EnumEfecteTemplate.KEY, attributeImports);
        EfecteEntitySetImport entitySetImport = new EfecteEntitySetImport(efecteEntityImport);

        return entitySetImport;
    }

    public EfecteEntitySetImport buildEfecteEntitySetUpdate(EnrichedILoqKey enrichedILoqKey, String efecteId)
            throws Exception {
        List<EfecteAttributeImport> attributeImports = new ArrayList<>();

        attributeImports.add(createSecurityAccessAttribute(enrichedILoqKey.getSecurityAccesses()));
        attributeImports.add(createEfecteIdAttribute(efecteId));

        EfecteEntityImport efecteEntityImport = new EfecteEntityImport(EnumEfecteTemplate.KEY, attributeImports);
        EfecteEntitySetImport entitySetImport = new EfecteEntitySetImport(efecteEntityImport);

        return entitySetImport;
    }

    public EfecteEntitySetImport buildEfecteEntitySetUpdate(String iLoqKeyId, String efecteId)
            throws Exception {
        List<EfecteAttributeImport> attributeImports = new ArrayList<>();

        attributeImports.add(createExternalIdAttribute(iLoqKeyId));
        attributeImports.add(createStateAttribute());
        attributeImports.add(createEfecteIdAttribute(efecteId));

        EfecteEntityImport efecteEntityImport = new EfecteEntityImport(EnumEfecteTemplate.KEY, attributeImports);
        EfecteEntitySetImport entitySetImport = new EfecteEntitySetImport(efecteEntityImport);

        return entitySetImport;
    }

    private EfecteAttributeImport createStreetAddressAttribute(String iLoqRealEstateId) throws Exception {
        String efecteStreetAddressEfecteId = ri.getConfigProvider()
                .getEfecteAddressEfecteIdByILoqRealEstateId(iLoqRealEstateId);
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_STREET_ADDRESS, efecteStreetAddressEfecteId);
    }

    private EfecteAttributeImport createSecurityAccessAttribute(Set<ILoqSecurityAccess> iLoqSecurityAccesses)
            throws Exception {
        List<String> securityAccessEfecteIds = new ArrayList<>();

        for (ILoqSecurityAccess iLoqSecurityAccess : iLoqSecurityAccesses) {
            securityAccessEfecteIds.add(ri.getConfigProvider()
                    .getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(iLoqSecurityAccess.getSecurityAccessId()));
        }

        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_SECURITY_ACCESS, securityAccessEfecteIds);
    }

    private EfecteAttributeImport createKeyHolderAttribute(String keyHolderEntityIdentifierJson)
            throws Exception {
        EfecteEntityIdentifier efecteEntityIdentifier = ri.getHelper().writeAsPojo(keyHolderEntityIdentifierJson,
                EfecteEntityIdentifier.class);
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_HOLDER, efecteEntityIdentifier.getEfecteId());
    }

    private EfecteAttributeImport createExternalIdAttribute(String iLoqKeyId) {
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_EXTERNAL_ID, iLoqKeyId);
    }

    private EfecteAttributeImport createStateAttribute() {
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_STATE, EnumEfecteKeyState.AKTIIVINEN.getName());
    }

    private EfecteAttributeImport createTypeAttribute() {
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_TYPE, "iLOQ");
    }

    private EfecteAttributeImport createValidityDateAttribute() {
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_VALIDITY_DATE, createDatePlusDays(365));
    }

    private EfecteAttributeImport createEfecteIdAttribute(String efecteId) {
        return new EfecteAttributeImport(
                EnumEfecteAttribute.KEY_EFECTE_ID, efecteId);
    }

    private String createDatePlusDays(Integer daysToAdd) {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Helsinki"));
        return date.plusDays(daysToAdd).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " 00:00";
    }
}
