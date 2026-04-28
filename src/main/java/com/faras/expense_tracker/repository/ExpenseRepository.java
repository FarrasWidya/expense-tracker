package com.faras.expense_tracker.repository;

import com.faras.expense_tracker.entity.Expense;
import com.faras.expense_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);
    List<Expense> findByUserAndCategory(User user, String category);
    List<Expense> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Expense> findByUserAndCategoryAndDateBetween(User user, String category, LocalDate startDate, LocalDate endDate);
    Optional<Expense> findByIdAndUser(Long id, User user);
    List<Expense> findAllByIdInAndUser(List<Long> ids, User user);
}
