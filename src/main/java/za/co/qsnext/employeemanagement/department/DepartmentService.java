package za.co.qsnext.employeemanagement.department;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    @Cacheable(value = "departments", key = "#departmentId")
    public Department getById(UUID departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new DepartmentNotFoundException(
                                "Department not found: " + departmentId
                        )
                );
    }

    @Cacheable(value = "departmentsByName", key = "#name")
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

    /*
     * The by-name cache is keyed on a mutable field (a department's name
     * can change), so a rename can't be evicted by a single key the way
     * the by-id cache can. Renames/deletes are low-frequency admin
     * operations, so a full clear of that cache is a deliberate,
     * correctness-first trade-off rather than tracking old/new names.
     * Both evictions are declared directly on these public methods
     * (rather than delegated to a private helper) because Spring's
     * proxy-based caching aspect does not apply to self-invoked calls.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "departments", key = "#departmentId"),
            @CacheEvict(value = "departmentsByName", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "departments", key = "#departmentId"),
            @CacheEvict(value = "departmentsByName", allEntries = true)
    })
    public void delete(UUID departmentId) {
        Department department = getById(departmentId);

        departmentRepository.delete(department);
    }
}