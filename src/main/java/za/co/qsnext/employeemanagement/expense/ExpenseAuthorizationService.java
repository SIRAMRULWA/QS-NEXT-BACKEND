package za.co.qsnext.employeemanagement.expense;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import za.co.qsnext.employeemanagement.security.EmployeeAuthorizationService;

import java.util.UUID;

/**
 * Object-level authorization for approving an expense claim: HR/admin for
 * any claim, or the claimant's direct manager. An unknown claim id is
 * treated as accessible so the service surfaces its own 404.
 */
@Service("expenseAuthorizationService")
public class ExpenseAuthorizationService {

    private final ExpenseClaimRepository claimRepository;
    private final EmployeeAuthorizationService employeeAuthorizationService;

    public ExpenseAuthorizationService(
            ExpenseClaimRepository claimRepository,
            EmployeeAuthorizationService employeeAuthorizationService
    ) {
        this.claimRepository = claimRepository;
        this.employeeAuthorizationService = employeeAuthorizationService;
    }

    public boolean canApproveClaim(UUID claimId, Authentication authentication) {

        return claimRepository.findById(claimId)
                .map(ExpenseClaim::getEmployeeId)
                .map(employeeId -> employeeAuthorizationService.canApproveFor(employeeId, authentication))
                .orElse(true);
    }
}
