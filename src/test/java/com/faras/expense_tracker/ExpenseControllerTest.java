package com.faras.expense_tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String email = UUID.randomUUID() + "@test.com";
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "pass123"))))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("token").asText();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    @Test
    void addExpense_returnsCreatedExpenseWithId() throws Exception {
        String body = "{\"amount\":50000,\"category\":\"Food\",\"note\":\"lunch\",\"date\":\"2026-04-25\"}";
        mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(50000.0))
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.note").value("lunch"))
                .andExpect(jsonPath("$.date").value("2026-04-25"));
    }

    @Test
    void addExpense_withoutDate_defaultsToToday() throws Exception {
        String body = "{\"amount\":15000,\"category\":\"Food\",\"note\":\"Coffee\"}";
        mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()));
    }

    @Test
    void getAllExpenses_returnsJsonArray() throws Exception {
        mockMvc.perform(authed(get("/expenses")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void expenses_notVisibleToOtherUser() throws Exception {
        String body = "{\"amount\":99999,\"category\":\"Other\",\"note\":\"private\"}";
        mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String email2 = UUID.randomUUID() + "@test.com";
        String resp2 = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email2, "password", "pass456"))))
                .andReturn().getResponse().getContentAsString();
        String token2 = objectMapper.readTree(resp2).get("token").asText();

        mockMvc.perform(get("/expenses").header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteExpense_returnsNoContent() throws Exception {
        String body = "{\"amount\":20000,\"category\":\"Transport\",\"note\":\"Grab\"}";
        String created = mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(authed(delete("/expenses/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get("/expenses")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void deleteExpense_notFound_returns404() throws Exception {
        mockMvc.perform(authed(delete("/expenses/99999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExpense_otherUsersExpense_returns404() throws Exception {
        String body = "{\"amount\":5000,\"category\":\"Food\",\"note\":\"mine\"}";
        String created = mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        String email2 = UUID.randomUUID() + "@test.com";
        String resp2 = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email2, "password", "pass"))))
                .andReturn().getResponse().getContentAsString();
        String token2 = objectMapper.readTree(resp2).get("token").asText();

        mockMvc.perform(delete("/expenses/" + id).header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateExpense_returnsUpdatedExpense() throws Exception {
        String body = "{\"amount\":10000,\"category\":\"Food\",\"note\":\"Snack\"}";
        String created = mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        String update = "{\"amount\":25000,\"category\":\"Bills\",\"note\":\"Internet\",\"date\":\"2026-04-01\"}";
        mockMvc.perform(authed(put("/expenses/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(25000.0))
                .andExpect(jsonPath("$.category").value("Bills"))
                .andExpect(jsonPath("$.note").value("Internet"))
                .andExpect(jsonPath("$.date").value("2026-04-01"));
    }

    @Test
    void updateExpense_otherUsersExpense_returns404() throws Exception {
        String body = "{\"amount\":5000,\"category\":\"Food\",\"note\":\"mine\"}";
        String created = mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        String email2 = UUID.randomUUID() + "@test.com";
        String resp2 = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email2, "password", "pass"))))
                .andReturn().getResponse().getContentAsString();
        String token2 = objectMapper.readTree(resp2).get("token").asText();

        String update = "{\"amount\":99999,\"category\":\"Other\",\"note\":\"hacked\"}";
        mockMvc.perform(put("/expenses/" + id)
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterByCategory_returnsOnlyMatchingExpenses() throws Exception {
        String body = "{\"amount\":5000,\"category\":\"Health\",\"note\":\"Vitamin\"}";
        mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get("/expenses")).param("category", "Health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].category", everyItem(is("Health"))));
    }

    @Test
    void filterByDateRange_returnsOnlyExpensesWithinRange() throws Exception {
        String body = "{\"amount\":30000,\"category\":\"Entertainment\",\"note\":\"Concert\",\"date\":\"2025-06-15\"}";
        mockMvc.perform(authed(post("/expenses"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get("/expenses"))
                        .param("startDate", "2025-06-01")
                        .param("endDate", "2025-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].date", everyItem(is("2025-06-15"))));
    }

    @Test
    void expenses_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().isUnauthorized());
    }
}
