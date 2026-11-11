# Changelog

All notable changes to WhiskrKit for Android are listed here. Versions follow
[Semantic Versioning](https://semver.org).

Add a line under **Unreleased** in each pull request. When releasing, rename
**Unreleased** to the new version and date, and start a new empty section.

## Unreleased

## 0.2.0 - 2026-11-11

### Added
- `WhiskrKit.setOnSurveyCompletedListener`: called on the main thread when the
  user submits a survey, with the survey id and the submitted answers (question
  id, question type and value). It fires whether or not the response reaches the
  server, and not when a survey is dismissed without submitting.
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

### Changed
- The minimum Android version is now 7.0 (API 24, was 26). The SDK no longer
  uses `java.time` internally; the public API, the wire format and stored data
  are unchanged.
- The SDK needs Jetpack Compose 1.10 instead of 1.11, so apps on Compose 1.10
  no longer have to upgrade.
- The star rating's TalkBack labels and its required message use plural
  forms ("1 star", "5 stars") in every language.
- Removed the unused strings `whiskrkit_a11y_score`, `whiskrkit_a11y_x_out_of_y`
  and `whiskrkit_a11y_text_max`. Apps overriding them can drop the overrides.
- `whiskrkit_a11y_star` and `whiskrkit_required_symbol` are now plurals
  resources. Apps overriding them need to override the plurals instead.

## 0.1.4 and earlier

See the [releases on GitHub](https://github.com/WhiskrKit/WhiskrKit-Kotlin/releases).
