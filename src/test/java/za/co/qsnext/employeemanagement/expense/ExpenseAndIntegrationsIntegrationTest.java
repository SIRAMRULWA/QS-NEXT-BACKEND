package za.co.qsnext.employeemanagement.expense;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.department.dto.CreateDepartmentRequest;
import za.co.qsnext.employeemanagement.employee.dto.CreateEmployeeRequest;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the Phase 8 modules (Expense Management,
 * Integrations): an employee drafts, submits and gets an expense claim
 * approved and reimbursed by HR, and an admin reconfigures the PAYROLL
 * integration point - all via real HTTP + the real security chain.
 */
class ExpenseAndIntegrationsIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void expenseAndIntegrationsModules_workTogetherEndToEnd() throws Exception {
        UUID adminUserId = registerUser("hr8.admin", "hr8.admin@qsnext.co.za");
        promoteToAdmin("hr8.admin");
        String adminToken = loginAndGetAccessToken("hr8.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr8.employee", "hr8.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr8.employee", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "Expense Dept");
        UUID employeeId = createEmployeeProfile(adminToken, employeeUserId, departmentId);

        UUID categoryId = createExpenseCategory(adminToken);

        // Employee drafts and submits their own claim.
        UUID claimId = createExpenseClaim(employeeToken, employeeId, categoryId);

        mockMvc.perform(patch("/api/v1/expenses/claims/{id}/submit", claimId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // An employee cannot approve their own claim - that needs EXPENSE_MANAGE.
        mockMvc.perform(patch("/api/v1/expenses/claims/{id}/approve", claimId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // HR approves it, then marks it reimbursed once payment has gone out.
        mockMvc.perform(patch("/api/v1/expenses/claims/{id}/approve", claimId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(patch("/api/v1/expenses/claims/{id}/reimburse", claimId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REIMBURSED"));

        mockMvc.perform(get("/api/v1/expenses/my-claims")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REIMBURSED"));

        // Integrations: an employee cannot see the integration catalog at all.
        mockMvc.perform(get("/api/v1/integrations")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // An admin can inspect and reconfigure the PAYROLL integration point,
        // which starts seeded as disabled/"none" (no Payroll module exists yet).
        mockMvc.perform(get("/api/v1/integrations/PAYROLL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/v1/integrations/PAYROLL")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerName":"acme-payroll","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("acme-payroll"))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(put("/api/v1/integrations/PAYROLL/settings/base-url")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settingValue":"https://payroll.example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingValue").value("https://payroll.example.com"));
    }

    private UUID registerUser(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("userId").asText());
    }

    private void promoteToAdmin(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Role adminRole = roleRepository.findByName(ADMIN_ROLE).orElseThrow();
        user.assignRole(adminRole);
        userRepository.saveAndFlush(user);
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private UUID createDepartment(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateDepartmentRequest(name, "Description"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createEmployeeProfile(String adminToken, UUID userId, UUID departmentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEmployeeRequest(
                                userId, departmentId, "EMP-HR8-001", "Jane", "Doe",
                                "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createExpenseCategory(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/expenses/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Travel HR8","description":"Travel expenses"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createExpenseClaim(String employeeToken, UUID employeeId, UUID categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/expenses/employees/{employeeId}/claims", employeeId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","amount":250.00,"currency":"ZAR",
                                 "description":"Client visit taxi","expenseDate":"2026-01-15"}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
