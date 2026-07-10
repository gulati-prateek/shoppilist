# ShoppiList Privacy Policy

_Last updated: [DATE — fill in before publishing]_

This policy describes what data ShoppiList ("the app") collects, why, and how it is
handled. It is written to support the Google Play Data Safety form and the Apple App Store
Privacy "nutrition label." **Before publishing**, replace the bracketed placeholders below
(contact email, hosting URL, company/developer name) and host this file at a public URL —
both stores require a live privacy policy link, not just a document.

## Data we collect

Your shopping data lives primarily in a local database on your device. The app also uses
**Firebase (Google Cloud)** for accounts and a small set of cloud features described below.

| Data | Why we collect it | Where it's stored |
|---|---|---|
| Account credentials (email + password, or phone number verified by SMS code) | Sign-in and account verification | Firebase Authentication (Google Cloud). Passwords are handled by Firebase; the app never stores them. |
| Profile (first/last name; optional email, address) | Personalizing the app and, for address, understanding roughly where our users are | Local database + a profile mirror in Firebase Cloud Firestore |
| Approximate/precise device location (only when you tap the location button and grant permission) | Showing your current city on the dashboard, autofilling your address, and choosing the right per-country item catalog | Locally, and as part of the profile mirror in Cloud Firestore |
| Shopping lists, items, categories, assignments | Core app functionality — this is the data you create | Local on-device database only |
| Item names you add that aren't in our catalog | Reviewed by the app's catalog administrators so popular items can be added to the shared master catalog for everyone | Cloud Firestore (item name, your user id/display name, and your country code) |
| List membership / collaborators | To support shared lists between household members | Local on-device database only |
| Voice input (microphone audio) | To let you add items by speaking instead of typing | Processed transiently by the Android system's speech recognition service (`SpeechRecognizer`/Google's on-device or cloud speech engine, depending on device settings) to convert speech to text. **The app itself does not record, store, or transmit raw audio** — it only receives the recognized text back from the OS. |
| Country / language preference | To localize categories, units, and suggestions | Local database + profile mirror in Cloud Firestore |

We do not collect payment information, contacts, or advertising identifiers, and the app
shows no third-party ads. Location is collected **only** when you explicitly tap the
location button and grant the permission — never in the background.

## Third-party services

- **Firebase Authentication & Cloud Firestore (Google LLC)** — account sign-in, the shared
  item catalog, your profile mirror, and the review queue for user-suggested items. Data is
  processed under [Google's privacy policy](https://policies.google.com/privacy) and the
  [Firebase data processing terms](https://firebase.google.com/terms/data-processing-terms).
- **Google Play services location** — used to obtain the device location when you request it.
- **Android system speech recognition** — invoked via the standard `RecognizerIntent`; audio
  handling is governed by the device's own speech-recognition provider (typically Google),
  not by this app.
- **Retailer links** — tapping "order online" for an item opens your browser to a
  third-party retailer's search page (e.g. Amazon, Flipkart); once you leave the app, that
  site's own privacy policy applies.

## Data retention and deletion

Your lists and items live in this app's local database on your device; uninstalling the app
deletes them. Your account, profile mirror, and any catalog suggestions you submitted are
stored in Firebase until deleted. To delete your account and associated cloud data, contact
us at **[contact email — fill in]** and we will remove them. [If you add an in-app "delete
my account" action, update this section accordingly.]

## Children's privacy

ShoppiList is not directed at children under 13 and does not knowingly collect data from
them.

## Permissions

- **Microphone (`RECORD_AUDIO`)** — only accessed when you explicitly tap the voice-input
  button; never runs in the background.
- **Location (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)** — only accessed when you
  tap the location button on the dashboard or the "use my current location" button on the
  profile form; never in the background.
- **Internet** — used for Firebase sign-in, syncing the item catalog and profile mirror, and
  opening retailer websites in your browser.

## Changes to this policy

We may update this policy as the app evolves. Material changes will be reflected here with
an updated "Last updated" date.

## Contact

Questions about this policy or your data: **[contact email — fill in]**
