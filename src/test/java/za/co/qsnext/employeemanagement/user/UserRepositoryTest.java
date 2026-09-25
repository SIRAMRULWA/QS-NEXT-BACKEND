package za.co.qsnext.employeemanagement.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import za.co.qsnext.employeemanagement.common.AbstractRepositoryTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level tests for {@link UserRepository#search}, run against a
 * real PostgreSQL instance (via Testcontainers) rather than a mock, since
 * the null-query type-inference bug this mirrors (see
 * {@link za.co.qsnext.employeemanagement.employee.EmployeeRepository#search})
 * only manifests against a real JDBC driver/database - a mocked repository
 * never sends any SQL.
 */
class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void search_withNullQuery_matchesEverything() {
        User user = seedUser();

        Page<User> result = userRepository.search(null, PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(User::getId)
                .contains(user.getId());
    }

    @Test
    void search_withEmptyStringQuery_matchesEverything() {
        User user = seedUser();

        Page<User> result = userRepository.search("", PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(User::getId)
                .contains(user.getId());
    }

    @Test
    void search_withNonBlankQuery_matchesByUsername() {
        User user = seedUser();

        Page<User> result = userRepository.search(
                user.getUsername().substring(0, 5),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(User::getId)
                .contains(user.getId());
    }

    @Test
    void search_withNonBlankQuery_matchesByEmail() {
        User user = seedUser();

        Page<User> result = userRepository.search(
                user.getEmail().substring(0, 5),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(User::getId)
                .contains(user.getId());
    }

    private User seedUser() {
        String suffix = UUID.randomUUID().toString();

        return userRepository.saveAndFlush(
                new User("search-user-" + suffix, "search-" + suffix + "@example.com", "hash"));
    }
}
