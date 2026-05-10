package com.department.ticketsystem.controller;

import com.department.ticketsystem.dto.SavedPassengerRequest;
import com.department.ticketsystem.dto.SavedPassengerResponse;
import com.department.ticketsystem.dto.UserResponse;
import com.department.ticketsystem.dto.UserSyncRequest;
import com.department.ticketsystem.security.FirebaseUserPrincipal;
import com.department.ticketsystem.service.AppUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserService appUserService;

    public UserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/sync")
    public UserResponse syncUser(Authentication authentication, @RequestBody(required = false) UserSyncRequest request) {
        return appUserService.syncUser((FirebaseUserPrincipal) authentication.getPrincipal(), request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        return appUserService.getCurrentUser((FirebaseUserPrincipal) authentication.getPrincipal());
    }

    @GetMapping("/me/passengers")
    public List<SavedPassengerResponse> getSavedPassengers(Authentication authentication) {
        return appUserService.getSavedPassengers((FirebaseUserPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/me/passengers")
    public SavedPassengerResponse savePassenger(Authentication authentication,
                                               @Valid @RequestBody SavedPassengerRequest request) {
        return appUserService.savePassenger((FirebaseUserPrincipal) authentication.getPrincipal(), request);
    }

    @DeleteMapping("/me/passengers/{passengerId}")
    public void deleteSavedPassenger(Authentication authentication, @PathVariable Long passengerId) {
        appUserService.deleteSavedPassenger((FirebaseUserPrincipal) authentication.getPrincipal(), passengerId);
    }
}
