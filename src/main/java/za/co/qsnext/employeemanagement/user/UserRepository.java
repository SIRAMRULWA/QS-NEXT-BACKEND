package za.co.qsnext.employeemanagement.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {
            "roles",
            "roles.permissions"
    })
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // :query is cast to text so a null value (browse-everything) doesn't
    // hit "function lower(bytea) does not exist" - see EmployeeRepository
    // .search() for the same fix against the same underlying issue.
    @Query("""
            select u from User u
            where :query is null
            or lower(u.username) like lower(concat('%', cast(:query as string), '%'))
            or lower(u.email) like lower(concat('%', cast(:query as string), '%'))
            """)
    Page<User> search(@Param("query") String query, Pageable pageable);
}