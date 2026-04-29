package com.faras.expense_tracker.service;

import com.faras.expense_tracker.entity.Expense;
import com.faras.expense_tracker.entity.User;
import com.faras.expense_tracker.repository.ExpenseRepository;
import com.faras.expense_tracker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public record SummaryResponse(double totalExpense, double totalIncome, double net, String month) {}

    public Expense add(UUID userId, Double amount, String category, String note, LocalDate date, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setNote(note);
        expense.setDate(date != null ? date : LocalDate.now());
        expense.setType("INCOME".equals(type) ? "INCOME" : "EXPENSE");
        return repository.save(expense);
    }

    public List<Expense> getAll(UUID userId, String category, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDateRange = startDate != null && endDate != null;
        if (hasCategory && hasDateRange) return repository.findByUserAndCategoryAndDateBetween(user, category, startDate, endDate);
        if (hasCategory) return repository.findByUserAndCategory(user, category);
        if (hasDateRange) return repository.findByUserAndDateBetween(user, startDate, endDate);
        return repository.findByUser(user);
    }

    public void delete(UUID userId, UUID id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Expense expense = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(expense);
    }

    @Transactional
    public Expense update(UUID userId, UUID id, Double amount, String category, String note, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Expense expense = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setNote(note);
        expense.setDate(date != null ? date : expense.getDate());
        return repository.save(expense);
    }

    @Transactional
    public void batchDelete(UUID userId, List<UUID> ids) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<Expense> expenses = repository.findAllByIdInAndUser(ids, user);
        repository.deleteAll(expenses);
    }

    @Transactional
    public List<Expense> batchUpdateCategory(UUID userId, List<UUID> ids, String category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<Expense> expenses = repository.findAllByIdInAndUser(ids, user);
        expenses.forEach(e -> e.setCategory(category));
        return repository.saveAll(expenses);
    }

    public SummaryResponse getSummary(UUID userId, String month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        YearMonth ym = YearMonth.parse(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<Expense> all = repository.findByUserAndDateBetween(user, from, to);
        double expense = all.stream().filter(e -> !"INCOME".equals(e.getType())).mapToDouble(Expense::getAmount).sum();
        double income = all.stream().filter(e -> "INCOME".equals(e.getType())).mapToDouble(Expense::getAmount).sum();
        return new SummaryResponse(expense, income, income - expense, month);
    }

    public byte[] buildCsvBytes(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = getAll(userId, null, startDate, endDate);
        StringBuilder sb = new StringBuilder("Tanggal,Kategori,Catatan,Jumlah,Tipe\n");
        for (Expense e : expenses) {
            sb.append(e.getDate()).append(",")
              .append(csvEscape(e.getCategory())).append(",")
              .append(csvEscape(e.getNote())).append(",")
              .append(e.getAmount()).append(",")
              .append(e.getType()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] buildJsonBytes(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = getAll(userId, null, startDate, endDate);
        try {
            return new ObjectMapper().findAndRegisterModules().writeValueAsBytes(expenses);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] buildExcelBytes(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = getAll(userId, null, startDate, endDate);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Pengeluaran");
            String[] headers = {"Tanggal", "Kategori", "Catatan", "Jumlah", "Tipe"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
            int rowNum = 1;
            for (Expense e : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getDate().toString());
                row.createCell(1).setCellValue(e.getCategory());
                row.createCell(2).setCellValue(e.getNote() != null ? e.getNote() : "");
                row.createCell(3).setCellValue(e.getAmount());
                row.createCell(4).setCellValue(e.getType());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
