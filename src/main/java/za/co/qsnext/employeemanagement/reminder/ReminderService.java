package za.co.qsnext.employeemanagement.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.compliance.ComplianceRecord;
import za.co.qsnext.employeemanagement.compliance.ComplianceRecordRepository;
import za.co.qsnext.employeemanagement.compliance.ComplianceRequirementRepository;
import za.co.qsnext.employeemanagement.document.Document;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.expense.ExpenseClaim;
import za.co.qsnext.employeemanagement.expense.ExpenseClaimRepository;
import za.co.qsnext.employeemanagement.leave.LeaveRequest;
import za.co.qsnext.employeemanagement.leave.LeaveRequestRepository;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.timesheet.Timesheet;
import za.co.qsnext.employeemanagement.timesheet.TimesheetRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily nudges so nothing slips through:
 * <ul>
 *   <li>an employee whose document or compliance item expires within
 *       {@value #EXPIRY_WINDOW_DAYS} days;</li>
 *   <li>a manager whose report's leave request, timesheet or expense claim
 *       has waited more than {@value #APPROVAL_WAIT_DAYS} days.</li>
 * </ul>
 * Each reminder is logged by key and sent once.
 */
@Service
public class ReminderService {

    static final int EXPIRY_WINDOW_DAYS = 30;
    static final int APPROVAL_WAIT_DAYS = 2;

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderLogRepository reminderLogRepository;
    private final DocumentRepository documentRepository;
    private final ComplianceRecordRepository complianceRecordRepository;
    private final ComplianceRequirementRepository complianceRequirementRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TimesheetRepository timesheetRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;

    public ReminderService(
            ReminderLogRepository reminderLogRepository,
            DocumentRepository documentRepository,
            ComplianceRecordRepository complianceRecordRepository,
            ComplianceRequirementRepository complianceRequirementRepository,
            LeaveRequestRepository leaveRequestRepository,
            TimesheetRepository timesheetRepository,
            ExpenseClaimRepository expenseClaimRepository,
            EmployeeRepository employeeRepository,
            NotificationPublisher notificationPublisher
    ) {
        this.reminderLogRepository = reminderLogRepository;
        this.documentRepository = documentRepository;
        this.complianceRecordRepository = complianceRecordRepository;
        this.complianceRequirementRepository = complianceRequirementRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.timesheetRepository = timesheetRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.employeeRepository = employeeRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional
    @Scheduled(cron = "${reminders.cron:0 0 6 * * *}")
    public void sendDailyReminders() {
        int sent = sendReminders(OffsetDateTime.now());
        log.info("Daily reminders sent: {}", sent);
    }

    @Transactional
    public int sendReminders(OffsetDateTime now) {

        int sent = 0;
        LocalDate today = now.toLocalDate();

        for (Document document : documentRepository.findByStatusAndExpiryDateBetween(
                Document.STATUS_ACTIVE, today, today.plusDays(EXPIRY_WINDOW_DAYS))) {
            sent += remindEmployee(
                    "document-expiry:" + document.getId() + ":" + document.getExpiryDate(),
                    document.getEmployeeId(),
                    "Document expiring soon",
                    "\"" + document.getTitle() + "\" expires on " + document.getExpiryDate() + ". Please upload a new version."
            );
        }

        for (ComplianceRecord record : complianceRecordRepository.findByStatusAndExpiresAtBetween(
                ComplianceRecord.STATUS_COMPLETED, now, now.plusDays(EXPIRY_WINDOW_DAYS))) {
            String name = complianceRequirementRepository.findById(record.getRequirementId())
                    .map(requirement -> requirement.getName())
                    .orElse("A compliance requirement");
            sent += remindEmployee(
                    "compliance-expiry:" + record.getId() + ":" + record.getExpiresAt().toLocalDate(),
                    record.getEmployeeId(),
                    "Compliance item expiring soon",
                    name + " expires on " + record.getExpiresAt().toLocalDate() + ". Please renew it."
            );
        }

        OffsetDateTime overdue = now.minusDays(APPROVAL_WAIT_DAYS);

        for (LeaveRequest request : leaveRequestRepository.findByStatus("PENDING", Pageable.unpaged())) {
            if (request.getCreatedAt() != null && request.getCreatedAt().isBefore(overdue)) {
                sent += remindManager("leave-approval:" + request.getId(), request.getEmployeeId(),
                        "Leave request waiting", "a leave request");
            }
        }

        for (Timesheet timesheet : timesheetRepository.findByStatus("SUBMITTED", Pageable.unpaged())) {
            if (timesheet.getSubmittedAt() != null && timesheet.getSubmittedAt().isBefore(overdue)) {
                sent += remindManager("timesheet-approval:" + timesheet.getId(), timesheet.getEmployeeId(),
                        "Timesheet waiting", "a timesheet");
            }
        }

        for (ExpenseClaim claim : expenseClaimRepository.findByStatus(ExpenseClaim.STATUS_SUBMITTED, Pageable.unpaged())) {
            if (claim.getSubmittedAt() != null && claim.getSubmittedAt().isBefore(overdue)) {
                sent += remindManager("expense-approval:" + claim.getId(), claim.getEmployeeId(),
                        "Expense claim waiting", "an expense claim");
            }
        }

        return sent;
    }

    private int remindEmployee(String key, UUID employeeId, String title, String message) {

        Optional<UUID> userId = employeeRepository.findById(employeeId).map(Employee::getUserId);

        if (userId.isEmpty() || reminderLogRepository.existsById(key)) {
            return 0;
        }

        notificationPublisher.publish(userId.get(), NotificationType.REMINDER, title, message);
        reminderLogRepository.save(new ReminderLog(key));
        return 1;
    }

    private int remindManager(String key, UUID employeeId, String title, String what) {

        Optional<Employee> employee = employeeRepository.findById(employeeId);

        Optional<UUID> managerUserId = employee
                .map(Employee::getManagerId)
                .flatMap(employeeRepository::findById)
                .map(Employee::getUserId);

        if (managerUserId.isEmpty() || reminderLogRepository.existsById(key)) {
            return 0;
        }

        String name = employee.get().getFirstName() + " " + employee.get().getLastName();

        notificationPublisher.publish(
                managerUserId.get(),
                NotificationType.REMINDER,
                title,
                name + " has had " + what + " waiting for your approval for more than "
                        + APPROVAL_WAIT_DAYS + " days."
        );
        reminderLogRepository.save(new ReminderLog(key));
        return 1;
    }
}
