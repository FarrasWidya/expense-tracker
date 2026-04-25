package com.faras.expense_tracker;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public Expense add(Double amount, String category, String note) {
        Expense expense = new Expense(null, amount, category, note);
        return repository.save(expense);
    }

    public List<Expense> getAll() {
        return repository.findAll();
    }
}
