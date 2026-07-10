# ShoppiList — Round 2 requirements (filed 2026-07-10)

Captured verbatim-intent from the product owner. Several registration items overlap the already-
approved auth-flow plan (`~/.claude/plans/tender-twirling-lobster.md`, Parts A–E). Grouped by area.

## Registration page  (mostly = approved plan Parts A/B — confirms behaviour)
- R1. States populate in a list per the selected country.
- R2. Cities populate once a State is selected; the City field stays DISABLED until a state is picked.
- R3. PIN/ZIP is a free-text box validated by a country-specific rule.

## Dashboard screen
- D1. Show the Location at the top, just below the "ShoppiList" title (move LocationChipRow up).
- D2. Add an **Edit** option to each list's 3-dot action menu that opens list edit. Today editing
      happens by tapping the list — tapping should OPEN the list; edit should be via the menu.
      (Phase 1 added a "Rename" menu item — expand it to a full Edit: name/color/description.)

## Profile section
- P1. REMOVE the "Hide Sponsored Links" toggle from Profile.
- P2. Allow editing the profile (name) along with address AND country. (= approved plan Part D.)

## Inside a List (ListDetailScreen)
- L1. The add-item field ("tap to add") must ALWAYS be available (currently missing while editing) so
      the list can be extended anytime.
- L2. Provide an option to move items from Cart (checked) back to the To-Get section.
- L3/L8. Selecting an item in To-Get (or Select-All on the top bar) must NOT immediately move it to
      Cart. Add an explicit second step ("Move to Cart" action) after selection.
- L4. Add the ability to ASSIGN an item to a particular person on the list.
- L5. Show who is contributing to this list (the members/contributors roster).
- L6. "Add People" currently appears twice — keep it ONCE, as an icon on the top bar only.
- L7. The (removed) second "Add People" spot becomes an **"Order Online"** button/link that opens the
      Order Online Dashboard.
- L9. The Order Online Dashboard shows affiliate links to quick-commerce and e-commerce sites.
- L10. Use CORRECT vendor icons for the e-commerce / quick-commerce sites.
- L11. Re-check item icons in the list; if the matched icon isn't appropriate, show a BLANK icon
       instead of a wrong/generic one.

## Categories section
- C1. Clicking a category must SCROLL to that category's items in the list, not jump to the top.

## Admin panel
- A1. Add affiliate-links configuration in the admin section.
- A2. Each vendor's affiliate program can be toggled ON/OFF (starts/closes anytime).
- A3. A SEPARATE admin login (a specific admin login id) opens the admin panel; it must NOT be visible
      to normal users.
- A4. Admins can add / edit / remove items in a category.

## Notes / open design questions to resolve at implementation time
- L7 wording ("Convert Add People to a link to Order Online button") is a bit ambiguous — interpret as:
  remove the duplicate Add-People affordance and replace that spot with an "Order Online" button.
- A3 separate-admin-login: today admin is gated by a Firestore `admins/{uid}` marker on the normal
  account. "Separate login id" may mean a dedicated admin sign-in entry (hidden route) — confirm scope.
- Vendor icons (L10): no bundled vendor logos exist; likely emoji/initial placeholders or bundled
  small assets — decide per vendor.
