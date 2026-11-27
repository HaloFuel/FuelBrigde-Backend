package com.halofuel.fuelbridge.platform.iam.domain.model.valueobjects;

import java.util.Locale;

/**
 * ServiceIdentity
 *
 * A JWT whose subject starts with {@link #SUBJECT_PREFIX} is a service-to-service token (issued by the
 * shared-kernel ServiceTokenIssuer) and is authenticated without a user lookup. Because a user token
 * carries the username as its subject, usernames starting with that prefix are reserved: otherwise a
 * self-registered user could obtain a service identity.
 */
public final class ServiceIdentity {

    /** Subject prefix of service-to-service tokens; also the reserved username prefix. */
    public static final String SUBJECT_PREFIX = "fuelbridge-";

    private ServiceIdentity() {
    }

    /** Exact rule used by the bearer filter to recognise a service token subject. */
    public static boolean isServiceSubject(String subject) {
        return subject != null && subject.startsWith(SUBJECT_PREFIX);
    }

    /**
     * Rule used at registration. It is deliberately wider than {@link #isServiceSubject}: it ignores
     * surrounding whitespace and letter case, so no variant that a case-insensitive database collation
     * or a later normalisation could fold into the prefix gets through.
     */
    public static boolean isReservedUsername(String username) {
        return username != null && username.strip().toLowerCase(Locale.ROOT).startsWith(SUBJECT_PREFIX);
    }
}
