# ShoppiList backend (Firestore)

The app's cloud pieces live in the Firebase project `shoppilist`:

| Collection | Purpose | Written by |
|---|---|---|
| `catalog/{region}/items/{id}` | Master item catalog per region (`IN`, `US`, `EU`) | seed script / admin approvals |
| `custom_items/{name-slug}` | Items users typed that aren't in the catalog (admin review queue) | the app (any signed-in user) |
| `users/{uid}` | Profile mirror (name, contact, address, last location) | the app (owner) |
| `admins/{uid}` | Marker docs granting the in-app admin dashboard | seed script / console only |

## One-time setup (Firebase console)

1. **Create the database**: Build → Firestore Database → Create database → production mode
   (pick a region close to your users, e.g. `asia-south1` for India).
2. **Rules**: Firestore Database → Rules → paste the contents of `firestore.rules` → Publish.
3. **Service account key** (for the seed script): Project settings → Service accounts →
   Generate new private key → save as `backend/service-account.json`.
   **Never commit this file** (it's gitignored).

## Seeding the catalog + granting admin

```bash
cd backend
npm install firebase-admin
node seed-firestore.mjs --admin-email prateek.gulati1@gmail.com
```

Notes:
- `--admin-email` requires that account to have signed in to the app at least once
  (it looks up the Firebase Auth user by email). Run the script again with the flag
  later if needed — catalog re-uploads are idempotent (doc ids are stable slugs).
- `--only in|us|eu` uploads a single region.

## Catalog files

`catalog/in.json`, `catalog/us.json`, `catalog/eu.json` — ~500 items per region, each:

```json
{ "id": "basmati-rice", "name": "basmati rice", "categoryId": "canned_packaged",
  "translations": { "hi": "बासमती चावल" }, "isSeasonal": false, "seasonMonths": [] }
```

`categoryId` must be one of the app's 14 stable category ids (see
`shared/.../data/local/seed/CategorySeedData.kt`). The app fetches its region's catalog
(GPS country → onboarding country → GLOBAL fallback), caches it in Room, and refreshes
at most once every 24 h. Items an admin approves in the in-app dashboard are added to
these collections directly — the JSON files here are only the initial seed.
