package za.co.qsnext.employeemanagement.payroll.dto;

import java.util.List;

/**
 * Everything needed to render a payslip document (the "payslip
 * generation metadata" requirement) - this system does not itself
 * produce a PDF, only the structured data a renderer would consume.
 */
public record PayslipResponse(
        PayrollRunEntryResponse entry,
        PayPeriodResponse payPeriod,
        List<PayrollLineItemResponse> lineItems
) {
}
