# ShoppiList — Google Play Store Listing Draft

Draft copy for the Play Console store listing. Review/edit before pasting into Play Console —
character limits are Play's actual current limits (double-check if they change).

## App name

**ShoppiList**

## Short description (max 80 characters)

```
Shared shopping lists with voice input, smart categories, and suggestions.
```
(75 characters)

## Full description (max 4000 characters)

```
ShoppiList is a shopping list app built for households and groups who shop together.

SHARE LISTS, NOT CONFUSION
Invite people to your list as an Owner, Editor, or Viewer. See who's actively looking at
a list right now, and who's picking up what with per-item assignments — no more "wait,
did you already get the milk?"

ADD ITEMS YOUR WAY
Type it, or just say it — ShoppiList understands voice commands like "add milk and eggs
to my weekly list." Items are automatically sorted into the right category and aisle, so
your list matches how the store is laid out.

NEVER FORGET THE USUAL
ShoppiList notices what you buy regularly and gently reminds you when something's missing
from your active lists — before you're standing in the kitchen without it.

SPLIT AND MERGE LISTS
Done shopping but a few items didn't make it into the cart? Turn the leftovers into a new
list automatically. Planning a big trip? Merge multiple lists into one.

SHOP ONLINE TOO
When you'd rather order than walk the aisles, ShoppiList can point you to online retailer
options for individual items or your whole list.

WORKS IN YOUR LANGUAGE
ShoppiList is available in English, Hindi, Arabic, Spanish, French, German, Portuguese,
and Indonesian, with country-aware category ordering.

YOUR DATA STAYS ON YOUR DEVICE
ShoppiList has no cloud backend and no account server — everything is stored locally on
your device. There's no ad tracking and no data collection beyond what's needed for the
app to function on your device. See our privacy policy for full details.

Whether it's a weekly grocery run for the household or a big trip for a group event,
ShoppiList keeps everyone on the same list.
```
(~1,550 characters — well under the 4000 limit; can be expanded with more feature detail if desired)

## Category

**Shopping** (primary). Alternative if Play doesn't like "Shopping" for a list-management
app with no in-app purchases: **Productivity**.

## Tags / keywords (for ASO, not a formal Play field but useful for the description)

shopping list, grocery list, family shopping, shared list, voice shopping list,
collaborative list, grocery app

## Content rating questionnaire — likely answers

Play's questionnaire is dynamic (IARC-based) but based on the app's actual content, expect:

- Violence: None
- Sexual content: None
- Profanity: None
- Controlled substances: None
- Gambling: None
- User-generated content shared with others: **Yes** — list names/items/member names are
  visible to other users you invite to a shared list. Flag this honestly; it typically
  doesn't raise the rating on its own for text-only, non-public content, but the
  questionnaire will ask.
- Location sharing: No
- Personal info shared with third parties: No (see Data Safety section below)
- Digital purchases: No (no IAP, no ads)

Expected outcome: **Everyone** or **Everyone 10+** rating tier across the major rating
boards (ESRB/PEGI/USK/etc.), typical for a utility app with only private user-generated
list content and no monetization.

## Data Safety form — draft answers

Matches `PRIVACY_POLICY.md`. Fill this into Play Console's Data Safety section.

**Does your app collect or share any of the required user data types?**
Yes — collected. Account/profile/catalog data is processed by Firebase (Google Cloud) as a
service provider; it is not shared with third parties for their own purposes.

| Data type | Collected? | Shared with third parties? | Purpose | Optional? |
|---|---|---|---|---|
| Name (first/last on the profile form) | Yes | No (processed by Firebase as service provider) | App functionality, account management | First name required at sign-up; last name optional |
| Email address / phone number | Yes (sign-in credential; email optional for phone users) | No (Firebase Authentication) | Account management, verification | One of the two is required |
| Address | Yes, if entered (profile form) | No (Firestore profile mirror) | App functionality (user demographics) | Yes |
| Approximate & precise location | Yes, only when the user taps the location button and grants permission | No (Firestore profile mirror) | App functionality (dashboard location, per-country catalog) | Yes |
| App activity — in-app actions (list/item data you create) | Lists/items stay on-device. Item names not found in the master catalog are uploaded for admin review (with user id/display name + country) | No | App functionality (growing the shared item catalog) | No — core purpose |
| Audio — voice input | Processed transiently by the OS speech recognizer, not stored or transmitted by the app itself | No | App functionality (voice-to-text for adding items) | Yes — voice input is optional, typing works too |

**Is all user data encrypted in transit?**
Yes — all Firebase traffic (Authentication, Firestore) is HTTPS/TLS. Retailer links open in
the device browser over that site's own HTTPS connection.

**Do you provide a way for users to request data deletion?**
Yes — uninstalling deletes all local data; account + cloud profile deletion is available on
request via the privacy-policy contact email. (Play increasingly expects an account-deletion
web link or in-app flow for apps with account creation — plan an in-app "delete account"
before wide release.)

**Data collection is required for app functionality?**
An account (email or phone) is required. Profile last name, email (for phone users),
address, and location are optional; voice input is optional (typing works too).

## Screenshots / feature graphic — not yet produced

Needs a device/emulator to capture real screenshots (Home, ListDetail, Voice, Assignments
screens are the strongest candidates) and a 1024×500 feature graphic. Deferred until a
device/emulator is available for a real smoke test — see PRIVACY_POLICY.md and the memory
notes for this project's status.

## Still needs your input before submission

- Contact email + hosting URL for `PRIVACY_POLICY.md` (deferred per your request)
- Play Console developer account ($25 one-time, your own action)
- Final call on category (Shopping vs. Productivity)
- Real screenshots once a device/emulator smoke test happens
