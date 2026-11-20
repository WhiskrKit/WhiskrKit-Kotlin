# Changelog

All notable changes to WhiskrKit for Android are listed here. Versions follow
[Semantic Versioning](https://semver.org).

Add a line under **Unreleased** in each pull request. When releasing, rename
**Unreleased** to the new version and date, and start a new empty section.

## Unreleased

## 0.3.0 (not yet released)

### Added
- Rating metrics for scale questions. The server sends a `metric` (`nps`,
  `csat`, `ces` or `custom`), a `displayStyle` (`numbers` or `stars`) and, for
  `custom`, optional `minLabel` and `maxLabel`. The server calculates the
  metric; the SDK renders the question and sends the chosen number, under the
  same `npsRating` key as before. All fields are optional: a survey from an
  older server shows as `custom` with numbers. Unknown, null or wrongly typed
  values fall back to `custom` and `numbers` instead of failing the survey.
- Selected scores are colored per metric: NPS 0–6 red, 7–8 dark gold, 9–10
  green; CSAT 4–5 green; CSAT 1–3, CES and custom a neutral blue. The colors are
  fixed (not themeable), meet WCAG contrast in light and dark mode, and carry
  black text. A 2dp outline marks the selected score as well, so the selection
  never relies on color alone.
- End labels in one line under the scale: translated defaults for NPS ("Not at
  all likely" / "Extremely likely"), CSAT ("Very dissatisfied" / "Very
  satisfied") and CES ("Very difficult" / "Very easy") in all ten languages, and
  the server's labels for `custom`. TalkBack reads them with every button, for
  example "8 out of 10. 0 is Not at all likely, 10 is Extremely likely".
- Five stars for `displayStyle: stars` with a 1 to 5 range; other ranges show
  numbers. Tapping the chosen star again clears it.
- Every request sends `X-WhiskrKit-SDK: android/<version>`, the same header iOS
  sends, so the server can handle compatibility.
- The sample Playground has a "Rating metrics" section with an NPS, CSAT, CSAT
  stars, CES and custom rating survey. The `nps-survey` mock now uses the NPS
  metric.
- Banners can style their own title and description: set `title` and
  `description` on `WhiskrKitTheme.BannerTheme`. Without them, banners look as
  before (the `headline` and `subheadline`, the same as the question title).

### Changed
- Scale questions without a metric show the neutral blue for the selected score
  instead of the red to green gradient.
- Score buttons grow with the font scale (never smaller than 48dp) and wrap into
  even rows when they don't fit, instead of a flow layout that could leave one
  button on the last row.

### Fixed
- Nothing to fix for deselecting: unlike iOS, Android already removed a
  deselected scale rating from the response. Tests now cover it.

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
