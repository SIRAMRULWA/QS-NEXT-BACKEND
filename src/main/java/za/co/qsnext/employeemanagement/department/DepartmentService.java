package za.co.qsnext.employeemanagement.department;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository
    ) {
        this.departmentRepository = departmentRepository;
    }

    public Department getById(UUID departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new DepartmentNotFoundException(
                                "Department not found: " + departmentId
                        )
                );
    }

    public Department getByName(String name) {
        return departmentRepository.findByName(name)
                .orElseThrow(() ->
                        new DepartmentNotFoundException(
                                "Department not found: " + name
                        )
                );
    }

    @Transactional
    public Department create(
            String name,
            String description
    ) {
        if (departmentRepository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "Department already exists: " + name
            );
        }

        Department department = new Department(
                name,
                description
        );

        return departmentRepository.save(department);
    }

    @Transactional
    public Department update(
            UUID departmentId,
            String name,
            String description
    ) {
        Department department = getById(departmentId);

        if (!department.getName().equalsIgnoreCase(name)
                && departmentRepository.existsByName(name)) {

            throw new DuplicateResourceException(
                    "Department already exists: " + name
            );
        }

        department.update(name, description);

        return department;
    }

    @Transactional
    public void delete(UUID departmentId) {
        Department department = getById(departmentId);

        departmentRepository.delete(department);
    }
}