package com.department.ticketsystem.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class FirebaseAdminConfig {

    private final ResourceLoader resourceLoader;

    public FirebaseAdminConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseApp firebaseApp(@Value("${firebase.service-account-path:}") String configuredPath) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        String serviceAccountPath = resolveCredentialPath(configuredPath);
        try (InputStream serviceAccount = openCredentialStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    private String resolveCredentialPath(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return configuredPath.trim();
        }

        String fallbackPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (fallbackPath != null && !fallbackPath.isBlank()) {
            return fallbackPath.trim();
        }

        throw new IllegalStateException(
                "Firebase Admin credentials are missing. Set firebase.service-account-path, "
                        + "FIREBASE_SERVICE_ACCOUNT_PATH, or GOOGLE_APPLICATION_CREDENTIALS.");
    }

    private InputStream openCredentialStream(String serviceAccountPath) throws IOException {
        if (serviceAccountPath.startsWith("classpath:")) {
            Resource resource = resourceLoader.getResource(serviceAccountPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Firebase service account file not found: " + serviceAccountPath);
            }
            return resource.getInputStream();
        }

        Path filePath = Path.of(serviceAccountPath);
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Firebase service account file not found: " + serviceAccountPath);
        }
        return new FileInputStream(filePath.toFile());
    }
}
