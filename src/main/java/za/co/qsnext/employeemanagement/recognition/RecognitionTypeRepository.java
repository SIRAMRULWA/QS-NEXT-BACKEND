package za.co.qsnext.employeemanagement.recognition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecognitionTypeRepository extends JpaRepository<RecognitionType, UUID> {

    boolean existsByName(String name);

    List<RecognitionType> findByActiveTrue();
}
