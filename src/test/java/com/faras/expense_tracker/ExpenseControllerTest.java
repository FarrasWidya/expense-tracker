package com.faras.expense_tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addExpense_returnsCreatedExpenseWithId() throws Exception {
        Expense request = new Expense(null, 50000.0, "food", "lunch", LocalDate.of(2026, 4, 25));

        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(50000.0))
                .andExpect(jsonPath("$.category").value("food"))
                .andExpect(jsonPath("$.note").value("lunch"))
                .andExpect(jsonPath("$.date").value("2026-04-25"));
    }

    @Test
    void addExpense_withoutDate_defaultsToToday() throws Exception {
        Expense request = new Expense(null, 15000.0, "Food", "Coffee", null);

        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()));
    }

    @Test
    void getAllExpenses_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteExpense_returnsNoContent() throws Exception {
        Expense created = new Expense(null, 20000.0, "Transport", "Grab", LocalDate.now());
        String body = mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/expenses/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/expenses"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void deleteExpense_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/expenses/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateExpense_returnsUpdatedExpense() throws Exception {
        Expense created = new Expense(null, 10000.0, "Food", "Snack", LocalDate.now());
        String body = mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        Expense update = new Expense(null, 25000.0, "Bills", "Internet", LocalDate.of(2026, 4, 1));
        mockMvc.perform(put("/expenses/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(25000.0))
                .andExpect(jsonPath("$.category").value("Bills"))
                .andExpect(jsonPath("$.note").value("Internet"))
                .andExpect(jsonPath("$.date").value("2026-04-01"));
    }

    @Test
    void filterByCategory_returnsOnlyMatchingExpenses() throws Exception {
        Expense e = new Expense(null, 5000.0, "Health", "Vitamin", LocalDate.now());
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(e)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/expenses").param("category", "Health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].category", everyItem(is("Health"))));
    }

    @Test
    void filterByDateRange_returnsOnlyExpensesWithinRange() throws Exception {
        Expense e = new Expense(null, 30000.0, "Entertainment", "Concert", LocalDate.of(2025, 6, 15));
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(e)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/expenses")
                .param("startDate", "2025-06-01")
                .param("endDate", "2025-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].date", everyItem(is("2025-06-15"))));
    }
}
