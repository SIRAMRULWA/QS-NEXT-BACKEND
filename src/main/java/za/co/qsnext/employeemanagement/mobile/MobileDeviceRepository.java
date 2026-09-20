package za.co.qsnext.employeemanagement.mobile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobileDeviceRepository extends JpaRepository<MobileDevice, UUID> {

    List<MobileDevice> findByUserId(UUID userId);

    Optional<MobileDevice> findByDeviceToken(String deviceToken);
}
