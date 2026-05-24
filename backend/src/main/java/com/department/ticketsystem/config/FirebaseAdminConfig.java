package com.department.ticketsystem.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FirebaseAdminConfig {

    private final Environment environment;
    private final ObjectMapper objectMapper;

    public FirebaseAdminConfig(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = openCredentialStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    private InputStream openCredentialStream() throws IOException {
        return openStructuredCredentialStream();
    }

    private InputStream openStructuredCredentialStream() throws IOException {
        String projectId = getProperty("firebase.service-account.project-id");
        String privateKey = getProperty("firebase.service-account.private-key");
        String clientEmail = getProperty("firebase.service-account.client-email");

        if (projectId.isBlank() || privateKey.isBlank() || clientEmail.isBlank()) {
            throw new IllegalStateException(
                    "Firebase Admin credentials are missing. Set FIREBASE_PROJECT_ID, "
                            + "FIREBASE_PRIVATE_KEY, and FIREBASE_CLIENT_EMAIL.");
        }

        Map<String, String> credentials = new LinkedHashMap<>();
        credentials.put("type", defaultValue(getProperty("firebase.service-account.type"), "service_account"));
        credentials.put("project_id", projectId);
        credentials.put("private_key_id", getProperty("firebase.service-account.private-key-id"));
        credentials.put("private_key", privateKey.replace("\\n", "\n"));
        credentials.put("client_email", clientEmail);
        credentials.put("client_id", getProperty("firebase.service-account.client-id"));
        credentials.put("auth_uri", defaultValue(getProperty("firebase.service-account.auth-uri"),
                "https://accounts.google.com/o/oauth2/auth"));
        credentials.put("token_uri", defaultValue(getProperty("firebase.service-account.token-uri"),
                "https://oauth2.googleapis.com/token"));
        credentials.put("auth_provider_x509_cert_url",
                defaultValue(getProperty("firebase.service-account.auth-provider-x509-cert-url"),
                        "https://www.googleapis.com/oauth2/v1/certs"));
        credentials.put("client_x509_cert_url", getProperty("firebase.service-account.client-x509-cert-url"));
        credentials.put("universe_domain", defaultValue(getProperty("firebase.service-account.universe-domain"),
                "googleapis.com"));

        byte[] json = objectMapper.writeValueAsBytes(credentials);
        return new ByteArrayInputStream(json);
    }

    private String getProperty(String key) {
        String value = environment.getProperty(key, "");
        return value == null ? "" : value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
