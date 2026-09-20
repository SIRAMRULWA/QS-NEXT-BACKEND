package za.co.qsnext.employeemanagement.recognition;

import java.util.UUID;

/**
 * Spring Data interface projection for the points-leaderboard
 * aggregate query - a dynamic proxy backed by the query's aliased
 * columns, not a persisted type.
 */
public interface RecognitionLeaderboardEntry {

    UUID getEmployeeId();

    Long getTotalPoints();
}
