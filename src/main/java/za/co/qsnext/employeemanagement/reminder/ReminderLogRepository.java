package za.co.qsnext.employeemanagement.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, String> {
}
