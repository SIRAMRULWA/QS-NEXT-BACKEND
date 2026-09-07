package za.co.qsnext.employeemanagement.selfservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.selfservice.dto.SelfServiceProfileResponse;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SelfServiceService {

    private final EmployeeRepository employeeRepository;

    public SelfServiceService(
            EmployeeRepository employeeRepository
    ) {
        this.employeeRepository = employeeRepository;
    }

    public SelfServiceProfileResponse getOwnProfile(
            UUID userId
    ) {
        Employee employee = employeeRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee profile not found"
                        )
                );

        return SelfServiceProfileResponse.from(employee);
    }
}