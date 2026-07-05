# ShoppiList Privacy Policy

_Last updated: [DATE — fill in before publishing]_

This policy describes what data ShoppiList ("the app") collects, why, and how it is
handled. It is written to support the Google Play Data Safety form and the Apple App Store
Privacy "nutrition label." **Before publishing**, replace the bracketed placeholders below
(contact email, hosting URL, company/developer name) and host this file at a public URL —
both stores require a live privacy policy link, not just a document.

## Data we collect

**All app data is stored locally on your device only.** ShoppiList does not have a cloud
backend or account server — there is no data collection or transmission to us or any
third-party server.

| Data | Why we collect it | Where it's stored |
|---|---|---|
| Account identifier / name / contact info | To identify you locally and let you share lists with other people on the same device/household | Local on-device database only |
| Shopping lists, items, categories, assignments | Core app functionality — this is the data you create | Local on-device database only |
| List membership / collaborators | To support shared lists between household members | Local on-device database only |
| Voice input (microphone audio) | To let you add items by speaking instead of typing | Processed transiently by the Android system's speech recognition service (`SpeechRecognizer`/Google's on-device or cloud speech engine, depending on device settings) to convert speech to text. **The app itself does not record, store, or transmit raw audio** — it only receives the recognized text back from the OS. |
| Country / language preference | To localize categories, units, and suggestions | Local on-device database only |

We do not collect payment information, precise location, contacts, or advertising
identifiers, and the app currently shows no third-party ads. Nothing above is transmitted
off your device by this app.

## Third-party services

- **Android system speech recognition** — invoked via the standard `RecognizerIntent`; audio
  handling is governed by the device's own speech-recognition provider (typically Google),
  not by this app.
- **Retailer links** — tapping "order online" for an item opens your browser to a
  third-party retailer's search page (e.g. Amazon, Flipkart); once you leave the app, that
  site's own privacy policy applies.

## Data retention and deletion

All your data lives in this app's local database on your device. Uninstalling the app
deletes it. [If you add a real in-app "delete my data" action or ever introduce cloud sync,
update this section accordingly before publishing.]

## Children's privacy

ShoppiList is not directed at children under 13 and does not knowingly collect data from
them.

## Permissions

- **Microphone (`RECORD_AUDIO`)** — only accessed when you explicitly tap the voice-input
  button; never runs in the background.
- **Internet** — only used to open a retailer's website in your browser when you tap
  "order online" for an item; the app has no server of its own to talk to.

## Changes to this policy

We may update this policy as the app evolves. Material changes will be reflected here with
an updated "Last updated" date.

## Contact

Questions about this policy or your data: **[contact email — fill in]**
