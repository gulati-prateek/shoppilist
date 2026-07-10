#!/usr/bin/env node
/**
 * Seeds the ShoppiList Firestore backend:
 *   - uploads backend/catalog/{in,us,eu}.json into catalog/{REGION}/items/{id}
 *   - optionally grants admin rights: --admin-email you@example.com
 *
 * Setup (one time):
 *   1. Firebase console -> Project settings -> Service accounts -> "Generate new private key"
 *      Save the downloaded file as backend/service-account.json (gitignored — NEVER commit it).
 *   2. cd backend && npm install firebase-admin
 *
 * Run:
 *   node seed-firestore.mjs                          # upload all three catalogs
 *   node seed-firestore.mjs --only in                # just one region
 *   node seed-firestore.mjs --admin-email you@x.com  # also create admins/{uid} for that user
 *      (the user must have signed in to the app at least once so the account exists)
 */
import { readFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";

const here = dirname(fileURLToPath(import.meta.url));
const keyPath = join(here, "service-account.json");
if (!existsSync(keyPath)) {
  console.error(
    "Missing backend/service-account.json — download it from Firebase console:\n" +
      "Project settings -> Service accounts -> Generate new private key"
  );
  process.exit(1);
}

initializeApp({ credential: cert(JSON.parse(readFileSync(keyPath, "utf8"))) });
const db = getFirestore();

const REGIONS = { in: "IN", us: "US", eu: "EU" };
const VALID_CATEGORIES = new Set([
  "fresh_produce", "meat_seafood", "dairy_eggs", "bakery_bread", "frozen_foods",
  "canned_packaged", "personal_care", "household_cleaning", "beverages",
  "snacks_confectionery", "baby_kids", "pet_care", "health_pharmacy", "spices_condiments",
]);

const args = process.argv.slice(2);
function argValue(flag) {
  const i = args.indexOf(flag);
  return i >= 0 ? args[i + 1] : undefined;
}
const only = argValue("--only");
const adminEmail = argValue("--admin-email");

async function seedRegion(fileKey, region) {
  const file = join(here, "catalog", `${fileKey}.json`);
  const items = JSON.parse(readFileSync(file, "utf8"));
  const seen = new Set();
  for (const item of items) {
    if (!item.id || !item.name || !VALID_CATEGORIES.has(item.categoryId)) {
      throw new Error(`${fileKey}.json: invalid item ${JSON.stringify(item).slice(0, 120)}`);
    }
    if (seen.has(item.id)) throw new Error(`${fileKey}.json: duplicate id "${item.id}"`);
    seen.add(item.id);
  }

  // Firestore batches cap at 500 writes.
  const collection = db.collection("catalog").doc(region).collection("items");
  let batch = db.batch();
  let inBatch = 0;
  let written = 0;
  for (const item of items) {
    batch.set(collection.doc(item.id), {
      name: item.name.toLowerCase().trim(),
      categoryId: item.categoryId,
      translations: item.translations ?? {},
      isSeasonal: item.isSeasonal ?? false,
      seasonMonths: item.seasonMonths ?? [],
    });
    inBatch++;
    written++;
    if (inBatch === 450) {
      await batch.commit();
      batch = db.batch();
      inBatch = 0;
    }
  }
  if (inBatch > 0) await batch.commit();
  console.log(`catalog/${region}: ${written} items uploaded`);
}

async function grantAdmin(email) {
  const user = await getAuth().getUserByEmail(email);
  await db.collection("admins").doc(user.uid).set({ email, grantedAt: Date.now() });
  console.log(`admins/${user.uid} created for ${email}`);
}

for (const [fileKey, region] of Object.entries(REGIONS)) {
  if (only && only !== fileKey) continue;
  await seedRegion(fileKey, region);
}
if (adminEmail) await grantAdmin(adminEmail);
console.log("Done.");
