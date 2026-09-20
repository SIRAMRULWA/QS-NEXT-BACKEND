package za.co.qsnext.employeemanagement.notification.dto;

import za.co.qsnext.employeemanagement.notification.NotificationPreference;

public record NotificationPreferenceResponse(
        boolean inAppEnabled,
        boolean emailEnabled
) {

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isInAppEnabled(),
                preference.isEmailEnabled()
        );
    }

    public static NotificationPreferenceResponse defaults() {
        return new NotificationPreferenceResponse(true, true);
    }
}
