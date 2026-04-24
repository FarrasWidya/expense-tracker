package com.faras.expense_tracker;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense addExpense(@RequestBody Expense expense) {
        return expenseService.add(expense.getAmount(), expense.getCategory(), expense.getNote());
    }

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseService.getAll();
    }
}
