# Changelog

All notable changes to WhiskrKit for Android are listed here. Versions follow
[Semantic Versioning](https://semver.org).

Add a line under **Unreleased** in each pull request. When releasing, rename
**Unreleased** to the new version and date, and start a new empty section.

## Unreleased

### Added
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
- The star rating's TalkBack labels and its required message use plural
  forms ("1 star", "5 stars") in every language.
- Removed the unused strings `whiskrkit_a11y_score`, `whiskrkit_a11y_x_out_of_y`
  and `whiskrkit_a11y_text_max`. Apps overriding them can drop the overrides.
- `whiskrkit_a11y_star` and `whiskrkit_required_symbol` are now plurals
  resources. Apps overriding them need to override the plurals instead.

## 0.1.4 and earlier

See the [releases on GitHub](https://github.com/WhiskrKit/WhiskrKit-Kotlin/releases).
