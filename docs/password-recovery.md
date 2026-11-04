# Password recovery

## API and frontend

- `POST /api/auth/forgot-password` accepts `{ "email": "reader@example.com" }` and returns `202` without a body for any syntactically valid email. No login is required. Account lookup runs in a bounded background executor, so neither account existence nor mail delivery is disclosed in the response.
- Only active accounts receive a link. One account can receive at most one new link every 120 seconds. A new link replaces the previous one.
- The email points to `{FRONTEND_URL}/reset-password#token=...`. The frontend also exposes `/forgot-password`, linked from login.
- Tokens contain 32 cryptographically random bytes. Only their SHA-256 digest is stored. They expire after 30 minutes and can be consumed once. A database user-row lock serializes issuance and consumption, including concurrent requests across instances.
- `POST /api/auth/reset-password` accepts `{ "token": "...", "password": "..." }`. It returns `204` on success and `410` for an expired, used or unknown well-formed token. Malformed requests or unsupported passwords return `400`.
- Reset passwords require lowercase, uppercase, a digit and one of `@$!%*?&._-`, with 8–72 characters and at most 72 UTF-8 bytes (the BCrypt limit). Confirmation is checked in the frontend; the backend enforces the actual password policy.
- Resetting a password increments `users.credentials_version`, invalidating older JWTs. Existing JWTs without this claim continue working until the account's first reset. New JWTs use the version of the principal that actually passed password authentication, so a reset racing with login cannot upgrade old credentials.
- The browser removes the recovery fragment from its address bar, keeps it only in component memory and never stores it in Local Storage. Password reset requests do not carry the session Authorization header. After success the browser clears the previous local session and offers a normal login.

## Deployment order

1. **Before deploying the backend**, run `docs/sql/password-recovery.sql` against Supabase. Render uses `ddl-auto: none`; the new column must exist before normal user queries run. The migration is additive and idempotent. Its token table has RLS enabled and must only be accessed through the backend database connection.
2. Prepare Resend and its Render variables as described in `docs/resend-email.md`, then deploy the backend branch together with the frontend branch `feature/password-recovery`.
3. Confirm `FRONTEND_URL` is the frontend origin, without a trailing slash, query or fragment. The Render profile defaults to `https://www.bookverse.com.ar`; local development defaults to `http://localhost:4200`. HTTPS is required except for localhost or 127.0.0.1.
4. The Render profile sends welcome, recovery and confirmation emails through Resend over HTTPS. Before deploying, configure `RESEND_API_KEY`, `MAIL_FROM` on a verified domain and `MAIL_PROVIDER=resend`. Follow `docs/resend-email.md`. Local development can continue using SMTP. Actual delivery needs verification in the deployed environment; automated checks replace the sender and do not send real messages.
5. Verify a test account can request the mail, open the link, set a new password and log in. The previous password and previous JWT must stop working; reusing the link must fail. No password or raw recovery token should appear in logs.

Mail is queued only after the database transaction commits. A failed or rolled-back request never sends an unusable link. A confirmation message is sent after a successful password change; no password is included in either message.

## Verification

```sh
./mvnw -Ppassword-recovery-tests package
```

This opt-in profile compiles and executes the recovery, authentication snapshot and mail tests. The default Maven lifecycle is unchanged. The existing complete test compilation is currently blocked by unrelated stale tests in reviews and subscriptions; the profile makes the new checks reproducible without editing or disabling those tests globally.

Verified locally: 37 backend tests pass and the executable jar is built. Tests use H2 in PostgreSQL mode and a mocked mail sender, not production data or email. Covered cases include generic public responses, validation, throttling, token expiry/reuse, concurrent consumption, suspended accounts, rollback before notification, BCrypt storage and revocation of legacy/current JWTs and the actual asynchronous transaction/mail ordering. The frontend has 19 passing focused auth tests and a successful production build.

Reference: [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
