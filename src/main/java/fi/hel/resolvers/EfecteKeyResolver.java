package fi.hel.resolvers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EfecteReference;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EfecteKeyResolver {

    @Inject
    ResourceInjector ri;

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    public EfecteEntity buildEqualEfecteKey(
            EnrichedILoqKey iLoqKey,
            String efecteAddress)
            throws Exception {
        String iLoqPersonId = iLoqKey.getPersonId();
        String efectePersonIdentifier = ri.getEfectePersonResolver().resolveEfectePersonIdentifier(iLoqPersonId);

        Set<String> efecteSecurityAccessEntityIds = getNewEfecteSecurityAccessEntityIds(iLoqKey.getSecurityAccesses());
        List<EfecteReference> efecteSecurityAccessReferences = createEfecteSecurityAccessReferences(
                efecteSecurityAccessEntityIds);

        EfecteEntityBuilder entityBuilder = new EfecteEntityBuilder();

        if (isValidEntityId(efectePersonIdentifier)) {
            // Person has been mapped or searching Efecte key holders resulted in one match
            entityBuilder.withKeyHolderReference(efectePersonIdentifier);
        } else {
            entityBuilder.withIsOutsider(true);
            if (isEfecteEntityIdentifierJson(efectePersonIdentifier)) {
                // Person has been mapped, but is an outsider
                EfecteEntityIdentifier efecteEntityIdentifier = ri.getHelper().writeAsPojo(efectePersonIdentifier,
                        EfecteEntityIdentifier.class);
                entityBuilder
                        .withOutsiderEmail(efecteEntityIdentifier.getOutsiderEmail())
                        .withOutsiderName(efecteEntityIdentifier.getOutsiderName());
            } else {
                // Person has not been mapped and key holder was not found in Efecte by name
                entityBuilder.withOutsiderName(efectePersonIdentifier);
            }
        }

        if (!efecteSecurityAccessEntityIds.isEmpty()) {
            entityBuilder.withSecurityAccesses(efecteSecurityAccessReferences.toArray(new EfecteReference[0]));
        }

        return entityBuilder.build();
    }

    public EfecteEntity findMatchingEfecteKey(
            EfecteEntity builtKey, List<EfecteEntity> efecteKeys)
            throws Exception {
        if (hasKeyHolder(builtKey)) {
            for (EfecteEntity key : efecteKeys) {
                if (hasKeyHolder(key)) {
                    if (hasMatchingKeyHolder(builtKey, key) && hasMatchingSecurityAccesses(builtKey, key)) {
                        return key;
                    }
                }
            }
        } else {
            for (EfecteEntity key : efecteKeys) {
                if (!hasKeyHolder(key)) {
                    if (hasMatchingOutsider(builtKey, key) && hasMatchingSecurityAccesses(builtKey, key)) {
                        return key;
                    }
                }
            }
        }

        return null;
    }

    public Set<String> getNewEfecteSecurityAccessEntityIds(Set<ILoqSecurityAccess> iLoqSecurityAccesses)
            throws Exception {
        Set<String> efecteSecurityAccessIds = new HashSet<>();

        for (ILoqSecurityAccess iLoqSecurityAccess : iLoqSecurityAccesses) {
            String efecteSecurityAccessId = ri.getConfigProvider()
                    .getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(
                            iLoqSecurityAccess.getSecurityAccessId());

            efecteSecurityAccessIds.add(efecteSecurityAccessId);
        }

        return efecteSecurityAccessIds;
    }

    private List<EfecteReference> createEfecteSecurityAccessReferences(Set<String> securityAccessEntityIds) {
        List<EfecteReference> references = new ArrayList<>();

        for (String entityId : securityAccessEntityIds) {
            references.add(new EfecteReference(entityId));
        }

        return references;
    }

    private boolean hasMatchingKeyHolder(EfecteEntity builtKey, EfecteEntity key) throws Exception {
        String builtKeyKeyHolderId = builtKey.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER).get(0).getId();
        String keyKeyHolderId = key.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER).get(0).getId();

        return keyKeyHolderId.equals(builtKeyKeyHolderId);
    }

    private boolean hasMatchingOutsider(EfecteEntity builtKey, EfecteEntity key) throws Exception {
        if (hasMappedOutsider(builtKey)) {
            String builtKeyOutsiderEmail = builtKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);
            String builtKeyOutsiderName = builtKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);

            try {
                String keyOutsiderEmail = key.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);
                String keyOutsiderName = key.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);

                return keyOutsiderEmail.equals(builtKeyOutsiderEmail) && keyOutsiderName.equals(builtKeyOutsiderName);
            } catch (Exception e) {
                return false;
            }
        } else {
            String builtKeyOutsiderName = builtKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);
            String keyOutsiderName = key.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);

            return keyOutsiderName.equals(builtKeyOutsiderName);
        }
    }

    private boolean hasMatchingSecurityAccesses(EfecteEntity builtKey, EfecteEntity key) throws Exception {
        List<EfecteReference> keySecurityAccesses = key.getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS);
        List<EfecteReference> builtKeySecurityAccesses = builtKey
                .getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS);

        if (keySecurityAccesses == null && builtKeySecurityAccesses == null) {
            return true;
        }

        if (keySecurityAccesses == null || builtKeySecurityAccesses == null) {
            return false;
        }

        Set<String> keySecurityAccessEntityIds = keySecurityAccesses.stream()
                .map(sa -> sa.getId())
                .collect(Collectors.toSet());
        Set<String> builtKeySecurityAccessEntityIds = builtKeySecurityAccesses.stream()
                .map(sa -> sa.getId())
                .collect(Collectors.toSet());

        return keySecurityAccessEntityIds.equals(builtKeySecurityAccessEntityIds);
    }

    private boolean isValidEntityId(String entityId) {
        try {
            Integer.parseInt(entityId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isEfecteEntityIdentifierJson(String input) {
        try {
            new JSONObject(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasKeyHolder(EfecteEntity key) throws Exception {
        try {
            key.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasMappedOutsider(EfecteEntity key) throws Exception {
        try {
            key.getAttributeReferences(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
