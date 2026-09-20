package za.co.qsnext.employeemanagement.payroll;

import tools.jackson.databind.JsonNode;

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
 * End-to-end coverage for the Phase 10 Payroll module: a configured tax
 * bracket, an employee payroll profile, and a full pay-period run from
 * DRAFT through a manual bonus line item, approval and payment, ending
 * with the employee viewing their own payslip - all via real HTTP + the
 * real security chain.
 */
class PayrollIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD = "S3curePassword!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void payrollModule_runsAFullPayPeriodEndToEnd() throws Exception {
        UUID adminUserId = registerUser("hr10.admin", "hr10.admin@qsnext.co.za");
        promoteToAdmin("hr10.admin");
        String adminToken = loginAndGetAccessToken("hr10.admin", PASSWORD);

        UUID employeeUserId = registerUser("hr10.employee", "hr10.employee@qsnext.co.za");
        String employeeToken = loginAndGetAccessToken("hr10.employee", PASSWORD);

        UUID departmentId = createDepartment(adminToken, "Payroll Dept");
        UUID employeeId = createEmployeeProfile(adminToken, employeeUserId, departmentId);

        // An employee cannot create a pay period - that needs PAYROLL_MANAGE.
        mockMvc.perform(post("/api/v1/payroll/pay-periods")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized","startDate":"2026-01-01","endDate":"2026-01-31","payDate":"2026-02-05"}
                                """))
                .andExpect(status().isForbidden());

        UUID taxConfigId = createTaxConfiguration(adminToken);
        createTaxBracket(adminToken, taxConfigId);

        mockMvc.perform(put("/api/v1/payroll/config/employees/{id}/profile", employeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseSalary":50000,"payFrequency":"MONTHLY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        UUID payPeriodId = createPayPeriod(adminToken);

        MvcResult runResult = mockMvc.perform(post("/api/v1/payroll/runs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("payPeriodId", payPeriodId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        JsonNode runBody = objectMapper.readTree(runResult.getResponse().getContentAsString());
        UUID runId = UUID.fromString(runBody.get("id").asText());

        MvcResult entriesResult = mockMvc.perform(get("/api/v1/payroll/runs/{id}/entries", runId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalEarnings").value(50000))
                .andReturn();

        JsonNode entriesBody = objectMapper.readTree(entriesResult.getResponse().getContentAsString());
        UUID entryId = UUID.fromString(entriesBody.get(0).get("id").asText());

        mockMvc.perform(post("/api/v1/payroll/entries/{id}/line-items", entryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EARNING","code":"BONUS","description":"Performance bonus","amount":2000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalEarnings").value(52000));

        mockMvc.perform(patch("/api/v1/payroll/runs/{id}/approve", runId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(patch("/api/v1/payroll/runs/{id}/pay", runId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // The employee can view their own payslip, showing the tax deduction and net pay.
        mockMvc.perform(get("/api/v1/payroll/my-payslips")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entryId.toString()));

        // Tax was computed at run-creation time from base salary alone (50000 * 10%);
        // it is not retroactively recomputed when the bonus line item is added afterward.
        mockMvc.perform(get("/api/v1/payroll/entries/{id}/payslip", entryId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entry.totalDeductions").value(5000.0))
                .andExpect(jsonPath("$.lineItems.length()").value(3));

        // An employee cannot view another employee's payroll profile.
        UUID otherEmployeeUserId = registerUser("hr10.other", "hr10.other@qsnext.co.za");
        UUID otherEmployeeId = createEmployeeProfile(adminToken, otherEmployeeUserId, departmentId);

        mockMvc.perform(get("/api/v1/payroll/config/employees/{id}/profile", otherEmployeeId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
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
                                userId, departmentId, "EMP-HR10-" + userId.toString().substring(0, 8),
                                "Jane", "Doe", "0123456789", "Engineer", LocalDate.of(2020, 1, 1)))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createTaxConfiguration(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payroll/config/tax-configurations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PAYE HR10","description":"Illustrative only","lineItemType":"DEDUCTION"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void createTaxBracket(String adminToken, UUID taxConfigId) throws Exception {
        mockMvc.perform(post("/api/v1/payroll/config/tax-configurations/{id}/brackets", taxConfigId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minAmount":0,"ratePercent":10,"effectiveFrom":"2026-01-01"}
                                """))
                .andExpect(status().isCreated());
    }

    private UUID createPayPeriod(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payroll/pay-periods")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"2026-01 HR10","startDate":"2026-01-01","endDate":"2026-01-31","payDate":"2026-02-05"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
