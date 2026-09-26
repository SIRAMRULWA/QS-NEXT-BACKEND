package za.co.qsnext.employeemanagement.employee;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.auth.AuthService;
import za.co.qsnext.employeemanagement.auth.PasswordService;
import za.co.qsnext.employeemanagement.employee.dto.InviteEmployeeRequest;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.RoleRepository;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates the login and employee record for an existing staff member in
 * one step, so staff never have to self-register (which now only makes
 * someone an applicant). The throwaway password is never shown to anyone;
 * the person sets their own from the invitation email.
 */
@Service
public class EmployeeInvitationService {

    private static final String EMPLOYEE_ROLE = "EMPLOYEE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;
    private final EmployeeService employeeService;
    private final AuthService authService;
    private final AuditService auditService;

    public EmployeeInvitationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordService passwordService,
            EmployeeService employeeService,
            AuthService authService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordService = passwordService;
        this.employeeService = employeeService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Transactional
    public Employee invite(InviteEmployeeRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Role employeeRole = roleRepository.findByName(EMPLOYEE_ROLE)
                .orElseThrow(() -> new IllegalStateException("EMPLOYEE role is not configured"));

        User user = new User(
                request.username(),
                request.email(),
                passwordService.encode(generateThrowawayPassword())
        );
        user.assignRole(employeeRole);
        User savedUser = userRepository.save(user);

        Employee employee = employeeService.create(
                savedUser.getId(),
                request.departmentId(),
                request.employeeNumber(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.jobTitle(),
                request.hireDate()
        );

        authService.sendInvitation(savedUser, request.firstName());

        auditService.log("EMPLOYEE_INVITED", "Employee", employee.getId(), AuditService.RESULT_SUCCESS);

        return employee;
    }

    private String generateThrowawayPassword() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
