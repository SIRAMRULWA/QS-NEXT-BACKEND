package za.co.qsnext.employeemanagement.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
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

    Page<Attendance> findByEmployeeIdAndAttendanceDateBetween(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            select a.status as status, count(a) as recordCount
            from Attendance a
            where a.attendanceDate between :from and :to
            group by a.status
            """)
    List<AttendanceStatusCount> countGroupedByStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}