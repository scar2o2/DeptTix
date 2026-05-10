package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.SavedPassenger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPassengerRepository extends JpaRepository<SavedPassenger, Long> {
    List<SavedPassenger> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedPassenger> findByIdAndUserId(Long id, Long userId);
}
