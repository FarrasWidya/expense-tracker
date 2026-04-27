package com.faras.expense_tracker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SharedExpenseRepository extends JpaRepository<SharedExpense, UUID> {
    Page<SharedExpense> findByRumahOrderByCreatedAtDesc(Rumah rumah, Pageable pageable);
    void deleteByRumah(Rumah rumah);

    @Query("SELECT s.createdBy, SUM(s.amount) FROM SharedExpense s " +
           "WHERE s.rumah = :rumah AND s.date BETWEEN :start AND :end " +
           "GROUP BY s.createdBy")
    List<Object[]> sumByUserForPeriod(@Param("rumah") Rumah rumah,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}
