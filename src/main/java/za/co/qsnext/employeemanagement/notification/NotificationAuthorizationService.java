package za.co.qsnext.employeemanagement.notification;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.UUID;

@Service("notificationAuthorizationService")
public class NotificationAuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_HR_MANAGER = "ROLE_HR_MANAGER";
    private static final String ROLE_HR_OFFICER = "ROLE_HR_OFFICER";

    private final NotificationRepository notificationRepository;

    public NotificationAuthorizationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    public boolean canAccessUserNotifications(
            UUID userId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        /*
         * HR users and administrators can access
         * employee notifications.
         */
        if (hasAuthority(authentication, ROLE_ADMIN)
                || hasAuthority(authentication, ROLE_HR_MANAGER)
                || hasAuthority(authentication, ROLE_HR_OFFICER)) {
            return true;
        }

        CustomUserDetails userDetails =
                getUserDetails(authentication);

        if (userDetails == null) {
            return false;
        }

        return userDetails
                .getUserId()
                .equals(userId);
    }

    public boolean canAccessNotification(
            UUID notificationId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        /*
         * HR users and administrators can access
         * employee notifications.
         */
        if (hasAuthority(authentication, ROLE_ADMIN)
                || hasAuthority(authentication, ROLE_HR_MANAGER)
                || hasAuthority(authentication, ROLE_HR_OFFICER)) {
            return true;
        }

        CustomUserDetails userDetails =
                getUserDetails(authentication);

        if (userDetails == null) {
            return false;
        }

        UUID authenticatedUserId =
                userDetails.getUserId();

        return notificationRepository
                .findById(notificationId)
                .map(notification ->
                        authenticatedUserId.equals(
                                notification.getUserId()
                        )
                )
                .orElse(false);
    }

    private boolean hasAuthority(
            Authentication authentication,
            String authority
    ) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority ->
                        authority.equals(
                                grantedAuthority.getAuthority()
                        )
                );
    }

    private CustomUserDetails getUserDetails(
            Authentication authentication
    ) {

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }

        return null;
    }
}