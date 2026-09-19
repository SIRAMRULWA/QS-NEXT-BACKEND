package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.PayrollLineItem;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollLineItemResponse(
        UUID id,
        String type,
        String code,
        String description,
        BigDecimal amount
) {

    public static PayrollLineItemResponse from(PayrollLineItem lineItem) {
        return new PayrollLineItemResponse(
                lineItem.getId(), lineItem.getType(), lineItem.getCode(),
                lineItem.getDescription(), lineItem.getAmount()
        );
    }
}
