package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.TaxBracket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TaxBracketResponse(
        UUID id,
        UUID taxConfigurationId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal ratePercent,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {

    public static TaxBracketResponse from(TaxBracket bracket) {
        return new TaxBracketResponse(
                bracket.getId(), bracket.getTaxConfigurationId(), bracket.getMinAmount(), bracket.getMaxAmount(),
                bracket.getRatePercent(), bracket.getEffectiveFrom(), bracket.getEffectiveTo()
        );
    }
}
