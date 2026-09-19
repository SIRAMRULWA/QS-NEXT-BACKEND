package za.co.qsnext.employeemanagement.esignature;

import org.springframework.stereotype.Component;

import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.util.List;

/**
 * Default {@link ESignatureProvider}: notifies signers and requesters
 * in-app via the existing notification pipeline. This does NOT produce a
 * legally binding signature - it is a self-hosted workflow only, clearly
 * separated behind the interface so a real external provider can replace
 * it later without touching {@link SignatureRequestService}.
 */
@Component
public class InternalESignatureProvider implements ESignatureProvider {

    private final NotificationPublisher notificationPublisher;

    public InternalESignatureProvider(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public void requestSignatures(SignatureRequest request, List<SignatureRequestSigner> signers) {
        for (SignatureRequestSigner signer : signers) {
            notificationPublisher.publish(
                    signer.getSignerUserId(),
                    NotificationType.SIGNATURE_REQUESTED,
                    "Signature requested",
                    "You have been asked to sign \"" + request.getTitle() + "\"."
            );
        }
    }

    @Override
    public void notifyOutcome(SignatureRequest request) {

        NotificationType type = switch (request.getStatus()) {
            case SignatureRequest.STATUS_COMPLETED -> NotificationType.SIGNATURE_REQUEST_COMPLETED;
            case SignatureRequest.STATUS_DECLINED -> NotificationType.SIGNATURE_REQUEST_DECLINED;
            default -> null;
        };

        if (type == null) {
            return;
        }

        String message = type == NotificationType.SIGNATURE_REQUEST_COMPLETED
                ? "All signers have signed \"" + request.getTitle() + "\"."
                : "\"" + request.getTitle() + "\" was declined by a signer.";

        notificationPublisher.publish(request.getRequestedBy(), type, "Signature request update", message);
    }
}
