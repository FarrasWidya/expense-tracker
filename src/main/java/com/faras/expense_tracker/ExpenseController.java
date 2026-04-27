package com.faras.expense_tracker;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public record BatchDeleteRequest(List<Long> ids) {}
    public record BatchCategoryRequest(List<Long> ids, String category) {}

    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense addExpense(@RequestBody Expense expense, Authentication auth) {
        return expenseService.add(userId(auth), expense.getAmount(),
                expense.getCategory(), expense.getNote(), expense.getDate());
    }

    @GetMapping
    public List<Expense> getAllExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication auth) {
        return expenseService.getAll(userId(auth), category, startDate, endDate);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id, Authentication auth) {
        expenseService.delete(userId(auth), id);
    }

    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @RequestBody Expense expense, Authentication auth) {
        return expenseService.update(userId(auth), id, expense.getAmount(),
                expense.getCategory(), expense.getNote(), expense.getDate());
    }

    @DeleteMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchDelete(@RequestBody BatchDeleteRequest req, Authentication auth) {
        expenseService.batchDelete(userId(auth), req.ids());
    }

    @PatchMapping("/batch/category")
    public List<Expense> batchUpdateCategory(@RequestBody BatchCategoryRequest req, Authentication auth) {
        return expenseService.batchUpdateCategory(userId(auth), req.ids(), req.category());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication auth) {
        Long uid = userId(auth);
        return switch (format.toLowerCase()) {
            case "json" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses.json\"")
                    .body(expenseService.buildJsonBytes(uid, startDate, endDate));
            case "excel" -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses.xlsx\"")
                    .body(expenseService.buildExcelBytes(uid, startDate, endDate));
            default -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses.csv\"")
                    .body(expenseService.buildCsvBytes(uid, startDate, endDate));
        };
    }
}
