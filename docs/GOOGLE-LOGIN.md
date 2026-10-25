# Google login: setup and verification

The application implements Google OpenID Connect through Spring Security. It requests only `openid`, `profile` and `email`. Google login is optional: both server environment variables below must be present before its button and routes are enabled.

## Configure a Google OAuth client

In your own Google Cloud project, configure the consent screen and create an OAuth client of type **Web application**. Keep access limited to basic identity (`openid`, `profile`, `email`); no Gmail or Drive access is needed. Google's [Audience documentation](https://support.google.com/cloud/answer/15549945?hl=en) exempts basic-identity-only requests from the usual test-user allowlist and seven-day authorization rules. Managed Google accounts can still impose their own restrictions.

Use the production site origin:

```text
https://khayem-car-rental.vercel.app
```

Register this exact authorized redirect URI:

```text
https://khayem-car-rental.vercel.app/login/oauth2/code/google
```

For local development on port 8080, you can separately register:

```text
http://localhost:8080/login/oauth2/code/google
```

The scheme, host, port and path must match the actual callback. Preview deployment URLs are not automatically authorized. Keep the deployment's forwarded-header and HTTPS cookie settings intact.

Add these **server environment variables** to the existing Vercel project, then redeploy:

| Variable | Value |
| --- | --- |
| `GOOGLE_CLIENT_ID` | The web client's ID from Google Cloud |
| `GOOGLE_CLIENT_SECRET` | That client's secret |

Enter the secret in the hosting provider's environment settings. Do not commit it, put it in frontend JavaScript or paste it into issues, logs or chat. This repository contains no configured Google client.

## Identity and account behavior

- Spring Security validates the authorization response, state, nonce, ID-token signature and standard token claims.
- The application additionally requires Google's issuer, a non-empty subject and a verified email address.
- The stable account key is `(provider, subject)`. Email matching never automatically links an existing local account.
- The first login creates a CLIENT with fictional identity references, not a verified driving licence. Google login does not verify eligibility to rent a vehicle.
- A Google account cannot set a local password through the profile page. Manage the Google password with Google.
- Under the default H2 `demo` profile, data and HTTP sessions remain temporary and local to each running instance. For a hosted Google login, configure the [PostgreSQL `persistent` profile](POSTGRESQL.md) first: its shared JDBC sessions preserve pending OAuth state and authenticated users across instances and restarts. Enabling OAuth alone does not make H2 persistent.

For public production branding, provide accurate homepage/privacy links and follow Google's [branding requirements](https://support.google.com/cloud/answer/15549049?hl=en). Brand/domain verification and a successful real login are separate checks; neither is established by the mocked tests below.

## Existing Oracle installations

The checked-in demo seeds now contain BCrypt password hashes. Existing plaintext password records are deliberately **not** accepted. Before upgrading a separate existing database, plan password resets or an explicit offline migration that hashes its old values; ensure `personne.passwd` can hold at least 60 characters. There is no automatic plaintext fallback.

`car-rental-client/src/main/resources/db/manual/oidc-oracle.sql` is an additive table script for an existing Oracle schema. Review and run it separately before enabling Google against that schema. It has not been executed against any external database. Do not run the resetting demo schema against a persistent database.

## Required live check after configuration

1. Open the production login page and confirm the Google button appears.
2. Sign in with an allowed test account and complete Google's own consent screen.
3. Confirm the callback returns to Loc Auto, the profile opens, and a fictional booking can be created and returned.
4. Log out, sign in again with the same Google account, and confirm the same application account is used while its instance/database survives.
5. Check that cancelling consent returns to the login page without granting a session.

The automated Google tests use controlled token claims and inspect the authorization redirect. They do not prove a real Google callback works before these steps have been completed.

## Primary references

- [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)
- [OAuth 2.0 for web server applications](https://developers.google.com/identity/protocols/oauth2/web-server)
