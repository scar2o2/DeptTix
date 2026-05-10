package com.department.ticketsystem.security;

import java.security.Principal;

public class FirebaseUserPrincipal implements Principal {

    private final String uid;
    private final String email;
    private final String name;
    private final boolean emailVerified;

    public FirebaseUserPrincipal(String uid, String email, String name, boolean emailVerified) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.emailVerified = emailVerified;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return name;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public String getName() {
        return email;
    }
}
