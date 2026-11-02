# Render deployment

Create a **Web Service** from `luuongaro/BookVerseBackend`, branch `develop`, using
the **Docker** runtime. Leave Root Directory empty, use `./Dockerfile`, and set
the Health Check Path under Advanced to `/api/health`.

The image compiles the existing Java 24 target using Temurin 25 and runs with a
JRE. Its default `render` profile binds to `0.0.0.0:${PORT:10000}`. Database schema
updates are disabled in this profile; apply reviewed migrations to the existing
Supabase database before deployment, including `docs/sql/fellow-readers-profile.sql`.

## Environment variables

Enter private values in Render's **Environment Variables** section. Do not commit
an `.env` file or copy it into the Docker image. Use the deployment's credentials,
not placeholders. Existing `application.yaml` requires these names:

| Variable | Value source |
| --- | --- |
| `BOOKS_VERSE_URL` | Supabase PostgreSQL JDBC URL; require SSL. Use the Supabase session pooler if direct IPv6 connectivity is unavailable. |
| `DB_USER`, `DB_PASS` | Supabase connection credentials for the selected connection method |
| `JWT_SECRET` | Existing signing secret suitable for the configured JWT algorithm |
| `JWT_EXPIRATION` | Token lifetime in milliseconds |
| `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Existing configured bootstrap administrator |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Mail provider settings |
| `GOOGLE_BOOKS_API_URL` | Google Books volumes endpoint, `https://www.googleapis.com/books/v1/volumes` |
| `GOOGLE_BOOKS_API_KEY` | Google Books credential |
| `MERCADO_PAGO_ACCESS_TOKEN` | Mercado Pago server credential |

Optional `CORS_ALLOWED_ORIGINS` is a comma-separated list of exact origins. The
Render profile defaults to `https://www.bookverse.com.ar,https://bookverse.com.ar`.
Local development continues to allow `http://localhost:4200` by default. Include
only specific preview origins if needed; do not use a wildcard for all Vercel sites.

## Verify and connect the frontend

1. Deploy a commit containing `Dockerfile`, then wait for the service to be Live.
2. Open `https://<service>.onrender.com/api/health`; expect `{"status":"UP"}`.
3. Configure all frontend API requests, including authentication and its interceptor,
   to use the Render HTTPS URL instead of `localhost`. Rebuild the frontend.
4. Test login, a protected API request, and an unauthenticated request. Only the
   health endpoint and existing public endpoints bypass authentication.

The Docker packaging step skips test compilation because the current unrelated
legacy test suite does not compile. Run targeted verification before merging; a
successful image build alone does not verify application behavior.

## Free instance limitations

Free instances sleep after inactivity and wake on the next request. They also block
outbound SMTP ports 25, 465 and 587, so the current SMTP email flows require either
an HTTP email provider integration or an appropriate hosting plan. Do not treat a
successful deployment as confirmation that registration emails or password resets
work. Supabase remains the database; no Render Postgres service is needed.

References: [Docker on Render](https://render.com/docs/docker),
[free instance limitations](https://render.com/docs/free).
