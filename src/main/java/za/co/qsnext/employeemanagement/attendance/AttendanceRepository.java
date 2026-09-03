package za.co.qsnext.employeemanagement.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository
        extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(
            UUID employeeId,
            LocalDate attendanceDate
    );

    Page<Attendance> findByEmployeeId(
            UUID employeeId,
            Pageable pageable
    );

    Page<Attendance> findByAttendanceDate(
            LocalDate attendanceDate,
            Pageable pageable
    );
}