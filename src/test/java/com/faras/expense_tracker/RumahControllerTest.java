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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RumahControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token1;
    private String token2;

    @BeforeEach
    void setUp() throws Exception {
        token1 = registerAndGetToken();
        token2 = registerAndGetToken();
    }

    private String registerAndGetToken() throws Exception {
        String email = UUID.randomUUID() + "@test.com";
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "pass123"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    /** Creates a Rumah with token as admin. Returns raw JSON response string. */
    private String createRumah(String token) throws Exception {
        return mockMvc.perform(authed(post("/rumah"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Test Rumah", "emoji", "🏠", "color", "#00897B"))))
                .andReturn().getResponse().getContentAsString();
    }

    // ── Rumah management ─────────────────────────────────────────────────────

    @Test
    void createRumah_returnsRumahWithInviteToken() throws Exception {
        mockMvc.perform(authed(post("/rumah"), token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Keluarga Test", "emoji", "🏠", "color", "#00897B"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Keluarga Test"))
                .andExpect(jsonPath("$.emoji").value("🏠"))
                .andExpect(jsonPath("$.inviteToken").isString())
                .andExpect(jsonPath("$.members", hasSize(1)));
    }

    @Test
    void getMyRumah_returnsEmptyWhenNotInRumah() throws Exception {
        mockMvc.perform(authed(get("/rumah/me"), token1))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void previewJoin_publicEndpoint_noAuthRequired() throws Exception {
        String rumahJson = createRumah(token1);
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();

        mockMvc.perform(get("/rumah/join/" + inviteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Rumah"))
                .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void joinRumah_addsUserAsMember() throws Exception {
        String rumahJson = createRumah(token1);
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();

        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)));
    }

    @Test
    void joinRumah_whenAlreadyMember_returns409() throws Exception {
        String rumahJson = createRumah(token1);
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();

        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), token1))
                .andExpect(status().isConflict());
    }

    @Test
    void joinRumah_whenRumahFull_returns409() throws Exception {
        String rumahJson = createRumah(token1);
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();

        // admin = 1 member; join 5 more to reach cap of 6
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(authed(post("/rumah/join/" + inviteToken), registerAndGetToken()))
                    .andExpect(status().isOk());
        }

        // 7th person should be rejected
        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), registerAndGetToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void joinRumah_withInvalidToken_returns404() throws Exception {
        mockMvc.perform(authed(post("/rumah/join/" + UUID.randomUUID()), token1))
                .andExpect(status().isNotFound());
    }

    @Test
    void leaveRumah_adminLeave_returns403() throws Exception {
        createRumah(token1);

        mockMvc.perform(authed(delete("/rumah/me/leave"), token1))
                .andExpect(status().isForbidden());
    }

    @Test
    void leaveRumah_memberLeave_removesFromRumah() throws Exception {
        String rumahJson = createRumah(token1);
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();
        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), token2)).andExpect(status().isOk());

        mockMvc.perform(authed(delete("/rumah/me/leave"), token2))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get("/rumah/me"), token2))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deleteRumah_byAdmin_succeeds() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();

        mockMvc.perform(authed(delete("/rumah/" + rumahId), token1))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get("/rumah/me"), token1))
                .andExpect(content().string(""));
    }

    @Test
    void deleteRumah_byNonAdmin_returns403() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();
        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), token2)).andExpect(status().isOk());

        mockMvc.perform(authed(delete("/rumah/" + rumahId), token2))
                .andExpect(status().isForbidden());
    }

    // ── Shared expenses ───────────────────────────────────────────────────────

    @Test
    void addSharedExpense_byMember_returns201() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();

        mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("amount", 50000, "category", "Makan & Minum",
                                        "note", "nasi goreng", "date", "2026-04-27"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50000.0))
                .andExpect(jsonPath("$.category").value("Makan & Minum"));
    }

    @Test
    void addSharedExpense_byNonMember_returns403() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();

        mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("amount", 10000, "category", "Other", "note", "hack"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFeed_returnsPaginatedExpensesNewestFirst() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();
        String body = objectMapper.writeValueAsString(
                Map.of("amount", 25000, "category", "Transportasi", "note", "gojek"));

        mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token1)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token1)
                .contentType(MediaType.APPLICATION_JSON).content(body));

        mockMvc.perform(authed(get("/rumah/" + rumahId + "/feed?page=0&size=10"), token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].amount").value(25000.0));
    }

    @Test
    void getFeed_byNonMember_returns403() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();

        mockMvc.perform(authed(get("/rumah/" + rumahId + "/feed"), token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSharedExpense_byOwner_returns204() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();

        String expJson = mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("amount", 10000, "category", "Other", "note", "test"))))
                .andReturn().getResponse().getContentAsString();
        String expId = objectMapper.readTree(expJson).get("id").asText();

        mockMvc.perform(authed(delete("/rumah/" + rumahId + "/expenses/" + expId), token1))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSharedExpense_byNonOwner_returns403() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();
        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), token2)).andExpect(status().isOk());

        String expJson = mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("amount", 10000, "category", "Other", "note", "test"))))
                .andReturn().getResponse().getContentAsString();
        String expId = objectMapper.readTree(expJson).get("id").asText();

        mockMvc.perform(authed(delete("/rumah/" + rumahId + "/expenses/" + expId), token2))
                .andExpect(status().isForbidden());
    }

    // ── Contribution ──────────────────────────────────────────────────────────

    @Test
    void getContribution_returnsCorrectPercentages() throws Exception {
        String rumahJson = createRumah(token1);
        String rumahId = objectMapper.readTree(rumahJson).get("id").asText();
        String inviteToken = objectMapper.readTree(rumahJson).get("inviteToken").asText();
        mockMvc.perform(authed(post("/rumah/join/" + inviteToken), token2)).andExpect(status().isOk());

        // token1: 3 × Rp100k = Rp300k (75%), token2: 1 × Rp100k (25%)
        String expBody = objectMapper.writeValueAsString(
                Map.of("amount", 100000, "category", "Makan & Minum", "note", "test"));
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token1)
                    .contentType(MediaType.APPLICATION_JSON).content(expBody));
        }
        mockMvc.perform(authed(post("/rumah/" + rumahId + "/expenses"), token2)
                .contentType(MediaType.APPLICATION_JSON).content(expBody));

        mockMvc.perform(authed(get("/rumah/" + rumahId + "/contribution"), token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pct").value(75))
                .andExpect(jsonPath("$[1].pct").value(25));
    }
}
