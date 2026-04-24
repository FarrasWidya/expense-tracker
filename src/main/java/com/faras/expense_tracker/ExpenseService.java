package com.faras.expense_tracker;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExpenseService {
    private final List<Expense> expenses = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Expense add(Double amount, String category, String note) {
        Expense expense = new Expense(idCounter.getAndIncrement(), amount, category, note);
        expenses.add(expense);
        return expense;
    }

    public List<Expense> getAll() {
        return expenses;
    }
}
