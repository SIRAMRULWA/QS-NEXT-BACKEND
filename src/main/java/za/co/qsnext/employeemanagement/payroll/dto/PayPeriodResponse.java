package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.PayPeriod;

import java.time.LocalDate;
import java.util.UUID;

public record PayPeriodResponse(UUID id, String name, LocalDate startDate, LocalDate endDate, LocalDate payDate) {

    public static PayPeriodResponse from(PayPeriod period) {
        return new PayPeriodResponse(
                period.getId(), period.getName(), period.getStartDate(), period.getEndDate(), period.getPayDate()
        );
    }
}
