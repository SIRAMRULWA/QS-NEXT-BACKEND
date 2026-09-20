package za.co.qsnext.employeemanagement.payroll.dto;

import za.co.qsnext.employeemanagement.payroll.TaxConfiguration;

import java.util.UUID;

public record TaxConfigurationResponse(
        UUID id,
        String name,
        String description,
        String lineItemType,
        boolean active
) {

    public static TaxConfigurationResponse from(TaxConfiguration configuration) {
        return new TaxConfigurationResponse(
                configuration.getId(), configuration.getName(), configuration.getDescription(),
                configuration.getLineItemType(), configuration.isActive()
        );
    }
}
