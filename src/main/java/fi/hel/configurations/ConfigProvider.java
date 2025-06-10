package fi.hel.configurations;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;

import org.apache.camel.support.jsse.SSLContextParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.hel.models.CustomerConfiguration;
import fi.hel.models.CustomerRealEstate;
import fi.hel.models.CustomerSecurityAccess;
import fi.hel.models.CustomerZone;
import fi.hel.processors.ResourceInjector;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("configProvider")
public class ConfigProvider {

    @Inject
    ResourceInjector ri;
    @ConfigProperty(name = "CUSTOMER_CONFIGURATION")
    String customerConfigurationJson;

    private List<CustomerConfiguration> customerConfigurations;

    @PostConstruct
    public void initCustomerConfigurations() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        List<CustomerConfiguration> customerConfigurations = objectMapper.readValue(
                customerConfigurationJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CustomerConfiguration.class));

        this.customerConfigurations = customerConfigurations;
    }

    public List<CustomerConfiguration> getCustomerConfigurations() {
        return this.customerConfigurations;
    }

    public SSLContextParameters getSSLContextParameters() {
        return new CustomSSLContextParameters();
    }

    public HostnameVerifier getHostnameVerifier() {
        return new CustomHostnameVerifier();
    }

    public List<String> getConfiguredCustomerCodes() {
        List<String> customerCodes = new ArrayList<>();

        for (CustomerConfiguration customerConfiguration : this.customerConfigurations) {
            customerCodes.add(customerConfiguration.getCustomerCode());
        }

        return customerCodes;
    }

    public List<String> getConfiguredILoqRealEstateIds() throws Exception {
        List<String> configuredRealEstateIdsForCustomerCode = new ArrayList<>();
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerRealEstate customerRealEstate : customerConfiguration.getRealEstates()) {
            configuredRealEstateIdsForCustomerCode.add(
                    customerRealEstate.getILoq().getId());
        }
        return configuredRealEstateIdsForCustomerCode;
    }

    public boolean isValidEfecteAddress(String efecteAddressEntityId) {
        for (CustomerConfiguration customerConfigurations : customerConfigurations) {
            for (CustomerRealEstate customerRealEstate : customerConfigurations.getRealEstates()) {
                if (customerRealEstate.getEfecte().getEntityId().equals(efecteAddressEntityId)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getILoqCustomerCodeByEfecteAddress(String efecteAddress) throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfigurationByEfecteAddress(efecteAddress);

        return customerConfiguration.getCustomerCode();
    }

    public void saveCurrentCredentialsToRedis(String customerCode) throws Exception {
        ri.getRedis().set(ri.getILoqCurrentCustomerCodePrefix(), customerCode);

        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        ri.getRedis().set(ri.getILoqCurrentCustomerCodePasswordPrefix(),
                customerConfiguration.getCustomerCodePassword());
    }

    public String getILoqRealEstateIdByEfecteAddressEntityId(String efecteAddressEntityId)
            throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerRealEstate customerRealEstate : customerConfiguration.getRealEstates()) {
            if (customerRealEstate.getEfecte().getEntityId().equals(efecteAddressEntityId)) {
                return customerRealEstate.getILoq().getId();
            }
        }

        throw new Exception(
                "ConfigProvider: Could not find an iLOQ real estate matching the Efecte address entity id '"
                        + efecteAddressEntityId + "'");
    }

    public String getEfecteAddressNameByILoqRealEstateId(String realEstateId)
            throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerRealEstate customerRealEstate : customerConfiguration.getRealEstates()) {
            if (customerRealEstate.getILoq().getId().equals(realEstateId)) {
                return customerRealEstate.getEfecte().getName();
            }
        }

        throw new Exception(
                "ConfigProvider: Could not find an Efecte address matching the iLOQ real estate '" + realEstateId
                        + "'");
    }

    public boolean isValidEfecteSecurityAccess(String securityAccessEntityId)
            throws Exception {
        for (CustomerConfiguration customerConfigurations : customerConfigurations) {
            for (CustomerZone customerZone : customerConfigurations.getZones()) {
                for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                    if (customerSecurityAccess.getEfecte().getEntityId().equals(securityAccessEntityId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isValidILoqSecurityAccess(String securityAccessId)
            throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerZone customerZone : customerConfiguration.getZones()) {
            for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                if (customerSecurityAccess.getILoq().getId().equals(securityAccessId)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(String efecteSecurityAccessEntityId)
            throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerZone customerZone : customerConfiguration.getZones()) {
            for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                if (customerSecurityAccess.getEfecte().getEntityId().equals(efecteSecurityAccessEntityId)) {
                    return customerSecurityAccess.getILoq().getId();
                }
            }

        }

        throw new Exception(
                "ConfigProvider: No iLOQ security access found for an Efecte security access id '"
                        + efecteSecurityAccessEntityId
                        + "'");
    }

    public String getILoqSecurityAccessNameByEfecteSecurityAccessEntityId(String efecteSecurityAccessEntityId)
            throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerZone customerZone : customerConfiguration.getZones()) {
            for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                if (customerSecurityAccess.getEfecte().getEntityId().equals(efecteSecurityAccessEntityId)) {
                    return customerSecurityAccess.getILoq().getName();
                }
            }

        }

        throw new Exception(
                "ConfigProvider: No iLOQ security access found for an Efecte security access id '"
                        + efecteSecurityAccessEntityId
                        + "'");
    }

    public String getILoqZoneIdByILoqSecurityAccessId(String iLoqSecurityAccessId) throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerZone customerZone : customerConfiguration.getZones()) {
            for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                if (customerSecurityAccess.getILoq().getId().equals(iLoqSecurityAccessId)) {
                    return customerZone.getId();
                }
            }
        }

        throw new Exception(
                "ConfigProvider: No iLOQ zone found for an iLOQ security access id '"
                        + iLoqSecurityAccessId
                        + "'");
    }

    public String getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(
            String iLoqSecurityAccessId) throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerZone customerZone : customerConfiguration.getZones()) {
            for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                if (customerSecurityAccess.getILoq().getId().equals(iLoqSecurityAccessId)) {
                    return customerSecurityAccess.getEfecte().getEntityId();
                }
            }
        }

        throw new Exception(
                "ConfigProvider: No Efecte security access found for an iLOQ security access id '"
                        + iLoqSecurityAccessId
                        + "'");
    }

    public String getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(String iLoqSecurityAccessId) throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerZone customerZone : customerConfiguration.getZones()) {
            for (CustomerSecurityAccess customerSecurityAccess : customerZone.getSecurityAccesses()) {
                if (customerSecurityAccess.getILoq().getId().equals(iLoqSecurityAccessId)) {
                    return customerSecurityAccess.getEfecte().getEfecteId();
                }
            }
        }

        throw new Exception(
                "ConfigProvider: No Efecte security access found for an iLOQ security access id '"
                        + iLoqSecurityAccessId
                        + "'");
    }

    public String getEfecteAddressEfecteIdByILoqRealEstateId(String iLoqRealEstateId) throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerRealEstate customerRealEstate : customerConfiguration.getRealEstates()) {
            if (customerRealEstate.getILoq().getId().equals(iLoqRealEstateId)) {
                return customerRealEstate.getEfecte().getEfecteId();
            }
        }

        throw new Exception(
                "ConfigProvider: Could not find an Efecte address matching the iLOQ real estate '" + iLoqRealEstateId
                        + "'");
    }

    public String getILoqMainZoneId(String efecteAddressEntityId) throws Exception {
        CustomerConfiguration customerConfiguration = getCustomerConfiguration();

        for (CustomerRealEstate customerRealEstate : customerConfiguration.getRealEstates()) {
            if (customerRealEstate.getEfecte().getEntityId().equals(efecteAddressEntityId)) {
                return customerRealEstate.getAgreedMainZoneId();
            }
        }

        throw new Exception(
                "ConfigProvider: Could not find a main zone id for the given Efecte address entity id '"
                        + efecteAddressEntityId
                        + "'");
    }

    private CustomerConfiguration getCustomerConfigurationByEfecteAddress(String efecteAddressEntityId)
            throws Exception {
        for (CustomerConfiguration customerConfiguration : this.customerConfigurations) {
            List<CustomerRealEstate> customerRealEstates = customerConfiguration.getRealEstates();

            for (CustomerRealEstate customerRealEstate : customerRealEstates) {
                if (customerRealEstate.getEfecte().getEntityId().equals(efecteAddressEntityId)) {
                    return customerConfiguration;
                }
            }
        }

        throw new Exception(
                "ConfigProvider: No iLOQ customer code configured for Efecte address '" + efecteAddressEntityId + "'");
    }

    private CustomerConfiguration getCustomerConfiguration() throws Exception {
        String customerCode = ri.getRedis().get(ri.getILoqCurrentCustomerCodePrefix());

        for (CustomerConfiguration customerConfiguration : this.customerConfigurations) {
            if (customerConfiguration.getCustomerCode().equals(customerCode)) {
                return customerConfiguration;
            }
        }

        throw new Exception(
                "ConfigProvider: No iLOQ customer configuration found for customer code '" + customerCode + "'");
    }

}
