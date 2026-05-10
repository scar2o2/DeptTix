package com.department.ticketsystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TicketHolderDetails {

    @NotBlank(message = "Ticket holder name is required")
    private String name;

    @Min(value = 1, message = "Ticket holder age must be at least 1")
    @Max(value = 120, message = "Ticket holder age must be at most 120")
    private Integer age;

    @NotBlank(message = "Ticket holder gender is required")
    private String gender;

    public TicketHolderDetails() {
    }

    public TicketHolderDetails(String name, Integer age, String gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
