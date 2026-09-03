package za.co.qsnext.employeemanagement.timesheet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTimesheetEntryRequest(

        @NotNull(message = "Work date is required")
        LocalDate workDate,

        @NotNull(message = "Hours worked is required")
        @DecimalMin(value = "0.00", message = "Hours worked cannot be negative")
        @DecimalMax(value = "24.00", message = "Hours worked cannot exceed 24")
        BigDecimal hoursWorked,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}