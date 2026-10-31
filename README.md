# Family Planner

A service to manage household activities. It provides web application and a REST API

## Prerequisites

- JDK 17
- Docker
- `openssl`, for generating

## Configuration

The server reads these environment variables at startup

| Variable                        | Required        | Default             | Purpose                                                                              |
|---------------------------------|-----------------|---------------------|--------------------------------------------------------------------------------------|
| `FAMILYPLANNER_DB_PERSISTENCE`  | No              | `memory`            | `memory` or `postgres`. User postgres for any real deployment.                       |
| `FAMILYPLANNER_DB_URL`          | With `postgres` | -                   | JDBC URL, for example `jdbc:postgresql://db:5432/familyplanner`                      |
| `FAMILYPLANNER_DB_USER`         | With `postgres` | -                   | Database user.                                                                       |
| `FAMILYPLANNER_DB_PASSWORD`     | With `postgres` | -                   | Database password.                                                                   |
| `FAMILYPLANNER_SECRET`          | With `postgres` | Random per start    | Secret used for CSRF tokens. At least 32 characters.                                 |
| `FAMILYPLANNER_SECURE_COOKIES`  | No              | `true`              | Sets the `Secure` flag on cookies. Set to `false` only for plain-HTTP local testing. |
| `FAMILYPLANNER_JWT_PRIVATE_KEY` | With `postgres` | Random per start    | Path to the RSA private key in PKCS#8 PEM format. See below.                         |
| `FAMILYPLANNER_JWT_ISSUER`      | No              | `familyplanner`     | Issuer claim in access tokens.                                                       |
| `FAMILYPLANNER_JWT_AUDIENCE`    | No              | `familyplanner-api` | Audience claim in access tokens.                                                     |

The server listens on port 8080.

Notes:
- With `memory` persistence, the signing key and secret are generated at startup. All users, sessions, and tokens are lost when the server restarts.
- With `postgres`, the server will not start unless `FAMILYPLANNER_DB_*`, `FAMILYPLANNER_SECRET`, and `FAMILYPLANNER_JWT_PRIVATE_KEY` are set.
- Secure cookies are sent only over HTTPS, except on `localhost`. If you test over plain HTTP from another machine, set `FAMILYPLANNER_SECURE_COOKIES=false`.

## The JWT private key

API access tokens are signed with an RSA key. The serer reads the private key from the file named in `FAMILYPLANNER_JWT_PRIVATE_KEY` and derives the public key from it.

Generate a 2048-bit key in PKCS#8 format:

```
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/jwt-private.pem
chmod 600 keys/jwt-private.pem
```

Rules:

- The file must start with `-----BEGIN PRIVATE KEY-----`. The server rejects other formats, such as `BEGIN RSA PRIVATE KEY`.
- Keep the file out of version control. The `keys/` folder must be in `.gitignore`.
- Keep a backup. If the key is lost, all access tokens become invalid and clients must sign it again.
- If you replace the key, access token signed with the old key stop working. Clients receive a 401 and must refresh their tokens to get new ones.

## Running with Docker Compose

1. Create a `.env` file in the repo root. Use `.env.example` as the starting point and set real values for `POSTGRES_PASSWORD` and `FAMILYPLANNER_SECRET`.
2. Generate the JWT key as described above
3. Start the stack:
    
    ```
   docker compose up -d --build
   ```

4. Check that the server responds:

    ```
   curl -i http://localhost:8080/login
   ```

Database migrations run automatically when the server starts.

## Creating an API key

Each application that call the REST API needs its own API key. The key is sent in the `X-API-Key` header.

The key is printed once and cannot be recovered. Store it in the calling application's configuration. The database keeps only a hash.

Running locally with Gradle, against a running Postgres:

```
export FAMILYPLANNER_DB_URL=jdbc:postgresql://localhost:5432/familyplanner
export FAMILYPLANNER_DB_USER=familyplanner
export FAMILYPLANNER_DB_PASSWORD=<your password>
export FAMILYPLANNER_SECRET=<your secret>
export FAMILYPLANNER_JWT_PRIVATE_KEY=$PWD/keys/jwt-private.pem

./gradlew createApiKey -PclientName=my-app
```

Running inside the container:

```
docker compose exec app java -cp "/app/lib/*" com.luisteixeira.familyplanner.admin.CreateApiKeyKt my-app
```

## Running the test

- `./gradlew test` runs the unit tests. No Docker needed.
- `./gradlew postgresTest` runs the tests against Postgres. Docker must be running, because the tests start a database container.

## Running without Docker

```
export FAMILYPLANNER_PERSISTENCE=postgres
# plus the variables listed above
./gradlew :app:run
```