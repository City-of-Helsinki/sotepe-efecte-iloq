package fi.hel.processors;

import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.devikone.transports.Redis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import fi.hel.models.AuditExceptionRecord;
import fi.hel.models.enumerations.EnumDirection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("helper")
public class Helper {
    @Inject
    Redis redis;
    @ConfigProperty(name = "app.redis.prefix.auditMessage")
    String auditMessagePrefix;

    public String createHashFromJson(String json) {
        String sha3Hex = new DigestUtils("SHA3-256").digestAsHex(json);

        return sha3Hex;
    }

    public String writeAsJson(Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public String writeAsXml(Object obj) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();

        return xmlMapper.writeValueAsString(obj);
    }

    public String encodeToBase64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    public String urlEncode(String input) throws Exception {
        return URLEncoder.encode(input, "utf-8");
    }

    public String encodeUnicode(String input) throws Exception {
        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 128) {
                encoded.append(String.format("\\u%04x", (int) c));
            } else {
                encoded.append(c);
            }
        }
        return encoded.toString();
    }

    public <T> T writeAsPojo(String json, Class<T> valueType) throws JsonProcessingException {
        return new ObjectMapper().readValue(json, valueType);
    }

    public String createGUID() {
        return UUID.randomUUID().toString();
    }

    public String createDateTimeNow() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
        String pattern = "yyyy-MM-dd'T'HH-mm-ss";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    public String createAuditExceptionRecord(
            EnumDirection from,
            EnumDirection to,
            String entityId,
            String efecteId,
            String iLoqId,
            String message) throws Exception {
        String auditMessage = redis.get(auditMessagePrefix);

        return new AuditExceptionRecord(from, to, entityId, efecteId, iLoqId, auditMessage).toJson();
    }

    public String createIdentifier(String email, String name) {
        int MAX_LENGTH = 50;
        String emailPart = email.trim().toLowerCase();

        String identifier = createWithTwoLetters(emailPart, name);

        if (identifier.length() > MAX_LENGTH) {
            identifier = createWithOneLetter(emailPart, name);

            if (identifier.length() > MAX_LENGTH) {
                String username = emailPart.split("@")[0];
                identifier = createWithOneLetter(username, name);
            }
        }

        return identifier;
    }

    private String createWithTwoLetters(String emailPart, String name) {
        String[] nameParts = name.trim().split("\\s+");
        StringBuilder namePrefix = new StringBuilder();
        for (String part : nameParts) {
            if (!part.isEmpty()) {
                namePrefix.append(part.length() > 1 ? part.substring(0, 2) : part.substring(0, 1));
            }
        }
        return emailPart + "#" + namePrefix.toString().toUpperCase();
    }

    private String createWithOneLetter(String emailPart, String name) {
        String[] nameParts = name.trim().split("\\s+");
        StringBuilder namePrefix = new StringBuilder();
        for (String part : nameParts) {
            if (!part.isEmpty()) {
                namePrefix.append(part.charAt(0));
            }
        }
        return emailPart + "#" + namePrefix.toString().toUpperCase();
    }
}
