package za.co.qsnext.employeemanagement.department;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private static final String BY_NAME_CACHE = "departmentsByName";

    private final DepartmentRepository departmentRepository;
    private final CacheManager cacheManager;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            CacheManager cacheManager
    ) {
        this.departmentRepository = departmentRepository;
        this.cacheManager = cacheManager;
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

    @Cacheable(value = BY_NAME_CACHE, key = "#name")
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
     * can change), so it can't be evicted by the same @CacheEvict(key=...)
     * the by-id cache uses. It's evicted programmatically, by both the
     * old and new name, via CacheManager directly - this also sidesteps
     * Spring's proxy-based caching aspect not applying to self-invoked
     * calls, which a private @CacheEvict-annotated helper would hit.
     */
    @Transactional
    @CacheEvict(value = "departments", key = "#departmentId")
    public Department update(
            UUID departmentId,
            String name,
            String description
    ) {
        Department department = getById(departmentId);
        String previousName = department.getName();

        if (!previousName.equalsIgnoreCase(name)
                && departmentRepository.existsByName(name)) {

            throw new DuplicateResourceException(
                    "Department already exists: " + name
            );
        }

        department.update(name, description);

        evictByName(previousName);
        evictByName(name);

        return department;
    }

    @Transactional
    @CacheEvict(value = "departments", key = "#departmentId")
    public void delete(UUID departmentId) {
        Department department = getById(departmentId);

        departmentRepository.delete(department);

        evictByName(department.getName());
    }

    private void evictByName(String name) {

        Cache cache = cacheManager.getCache(BY_NAME_CACHE);

        if (cache != null) {
            cache.evict(name);
        }
    }
}