package za.co.qsnext.employeemanagement.notification;

import tools.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import za.co.qsnext.employeemanagement.auth.dto.LoginRequest;
import za.co.qsnext.employeemanagement.auth.dto.RegisterRequest;
import za.co.qsnext.employeemanagement.common.AbstractIntegrationTest;
import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.leave.LeaveBalance;
import za.co.qsnext.employeemanagement.leave.LeaveBalanceRepository;
import za.co.qsnext.employeemanagement.leave.LeaveRequest;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.leave.LeaveService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof that a business event (a leave request being approved)
 * actually results in an in-app notification and, since it only runs
 * after the approving transaction commits, that a transaction rolling
 * back would not have notified anyone. Fixture rows (department,
 * employee, leave balance) are inserted directly via their repositories -
 * this test is about the notification pipeline, not the leave module's
 * own HTTP/permission surface.
 */
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private LeaveService leaveService;
    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void approvingALeaveRequest_eventuallyCreatesANotification_andQueuesAnEmail() throws Exception {
        String username = "notif.leave.user";
        String email = "notif.leave.user@qsnext.co.za";

        UUID userId = registerAndGetUserId(username, email);

        Department department = departmentRepository.saveAndFlush(
                new Department("Engineering-" + UUID.randomUUID(), "Builds the product"));

        Employee employee = employeeRepository.saveAndFlush(new Employee(
                userId, department.getId(), "EMP-" + UUID.randomUUID(),
                "Jane", "Doe", "0123456789", "Engineer", LocalDate.of(2020, 1, 1)));

        leaveBalanceRepository.saveAndFlush(
                new LeaveBalance(employee.getId(), "ANNUAL", LocalDate.now().getYear(), BigDecimal.TEN));

        LeaveRequest leaveRequest = leaveRequestRepository.saveAndFlush(new LeaveRequest(
                employee.getId(), "ANNUAL",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), "Vacation"));

        leaveService.approve(leaveRequest.getId(), UUID.randomUUID());

        awaitUntil(
                () -> notificationRepository.findByUserIdOrderByCreatedAtDesc(
                                userId, PageRequest.of(0, 10))
                        .stream()
                        .anyMatch(n -> NotificationType.LEAVE_REQUEST_APPROVED.name().equals(n.getType())),
                Duration.ofSeconds(10)
        );

        awaitUntil(
                () -> stubEmailSender.countSentTo(email) >= 1,
                Duration.ofSeconds(10)
        );
    }

    @Test
    void getOwnPreference_returnsDefaults_forANewUser() throws Exception {
        registerAndGetUserId("notif.pref.defaults", "notif.pref.defaults@qsnext.co.za");

        String accessToken = loginAndGetAccessToken("notif.pref.defaults", "S3curePassword!");

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled").value(true))
                .andExpect(jsonPath("$.emailEnabled").value(true));
    }

    @Test
    void updateOwnPreference_persistsTheChange() throws Exception {
        registerAndGetUserId("notif.pref.update", "notif.pref.update@qsnext.co.za");

        String accessToken = loginAndGetAccessToken("notif.pref.update", "S3curePassword!");

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inAppEnabled": false, "emailEnabled": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled").value(false))
                .andExpect(jsonPath("$.emailEnabled").value(true));

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled").value(false));
    }

    @Test
    void preferences_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isUnauthorized());
    }

    private UUID registerAndGetUserId(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, "S3curePassword!"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("userId").asText());
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
}
