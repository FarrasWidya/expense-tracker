package com.faras.expense_tracker;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RumahMemberRepository extends JpaRepository<RumahMember, UUID> {
    Optional<RumahMember> findByUserId(Long userId);
    List<RumahMember> findByRumah(Rumah rumah);
    boolean existsByRumahAndUserId(Rumah rumah, Long userId);
    int countByRumah(Rumah rumah);
    void deleteByRumahAndUserId(Rumah rumah, Long userId);
    void deleteByRumah(Rumah rumah);
}
