package com.astrochakra.accounting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Validates every JPA entity against the REAL Supabase schema (ddl-auto=validate).
 * Skipped automatically unless SUPABASE_DB_URL is set, so normal builds/CI are unaffected.
 *
 * Run it (with backend/.env.local providing the credentials) and a green result means
 * all entity mappings match your actual tables; a failure names the exact mismatch.
 */
@SpringBootTest
@ActiveProfiles("supabase")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
class SchemaValidationTest {

    @Test
    void entitiesMatchRealSchema() {
        // If the application context loads under the 'supabase' profile, Hibernate's
        // schema validation passed against the real database.
    }
}
