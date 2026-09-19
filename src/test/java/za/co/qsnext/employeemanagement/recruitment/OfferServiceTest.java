package za.co.qsnext.employeemanagement.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import za.co.qsnext.employeemanagement.onboarding.OnboardingService;
import za.co.qsnext.employeemanagement.recruitment.dto.HireResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.OfferResponse;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private OnboardingService onboardingService;
    @Mock
    private PasswordService passwordService;
    @Mock
    private AuthService authService;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditService auditService;

    private OfferService offerService;

    @BeforeEach
    void setUp() {
        offerService = new OfferService(
                offerRepository, applicationRepository, candidateRepository, userRepository, roleRepository,
                employeeService, onboardingService, passwordService, authService, emailService, auditService);
    }

    private Application applicationWithId(UUID id, UUID candidateId) {
        Application application = new Application(candidateId, UUID.randomUUID());
        setId(application, id);
        return application;
    }

    private Candidate candidateWithId(UUID id) {
        Candidate candidate = new Candidate("Jane", "Doe", "jane@example.com", "0123456789", null, "referral");
        setId(candidate, id);
        return candidate;
    }

    private Offer offerWithId(UUID id, UUID applicationId) {
        Offer offer = new Offer(
                applicationId, "Backend Engineer", BigDecimal.valueOf(50000), "ZAR",
                LocalDate.now().plusMonths(1));
        setId(offer, id);
        return offer;
    }

    @Test
    void createOffer_savesOfferAndAdvancesApplicationToOfferStage() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID());

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(offerRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(offerRepository.save(any())).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            setId(offer, UUID.randomUUID());
            return offer;
        });

        OfferResponse response = offerService.createOffer(
                applicationId, "Backend Engineer", BigDecimal.valueOf(60000), null, LocalDate.now().plusMonths(1));

        assertThat(response.currency()).isEqualTo("ZAR");
        assertThat(application.getStatus()).isEqualTo(Application.STATUS_OFFER);
    }

    @Test
    void createOffer_rejectsWhenAnOfferAlreadyExists() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID());

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(offerRepository.findByApplicationId(applicationId))
                .thenReturn(Optional.of(offerWithId(UUID.randomUUID(), applicationId)));

        assertThatThrownBy(() -> offerService.createOffer(
                applicationId, "Backend Engineer", BigDecimal.TEN, "ZAR", LocalDate.now().plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sendOffer_queuesAnEmailToTheCandidate() {
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        Offer offer = offerWithId(offerId, applicationId);
        Application application = applicationWithId(applicationId, candidateId);

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidateWithId(candidateId)));

        OfferResponse response = offerService.sendOffer(offerId);

        assertThat(response.status()).isEqualTo(Offer.STATUS_SENT);
        verify(emailService).queueEmail(eq(EmailTemplate.OFFER_EXTENDED), eq("jane@example.com"), any());
    }

    @Test
    void declineOffer_rejectsTheUnderlyingApplication() {
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        Offer offer = offerWithId(offerId, applicationId);
        offer.send();
        Application application = applicationWithId(applicationId, UUID.randomUUID());

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        offerService.declineOffer(offerId);

        assertThat(application.getStatus()).isEqualTo(Application.STATUS_REJECTED);
    }

    @Test
    void withdrawOffer_throws_whenAlreadyAccepted() {
        UUID offerId = UUID.randomUUID();
        Offer offer = offerWithId(offerId, UUID.randomUUID());
        offer.send();
        offer.accept();

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.withdrawOffer(offerId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void hire_throws_whenOfferIsNotAccepted() {
        UUID offerId = UUID.randomUUID();
        Offer offer = offerWithId(offerId, UUID.randomUUID());

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.hire(
                offerId, "jane.doe", "EMP-100", UUID.randomUUID(), LocalDate.now(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void hire_throws_whenUsernameIsTaken() {
        UUID offerId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        Offer offer = offerWithId(offerId, applicationId);
        offer.send();
        offer.accept();
        Application application = applicationWithId(applicationId, candidateId);

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidateWithId(candidateId)));
        when(userRepository.existsByUsername("jane.doe")).thenReturn(true);

        assertThatThrownBy(() -> offerService.hire(
                offerId, "jane.doe", "EMP-100", UUID.randomUUID(), LocalDate.now(), null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void hire_createsUserAndEmployee_andStartsOnboarding_whenTemplateProvided() {
        UUID offerId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Offer offer = offerWithId(offerId, applicationId);
        offer.send();
        offer.accept();
        Application application = applicationWithId(applicationId, candidateId);
        Candidate candidate = candidateWithId(candidateId);
        Role employeeRole = new Role(UUID.randomUUID(), "EMPLOYEE", "Employee role");

        Employee employee = new Employee(
                UUID.randomUUID(), departmentId, "EMP-100", "Jane", "Doe",
                "0123456789", "Backend Engineer", LocalDate.now());
        setId(employee, employeeId);

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(userRepository.existsByUsername("jane.doe")).thenReturn(false);
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(passwordService.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, UUID.randomUUID());
            return user;
        });
        when(employeeService.create(
                any(), eq(departmentId), eq("EMP-100"), eq("Jane"), eq("Doe"),
                eq("0123456789"), eq("Backend Engineer"), any()
        )).thenReturn(employee);

        HireResponse response = offerService.hire(
                offerId, "jane.doe", "EMP-100", departmentId, LocalDate.now(), templateId);

        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.onboardingStarted()).isTrue();
        assertThat(application.getStatus()).isEqualTo(Application.STATUS_HIRED);
        verify(authService).forgotPassword(new ForgotPasswordRequest("jane@example.com"));
        verify(onboardingService).startWorkflow(employeeId, templateId);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
