package com.department.ticketsystem.model;

import java.util.Arrays;
import java.util.List;

public enum Department {
    ALL,
    CSE,
    ECE,
    EEE,
    CIVIL,
    MECH,
    IT;

    public static Department fromValue(String value) {
        return Arrays.stream(values())
                .filter(department -> department.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid department selection"));
    }

    public static Department fromUserValue(String value) {
        Department department = fromValue(value);
        if (department == ALL) {
            throw new IllegalArgumentException("Users cannot be assigned to ALL department");
        }
        return department;
    }

    public static List<String> userDepartments() {
        return Arrays.stream(values())
                .filter(department -> department != ALL)
                .map(Enum::name)
                .toList();
    }
}
