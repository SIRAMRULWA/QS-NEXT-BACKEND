package za.co.qsnext.employeemanagement.esignature;

import java.util.List;

/**
 * Extension point for dispatching signature requests and their outcomes
 * to whatever actually gets a signer's attention and captures their
 * signature. {@link InternalESignatureProvider} is the only
 * implementation today - it just raises in-app notifications, which is
 * NOT a legally binding e-signature. A real deployment that needs
 * legally binding signatures would plug in an adapter here for an
 * external provider (e.g. DocuSign, Adobe Sign) without
 * {@link SignatureRequestService} needing to change.
 */
public interface ESignatureProvider {

    void requestSignatures(SignatureRequest request, List<SignatureRequestSigner> signers);

    /**
     * Called once a request reaches a terminal outcome (completed,
     * declined or expired) so the requester can be told.
     */
    void notifyOutcome(SignatureRequest request);
}
