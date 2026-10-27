# Verification

Checked locally on 27 October 2026 with Java 17 and Maven 3.9.12.

- `mvn clean verify`: passed; executable JAR built successfully.
- 11 automated tests: passed, zero failures or skips.
- API tests verify missing-key behavior, invalid JSON, and removal of frontend and fixture endpoints.
- Client transport tests check authentication, request format, experimental headers, rate limits, malformed responses and missing keys.
- Terminal tests verify stdin, raw provider JSON, help and invalid-input errors.
- Packaged JAR checks verify request files, stdin, working-directory `.env` loading, configured model and authentication, JSON-only stdout, missing-key exit code 1, invalid-input exit code 2 and JSON help.
- Packaged JAR contains no frontend assets, canned-answer catalogue or example action policy.
- The previous browser demo process was stopped.

Provider transport checks use a local test server. Live TypeSafe model predictions were not run because no API key was supplied. Application requests always use the configured provider; no test server or simulated responses are included in the running application.
