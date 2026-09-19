package za.co.qsnext.employeemanagement.config;

public final class RedisCacheNames {

    private RedisCacheNames() {
    }

    /**
     * Per-request authorization lookups (userId, enabled/locked state,
     * authorities) keyed by username. Deliberately excludes the password
     * hash - see {@code CachedUserPrincipal}.
     */
    public static final String USER_PRINCIPALS = "userPrincipals";
}
