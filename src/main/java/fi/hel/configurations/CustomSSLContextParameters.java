package fi.hel.configurations;

import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

public class CustomSSLContextParameters extends SSLContextParameters {

    public CustomSSLContextParameters() {
        super();
        TrustManagersParameters tmp = new TrustManagersParameters();
        TrustManager trustManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // Trust all clients
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // Trust all servers
            }
        };

        tmp.setTrustManager(trustManager);
        this.setTrustManagers(tmp);
    }

    @Override
    public String toString() {
        return "CustomSSLContextParameters";
    }
}
