package fi.hel.configurations;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class CustomHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }

    @Override
    public String toString() {
        return "CustomHostnameVerifier";
    }
}
