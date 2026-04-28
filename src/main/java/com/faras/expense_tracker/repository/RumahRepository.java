package com.faras.expense_tracker.repository;

import com.faras.expense_tracker.entity.Rumah;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RumahRepository extends JpaRepository<Rumah, UUID> {
    Optional<Rumah> findByInviteToken(UUID inviteToken);
}
