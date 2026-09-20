package za.co.qsnext.employeemanagement.mobile.dto;

import za.co.qsnext.employeemanagement.mobile.MobileDevice;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MobileDeviceResponse(
        UUID id,
        String platform,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static MobileDeviceResponse from(MobileDevice device) {
        return new MobileDeviceResponse(
                device.getId(),
                device.getPlatform(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
