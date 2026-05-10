package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.UserResponse;
import com.department.ticketsystem.dto.UserSyncRequest;
import com.department.ticketsystem.model.Department;
import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.model.SavedPassenger;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.dto.SavedPassengerRequest;
import com.department.ticketsystem.dto.SavedPassengerResponse;
import com.department.ticketsystem.repository.SavedPassengerRepository;
import com.department.ticketsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import com.department.ticketsystem.security.FirebaseUserPrincipal;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppUserService {

    private static final String LEGACY_PASSWORD_PLACEHOLDER = "FIREBASE_AUTH_LEGACY_PLACEHOLDER";
    private static final String VELTECH_EMAIL_DOMAIN = "@veltech.edu.in";

    private final UserRepository userRepository;
    private final SavedPassengerRepository savedPassengerRepository;
    private final Set<String> adminEmails;

    public AppUserService(UserRepository userRepository,
                          SavedPassengerRepository savedPassengerRepository,
                          @Value("${app.auth.admin-emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.savedPassengerRepository = savedPassengerRepository;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public UserResponse syncUser(FirebaseUserPrincipal principal, UserSyncRequest request) {
        if (request == null || request.department() == null || request.department().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department is required");
        }

        User user = userRepository.findByFirebaseUid(principal.getUid())
                .orElseGet(() -> userRepository.findByEmail(principal.getEmail()).orElseGet(User::new));

        boolean isNew = user.getId() == null;
        Role resolvedRole = resolveRole(principal.getEmail());
        if (isNew && resolvedRole != Role.ADMIN && !isVelTechEmail(principal.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "New users must use a @veltech.edu.in email address");
        }
        user.setFirebaseUid(principal.getUid());
        user.setEmail(principal.getEmail());
        user.setName(resolveName(user, principal));
        user.setDepartment(Department.fromUserValue(request.department()).name());
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(LEGACY_PASSWORD_PLACEHOLDER);
        }
        if (isNew) {
            user.setRole(resolvedRole);
        } else if (user.getRole() == null) {
            user.setRole(resolvedRole);
        }

        return toResponse(userRepository.save(user));
    }

    public UserResponse getCurrentUser(FirebaseUserPrincipal principal) {
        User user = getRequiredUser(principal);
        return toResponse(user);
    }

    public List<SavedPassengerResponse> getSavedPassengers(FirebaseUserPrincipal principal) {
        User user = getRequiredUser(principal);
        return savedPassengerRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .sorted(Comparator.comparing(SavedPassenger::getCreatedAt).reversed())
                .map(this::toSavedPassengerResponse)
                .toList();
    }

    @Transactional
    public SavedPassengerResponse savePassenger(FirebaseUserPrincipal principal, SavedPassengerRequest request) {
        User user = getRequiredUser(principal);

        SavedPassenger passenger = new SavedPassenger();
        passenger.setUser(user);
        passenger.setName(request.getName().trim());
        passenger.setAge(request.getAge());
        passenger.setGender(request.getGender().trim());

        return toSavedPassengerResponse(savedPassengerRepository.save(passenger));
    }

    @Transactional
    public void deleteSavedPassenger(FirebaseUserPrincipal principal, Long passengerId) {
        User user = getRequiredUser(principal);
        SavedPassenger passenger = savedPassengerRepository.findByIdAndUserId(passengerId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved passenger not found"));
        savedPassengerRepository.delete(passenger);
    }

    private Role resolveRole(String email) {
        return adminEmails.contains(email.toLowerCase()) ? Role.ADMIN : Role.USER;
    }

    private boolean isVelTechEmail(String email) {
        return email != null && email.trim().toLowerCase().endsWith(VELTECH_EMAIL_DOMAIN);
    }

    private User getRequiredUser(FirebaseUserPrincipal principal) {
        return userRepository.findByFirebaseUid(principal.getUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not been synced yet"));
    }

    private String resolveName(User user, FirebaseUserPrincipal principal) {
        if (principal.getDisplayName() != null && !principal.getDisplayName().isBlank()) {
            return principal.getDisplayName();
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return principal.getEmail().split("@")[0];
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirebaseUid(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getDepartment(),
                user.getCreatedAt(),
                isProfileComplete(user));
    }

    private SavedPassengerResponse toSavedPassengerResponse(SavedPassenger passenger) {
        return new SavedPassengerResponse(
                passenger.getId(),
                passenger.getName(),
                passenger.getAge(),
                passenger.getGender(),
                passenger.getCreatedAt(),
                passenger.getUpdatedAt());
    }

    private boolean isProfileComplete(User user) {
        return user.getDepartment() != null
                && !user.getDepartment().isBlank()
                && !user.getDepartment().equalsIgnoreCase("Unassigned");
    }
}
