package za.co.qsnext.employeemanagement.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(

        @NotBlank
        @Size(max = 500)
        String deviceToken,

        @NotBlank
        @Pattern(regexp = "IOS|ANDROID|WEB")
        String platform
) {
}
