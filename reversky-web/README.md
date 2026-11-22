# Reversky Web

Next.js app for the public entry page, authentication flows, and the first signed-in Home shell.

## Local setup

1. Copy `.env.example` to `.env.local` in this folder.
2. Start `reversky-api` on port `8080`.
3. Start the web app from this folder:

   ```bash
   npm install
   npm run dev
   ```

Open [http://localhost:3000](http://localhost:3000).

Set `REVERSKY_API_URL` to the Spring API origin when it is not `http://localhost:8080`.

## Pages

- `/` — public entry and registration (P01)
- `/login` — sign in (P02)
- `/forgot-password` and `/reset-password` — password recovery (P03)
- `/verify-email` — email verification link target
- `/home` — signed-in Home shell (G01)

Authentication calls go through Next route handlers under `/api/auth`. Access and refresh tokens are held in HttpOnly cookies; browser code only receives the authenticated profile. The route handler rotates refresh tokens and retries profile/resend-verification calls after an expired access token.

Registration follows the API contract: after `POST /auth/register` succeeds, the web app makes a normal login request with the submitted credentials so first entry can continue to Home. If that login request is unavailable, the account-created state routes to Login.

The Home shell currently uses only `GET /api/v1/users/me` and the email resend endpoint. Catalog, inventory, deck, challenge, and trade data are not connected here yet; the section cards are marked as coming soon. No game-engine calls are made from this app.
