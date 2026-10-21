package com.example.simactivation;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ActivationControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ActivationRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearRecords() {
        repository.deleteAll();
    }

    @Test
    void createsSearchesAndSummarizesARecord() throws Exception {
        mvc.perform(post("/api/activations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Asha Rao","msisdn":"9876543210","iccid":"89012345678901234567"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        mvc.perform(get("/api/activations").param("query", "asha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName", is("Asha Rao")));
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.DRAFT", is(1)))
                .andExpect(jsonPath("$.REJECTED", is(0)));
    }

    @Test
    void enforcesRoleAndStatusTransitionRules() throws Exception {
        long id = createRecord("Test User", "9876543211", "89012345678901234568");

        mvc.perform(post("/api/activations/" + id + "/status")
                        .header("X-Actor-Role", "SUPERVISOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/activations/" + id + "/status")
                        .header("X-Actor-Role", "AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));
    }

    @Test
    void rejectsMalformedNumbersAndPreventsEditingSubmittedRecords() throws Exception {
        mvc.perform(post("/api/activations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Bad User","msisdn":"abc","iccid":"not-an-iccid"}
                                """))
                .andExpect(status().isBadRequest());

        long id = createRecord("Locked User", "9876543212", "89012345678901234569");
        mvc.perform(post("/api/activations/" + id + "/status")
                        .header("X-Actor-Role", "AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/activations/" + id)
                        .header("X-Actor-Role", "AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Changed User","msisdn":"9876543213","iccid":"89012345678901234570"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void supportsSupervisorRejectionAndAdministratorApproval() throws Exception {
        long rejectedId = createRecord("Rejected User", "9876543214", "89012345678901234571");
        moveToReview(rejectedId);
        mvc.perform(post("/api/activations/" + rejectedId + "/status")
                        .header("X-Actor-Role", "SUPERVISOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        long approvedId = createRecord("Approved User", "9876543215", "89012345678901234572");
        moveToReview(approvedId);
        mvc.perform(post("/api/activations/" + approvedId + "/status")
                        .header("X-Actor-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVATED")));

        mvc.perform(get("/api/activations")
                        .param("query", "Approved")
                        .param("status", "ACTIVATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    private void moveToReview(long id) throws Exception {
        mvc.perform(post("/api/activations/" + id + "/status")
                        .header("X-Actor-Role", "AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/activations/" + id + "/status")
                        .header("X-Actor-Role", "SUPERVISOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_REVIEW\"}"))
                .andExpect(status().isOk());
    }

    private long createRecord(String name, String msisdn, String iccid) throws Exception {
        ResultActions result = mvc.perform(post("/api/activations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "customerName", name, "msisdn", msisdn, "iccid", iccid))));
        MvcResult response = result.andExpect(status().isCreated()).andReturn();
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
