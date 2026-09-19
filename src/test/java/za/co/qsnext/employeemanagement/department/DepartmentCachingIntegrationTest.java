package za.co.qsnext.employeemanagement.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import za.co.qsnext.employeemanagement.TestcontainersConfiguration;
import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the Redis-backed department cache against real infrastructure:
 * a cache hit must serve stale data (proving the DB isn't hit again), and
 * {@code update}/{@code delete} must evict it so subsequent reads are
 * correct again.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DepartmentCachingIntegrationTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void getByIdIsCachedUntilUpdateEvictsIt() {

        Department department = departmentService.create(
                "Cache Test Dept " + UUID.randomUUID(),
                "original description"
        );

        Department firstRead = departmentService.getById(department.getId());
        assertThat(firstRead.getDescription()).isEqualTo("original description");

        /*
         * Change the row directly through the repository, bypassing
         * DepartmentService (and therefore its @CacheEvict). If the
         * cache is working, the next getById must still return the
         * stale value.
         */
        Department managed = departmentRepository
                .findById(department.getId())
                .orElseThrow();
        managed.update(managed.getName(), "changed out from under the cache");
        departmentRepository.save(managed);

        Department stillCached = departmentService.getById(department.getId());
        assertThat(stillCached.getDescription()).isEqualTo("original description");

        /*
         * Going through the service's update() must evict, so the next
         * read reflects what update() actually wrote.
         */
        departmentService.update(
                department.getId(),
                managed.getName(),
                "updated through the service"
        );

        Department afterEvict = departmentService.getById(department.getId());
        assertThat(afterEvict.getDescription()).isEqualTo("updated through the service");
    }

    @Test
    void getByNameCacheIsClearedOnDelete() {

        String name = "Cache Test Dept " + UUID.randomUUID();

        Department department = departmentService.create(
                name,
                "will be deleted"
        );

        Department cached = departmentService.getByName(name);
        assertThat(cached.getId()).isEqualTo(department.getId());

        departmentService.delete(department.getId());

        assertThat(
                departmentRepository.findById(department.getId())
        ).isEmpty();

        /*
         * If delete() hadn't evicted the by-name cache, this would still
         * return the deleted department instead of throwing.
         */
        assertThatThrownBy(() -> departmentService.getByName(name))
                .isInstanceOf(DepartmentNotFoundException.class);
    }
}
