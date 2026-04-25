package com.faras.expense_tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
        Expense request = new Expense(null, 50000.0, "food", "lunch");

        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(50000.0))
                .andExpect(jsonPath("$.category").value("food"))
                .andExpect(jsonPath("$.note").value("lunch"));
    }

    @Test
    void getAllExpenses_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteExpense_returnsNoContent() throws Exception {
        Expense created = new Expense(null, 20000.0, "Transport", "Grab");
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
        Expense created = new Expense(null, 10000.0, "Food", "Snack");
        String body = mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(created)))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        Expense update = new Expense(null, 25000.0, "Bills", "Internet");
        mockMvc.perform(put("/expenses/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(25000.0))
                .andExpect(jsonPath("$.category").value("Bills"))
                .andExpect(jsonPath("$.note").value("Internet"));
    }
}
