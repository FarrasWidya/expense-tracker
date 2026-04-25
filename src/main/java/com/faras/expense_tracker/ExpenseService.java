package com.faras.expense_tracker;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public Expense add(Double amount, String category, String note, LocalDate date) {
        Expense expense = new Expense(null, amount, category, note,
                date != null ? date : LocalDate.now());
        return repository.save(expense);
    }

    public List<Expense> getAll(String category, LocalDate startDate, LocalDate endDate) {
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDateRange = startDate != null && endDate != null;
        if (hasCategory && hasDateRange) return repository.findByCategoryAndDateBetween(category, startDate, endDate);
        if (hasCategory) return repository.findByCategory(category);
        if (hasDateRange) return repository.findByDateBetween(startDate, endDate);
        return repository.findAll();
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

    public Expense update(Long id, Double amount, String category, String note, LocalDate date) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setNote(note);
        expense.setDate(date != null ? date : expense.getDate());
        return repository.save(expense);
    }
}
