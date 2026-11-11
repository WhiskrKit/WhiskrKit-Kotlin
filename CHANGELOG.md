# Changelog

All notable changes to WhiskrKit for Android are listed here. Versions follow
[Semantic Versioning](https://semver.org).

Add a line under **Unreleased** in each pull request. When releasing, rename
**Unreleased** to the new version and date, and start a new empty section.

## Unreleased

## 0.2.0 - 2026-11-10

### Added
- A small "Powered by WhiskrKit" badge below the submit button of sheets and
  banners, shown only when the server asks for it (`branding.visible`). A missing
  or malformed `branding` hides it and never affects the survey. The badge uses
  the theme's secondary text color, moved toward black or white just enough
  when that would be under 4.5:1 contrast, scales with the font size and is read by
  TalkBack as one element after the submit button. Full-screen forms and banners
  with only follow-up buttons don't show it.
- Mocked surveys (`withMockedSurveys = true`) always show the badge on sheets and
  banners, so it can be previewed in both styles and themes.
- Global survey attributes: `WhiskrKit.setAttribute`, `setAttributes`,
  `removeAttribute` and `clearAttributes` attach a string, number or boolean to
  every survey response for segmentation. Personal data is dropped by the
  server and must not be sent.
- Translations of all built-in strings and TalkBack labels into Dutch, German,
  French, Spanish, Italian, Portuguese, Japanese, Korean and Simplified
  Chinese.
- WhiskrKit's own strings follow the language a survey was served in when the
  survey response includes a `language` field. Without it, they follow the
  app's language as before.
- Store review follow-up: a star question with the follow-up switched on in the
  dashboard (`opensStoreReview`) asks for the Google Play in-app review after the
  survey was submitted successfully with 4 or 5 stars, at most once per app
  version, shortly after the survey is dismissed. Closing without submitting or a
  failed (queued) submission never asks. Google Play decides whether the prompt
  appears.
- New optional artifact `eu.whiskrkit:whiskrkit-android-review` with
  `PlayReviewRequester`, passed as
  `WhiskrKit.initialize(..., reviewRequester = PlayReviewRequester())`. The core
  library gains no Google Play dependency; without the add-on a qualifying rating
  is only logged. Custom stores can implement `ReviewRequester`.
- Mocked surveys: the `feature-feedback` star survey has the follow-up switched
  on and logs the review request instead of calling Google Play.

### Changed
- The star rating's TalkBack labels and its required message use plural
  forms ("1 star", "5 stars") in every language.
- Removed the unused strings `whiskrkit_a11y_score`, `whiskrkit_a11y_x_out_of_y`
  and `whiskrkit_a11y_text_max`. Apps overriding them can drop the overrides.
- `whiskrkit_a11y_star` and `whiskrkit_required_symbol` are now plurals
  resources. Apps overriding them need to override the plurals instead.

## 0.1.4 and earlier

See the [releases on GitHub](https://github.com/WhiskrKit/WhiskrKit-Kotlin/releases).
