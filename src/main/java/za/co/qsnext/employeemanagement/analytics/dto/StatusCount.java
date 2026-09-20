package za.co.qsnext.employeemanagement.analytics.dto;

/**
 * A single status/count pair, shared across several analytics
 * categories that break a total down by an entity's status field.
 */
public record StatusCount(String status, long count) {
}
