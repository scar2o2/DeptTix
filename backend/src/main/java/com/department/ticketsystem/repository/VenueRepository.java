package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    boolean existsByNameIgnoreCase(String name);
}
