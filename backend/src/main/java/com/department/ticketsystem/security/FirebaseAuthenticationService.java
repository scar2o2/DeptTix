package com.department.ticketsystem.security;

import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthenticationService {

    private final UserRepository userRepository;

    public FirebaseAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UsernamePasswordAuthenticationToken authenticate(String authorizationHeader) {
        FirebaseToken decodedToken = verifyIdToken(extractToken(authorizationHeader));
        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Firebase token does not include an email address");
        }

        Role role = userRepository.findByFirebaseUid(decodedToken.getUid())
                .or(() -> userRepository.findByEmail(email))
                .map(user -> user.getRole())
                .orElse(Role.USER);

        FirebaseUserPrincipal principal = new FirebaseUserPrincipal(
                decodedToken.getUid(),
                email,
                decodedToken.getName(),
                Boolean.TRUE.equals(decodedToken.isEmailVerified()));

        return new UsernamePasswordAuthenticationToken(
                principal,
                extractToken(authorizationHeader),
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    public FirebaseToken verifyIdToken(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            if (decodedToken.getEmail() != null && !Boolean.TRUE.equals(decodedToken.isEmailVerified())) {
                throw new IllegalArgumentException("Please verify your email before accessing the application");
            }
            return decodedToken;
        } catch (FirebaseAuthException exception) {
            throw new IllegalArgumentException("Invalid or expired Firebase token");
        }
    }

    private String extractToken(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }
}
