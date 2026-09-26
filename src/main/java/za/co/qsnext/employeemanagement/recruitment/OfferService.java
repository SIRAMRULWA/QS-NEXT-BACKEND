package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.auth.AuthService;
import za.co.qsnext.employeemanagement.auth.PasswordService;
import za.co.qsnext.employeemanagement.auth.dto.ForgotPasswordRequest;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.onboarding.OnboardingService;
import za.co.qsnext.employeemanagement.recruitment.dto.HireResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.OfferResponse;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OfferService {

    private static final String DEFAULT_CURRENCY = "ZAR";
    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final String ENTITY_TYPE = "Offer";

    private final OfferRepository offerRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeService employeeService;
    private final OnboardingService onboardingService;
    private final PasswordService passwordService;
    private final AuthService authService;
    private final EmailService emailService;
    private final AuditService auditService;

    public OfferService(
            OfferRepository offerRepository,
            ApplicationRepository applicationRepository,
            CandidateRepository candidateRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            EmployeeService employeeService,
            OnboardingService onboardingService,
            PasswordService passwordService,
            AuthService authService,
            EmailService emailService,
            AuditService auditService
    ) {
        this.offerRepository = offerRepository;
        this.applicationRepository = applicationRepository;
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeService = employeeService;
        this.onboardingService = onboardingService;
        this.passwordService = passwordService;
        this.authService = authService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public OfferResponse createOffer(
            UUID applicationId,
            String jobTitle,
            BigDecimal salaryAmount,
            String currency,
            LocalDate startDate
    ) {
        Application application = findApplicationOrThrow(applicationId);

        if (!application.isActive()) {
            throw new BusinessRuleException("Cannot make an offer against an inactive application");
        }

        if (offerRepository.findByApplicationId(applicationId).isPresent()) {
            throw new BusinessRuleException("An offer already exists for this application");
        }

        String resolvedCurrency = (currency == null || currency.isBlank()) ? DEFAULT_CURRENCY : currency;

        Offer offer = offerRepository.save(
                new Offer(applicationId, jobTitle, salaryAmount, resolvedCurrency, startDate)
        );

        application.advanceToOffer();

        auditService.log("OFFER_CREATED", ENTITY_TYPE, offer.getId(), AuditService.RESULT_SUCCESS);

        return OfferResponse.from(offer);
    }

    public OfferResponse getOffer(UUID offerId) {
        return OfferResponse.from(findOfferOrThrow(offerId));
    }

    @Transactional
    public OfferResponse sendOffer(UUID offerId) {

        Offer offer = findOfferOrThrow(offerId);

        if (!offer.isDraft()) {
            throw new BusinessRuleException("Only a draft offer can be sent");
        }

        offer.send();

        Candidate candidate = candidateForOffer(offer);

        emailService.queueEmail(
                EmailTemplate.OFFER_EXTENDED,
                candidate.getEmail(),
                Map.of(
                        "candidateName", candidate.getFullName(),
                        "jobTitle", offer.getJobTitle(),
                        "startDate", offer.getStartDate().toString()
                )
        );

        auditService.log("OFFER_SENT", ENTITY_TYPE, offerId, AuditService.RESULT_SUCCESS);

        return OfferResponse.from(offer);
    }

    @Transactional
    public OfferResponse acceptOffer(UUID offerId) {

        Offer offer = findOfferOrThrow(offerId);

        if (!offer.isSent()) {
            throw new BusinessRuleException("Only a sent offer can be accepted");
        }

        offer.accept();

        auditService.log("OFFER_ACCEPTED", ENTITY_TYPE, offerId, AuditService.RESULT_SUCCESS);

        return OfferResponse.from(offer);
    }

    @Transactional
    public OfferResponse declineOffer(UUID offerId) {

        Offer offer = findOfferOrThrow(offerId);

        if (!offer.isSent()) {
            throw new BusinessRuleException("Only a sent offer can be declined");
        }

        offer.decline();

        Application application = applicationRepository.findById(offer.getApplicationId())
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Application not found: " + offer.getApplicationId()
                ));

        if (application.isActive()) {
            application.reject("Offer declined by candidate");
        }

        auditService.log("OFFER_DECLINED", ENTITY_TYPE, offerId, AuditService.RESULT_SUCCESS);

        return OfferResponse.from(offer);
    }

    @Transactional
    public OfferResponse withdrawOffer(UUID offerId) {

        Offer offer = findOfferOrThrow(offerId);

        if (offer.isAccepted()) {
            throw new BusinessRuleException("Cannot withdraw an offer that has already been accepted");
        }

        offer.withdraw();

        auditService.log("OFFER_WITHDRAWN", ENTITY_TYPE, offerId, AuditService.RESULT_SUCCESS);

        return OfferResponse.from(offer);
    }

    /**
     * Converts the candidate behind an accepted offer into a real
     * Employee and, optionally, starts their onboarding workflow - the
     * "integrate successful candidates with onboarding" requirement. A
     * throwaway random password is generated and immediately discarded;
     * the new hire gets a real password via the existing forgot-password
     * email flow, exactly as a self-registered user would.
     */
    @Transactional
    public HireResponse hire(
            UUID offerId,
            String username,
            String employeeNumber,
            UUID departmentId,
            LocalDate hireDate,
            UUID onboardingTemplateId
    ) {
        Offer offer = findOfferOrThrow(offerId);

        if (!offer.isAccepted()) {
            throw new BusinessRuleException("Only an accepted offer can be converted to a hire");
        }

        Application application = applicationRepository.findById(offer.getApplicationId())
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Application not found: " + offer.getApplicationId()
                ));

        Candidate candidate = candidateForOffer(offer);

        // An applicant who applied through the job board already has a
        // login; hiring promotes it instead of creating a second account.
        boolean existingLogin = candidate.getUserId() != null;

        User savedUser;

        if (existingLogin) {
            savedUser = userRepository.findById(candidate.getUserId())
                    .orElseThrow(() -> new RecruitmentNotFoundException(
                            "User not found for candidate: " + candidate.getId()
                    ));
            // EmployeeService#create swaps APPLICANT for EMPLOYEE.
        } else {
            if (userRepository.existsByUsername(username)) {
                throw new DuplicateResourceException("Username already exists: " + username);
            }

            Role employeeRole = roleRepository.findByName(EMPLOYEE_ROLE)
                    .orElseThrow(() -> new IllegalStateException("EMPLOYEE role is not configured"));

            User user = new User(username, candidate.getEmail(), passwordService.encode(generateThrowawayPassword()));
            user.assignRole(employeeRole);
            savedUser = userRepository.save(user);
        }

        Employee employee = employeeService.create(
                savedUser.getId(), departmentId, employeeNumber,
                candidate.getFirstName(), candidate.getLastName(), candidate.getPhone(),
                offer.getJobTitle(), hireDate
        );

        application.markHired();

        if (!existingLogin) {
            authService.forgotPassword(new ForgotPasswordRequest(candidate.getEmail()));
        }

        boolean onboardingStarted = onboardingTemplateId != null;

        if (onboardingStarted) {
            onboardingService.startWorkflow(employee.getId(), onboardingTemplateId);
        }

        auditService.log("CANDIDATE_HIRED", "Employee", employee.getId(), AuditService.RESULT_SUCCESS);

        return new HireResponse(employee.getId(), savedUser.getId(), onboardingStarted);
    }

    private String generateThrowawayPassword() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    private Candidate candidateForOffer(Offer offer) {

        Application application = applicationRepository.findById(offer.getApplicationId())
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Application not found: " + offer.getApplicationId()
                ));

        return candidateRepository.findById(application.getCandidateId())
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Candidate not found: " + application.getCandidateId()
                ));
    }

    private Application findApplicationOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Application not found: " + applicationId));
    }

    private Offer findOfferOrThrow(UUID offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Offer not found: " + offerId));
    }
}
