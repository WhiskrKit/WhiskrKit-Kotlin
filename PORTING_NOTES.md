# iOS parity & porting notes

**Audience: WhiskrKit maintainers, not SDK users.** The Android sources contain no
references to the iOS SDK — Android developers shouldn't need to know or care that a
sibling SDK exists. All cross-platform context (how a declaration relates to its iOS
counterpart, and which decision in the iOS repo's `ANDROID_PLAN.md` it implements)
lives here instead, keyed by file and declaration.

When changing one of the declarations below, check whether the iOS SDK needs the same
change — and vice versa. Wire-format entries are the critical ones: both SDKs must
stay byte-compatible with the same backend.

---

## Wire format & data models

### `core/serialization/WireJson.kt`

| Declaration | Parity note |
|---|---|
| `WireJson` | `explicitNulls = false` omits null fields, matching the iOS SDK's `encodeIfPresent` behaviour. |
| `IsoInstantSerializer` | Matches Swift's `JSONEncoder.dateEncodingStrategy = .iso8601`: `2026-06-11T12:00:00Z`, second precision, UTC. |

### `core/model/QuestionTemplate.kt`

| Declaration | Parity note |
|---|---|
| `QuestionTemplate` | Mirrors the iOS `SurveyPresentation.SurveyBase` union. The inconsistent `A11yLabel` / `a11yHint` JSON key casing replicates the backend contract that the iOS models defined (decision P3: match byte-for-byte, no normalisation). |
| `SymbolRatingTemplate.opensStoreReview` | Decoded but unimplemented on iOS too (decisions P5 / #8). When implemented, Android uses Play In-App Review in a separate `whiskrkit-play` artifact. |
| `UnknownQuestionTemplate` | Lenient decoding is an Android addition (decision B1) — iOS fails the whole template on unknown question types. |

### `core/model/SurveyTemplate.kt`

| Declaration | Parity note |
|---|---|
| `BannerTemplate` | iOS calls this presentation "toast" (`ToastTemplate`); the wire value stays `toast` (decision #3 renamed only the Android-facing concept). |
| `SurveyTemplate.validated()` | Implements decision B1 (skip unknown optional questions; unknown required question invalidates the template). iOS has no equivalent. |

### `core/model/SurveyResponse.kt`

| Declaration | Parity note |
|---|---|
| `SurveyResponse` / `SurveyAnswerSerializer` | Wire format must match the iOS `SurveyResponse.SurveyType` encoding exactly: single-key tagged objects (`{"npsRating": 7}`), `ThumbsRating` as raw strings `thumbsUp`/`thumbsDown`/`none`. |

## Core services

### `core/eligibility/EligibilityModels.kt`

| Declaration | Parity note |
|---|---|
| `SurveyEligibilityContext` | ISO-8601 dates match the iOS SDK's `JSONEncoder.dateEncodingStrategy = .iso8601`. |

### `core/eligibility/EligibilityStorage.kt`

| Declaration | Parity note |
|---|---|
| `EligibilityStorage` | Mirrors the iOS `EligibilityStorage` protocol. |
| `deviceId` | Self-generated UUID (decision #16). iOS uses *two* identifiers (`identifierForVendor` for the header, a generated UUID for the eligibility context); Android collapses both into one. |

### `core/eligibility/EligibilityService.kt`

| Declaration | Parity note |
|---|---|
| `WhiskrKitEligibilityService` | Direct port of the iOS `WhiskrKitEligibilityService`: in-flight dedup, `nextCheckAfter` cache, `removeFromHistory`, silent failure. |

### `core/network/WhiskrKitException.kt`

| Declaration | Parity note |
|---|---|
| `WhiskrKitException` | Mirrors the iOS `WhiskrKitError` cases; `isRetryable` mirrors the iOS retry policy (429, 5xx, transport). |

### `core/network/NetworkService.kt`

| Declaration | Parity note |
|---|---|
| `SurveyApi` | Android-only seam. The iOS retry coordinator depends on the concrete `NetworkService`; this interface replaces that for testability. |
| `NetworkService` | Port of the iOS `NetworkService`: same endpoints, 30 s timeouts, max 2 retries with 1 s/2 s exponential backoff, same status-code mapping. The iOS `submitRating` also took an `identifier` parameter that was always `""` and never reached the wire — dropped on Android (decision B4). |

### `internal/DeviceInfo.kt`

| Declaration | Parity note |
|---|---|
| `DeviceInfo` | Android counterpart of the iOS `addCommonHeaders` values. |
| `deviceIdentifier` | In the spirit of iOS's hardware model string ("iPhone14,2"); Android sends `MANUFACTURER MODEL` (decision B3, pending backend confirmation). |

### `internal/WhiskrLog.kt`

| Declaration | Parity note |
|---|---|
| `WhiskrLog` | Categories mirror the iOS OSLog categories (Core / UI / Networking / Cache). |

## Submission queue

### `core/queue/PendingSubmission.kt`

| Declaration | Parity note |
|---|---|
| `PendingSubmission` | Same fields and retry policy as the iOS struct; immutable with `withRetryAttempt` where the iOS struct mutates in place. Config constants (max 5 queued, max 5 retries, 5 min throttle, 7 day expiry) match iOS `SubmissionQueueConfig`. |

### `core/queue/SubmissionQueue.kt`

| Declaration | Parity note |
|---|---|
| `SubmissionQueue` | Ported from iOS, including dedup-by-surveyId and oldest-first eviction. Main-scope confinement mirrors the iOS `@MainActor` isolation. |

### `core/queue/SubmissionRetryCoordinator.kt`

| Declaration | Parity note |
|---|---|
| `SubmissionRetryCoordinator` | Ported from iOS. iOS registers the foreground observer inside the coordinator's init; Android registers lifecycle/connectivity triggers in `WhiskrKit.initialize` instead. The connectivity-restore trigger is Android-only (decision #19). |

## Entry point & trigger system

### `WhiskrKit.kt`

| Declaration | Parity note |
|---|---|
| `pendingSurvey` | Replaces the iOS `pendingSurveyId: String?` + `onChange` pattern (decision #4, amended). Deliberate divergence: a `present()` call before any host is composed is buffered and delivered to the first host (iOS drops it). |
| `autoCheckedIdentifiers` / `autoCheckAndPresent` | Per-process dedup of auto checks (decision #6) — Android-only; iOS re-checks on every view appearance. Applies to the auto path only, never `checkAndPresent`. |
| session counting in `registerLifecycleObservers` | Sessions = app foreground via `ProcessLifecycleOwner` (decision #5); iOS counts one session per `initialize()` (process launch). The `ON_START` retry is the analog of iOS's `willEnterForegroundNotification` observer. |

### `core/PendingSurveyRequest.kt`

| Declaration | Parity note |
|---|---|
| `PendingSurveyRequest.Present` | Carries the template from the eligibility response so it isn't fetched twice (decision P1 — fixes an iOS double-fetch; the same fix is planned for iOS). |

### `core/MockConfigurationService.kt`

| Declaration | Parity note |
|---|---|
| `MockConfigurationService` | Mirrors the iOS `MockConfigurationService` (template set approximates the iOS mock identifiers). |
| `MockEligibilityService` | Always-eligible, like iOS (decision P6: replicate). |

## Theme

### `theme/WhiskrKitTheme.kt`

| Declaration | Parity note |
|---|---|
| `WhiskrKitTheme` | Same concepts and field names as the iOS `WhiskrKitTheme` for cross-platform documentation consistency, but all fields optional with Material-derived defaults (decision #10, hybrid). iOS has no Material fallback and a required `setTheme()` call; Android theming is host-parameter only (decision #11, amended: `setTheme()` dropped). |
| `BannerTheme` | iOS calls this `ContainerTheme.Toast` ("toast" terminology). |
| `ButtonAppearance.Custom` | Compose counterpart of the iOS custom `ButtonStyle` escape hatch (decision #12). |
| `ButtonVariant` | The iOS `ButtonVariant.init` accepts a `size: ButtonSize` parameter that is never stored (dead API); intentionally absent here (decision P2). |

### `theme/ResolvedTheme.kt`

| Declaration | Parity note |
|---|---|
| `LocalWhiskrKitTheme` | Internal per decision #13 (iOS exposes its environment key publicly; Android deliberately does not — can be made public later without breaking). |

## UI

### `ui/WhiskrKitHost.kt`

| Declaration | Parity note |
|---|---|
| `WhiskrKitHost` | Compose counterpart of the iOS `.whiskrKit()` view modifier. |
| `WhiskrKitSurvey` | Compose counterpart of the iOS `.whiskrKitSurvey(identifier:)` modifier. |
| `SurveyPresenter` | Saveable active-template state implements decision #9 (rotation/process-death survival) — no iOS equivalent needed. |

### `ui/container/SheetContainer.kt`

| Declaration | Parity note |
|---|---|
| `SheetContainer` | M3 `ModalBottomSheet` per decision #1; replaces the iOS `ContentHeightSheet` detent workaround (content-height wrapping is native on Android). |
| `SheetContent` (`canSubmit`) | iOS parity rule: the sheet's main question must be answered to submit *regardless of `isRequired`* (matches `SheetContainerView.canSubmit`). |
| follow-up question | Same synthesized template as iOS: id `"<sheetId>-followUp"`, optional, appears once any answer exists. |

### `ui/container/FullScreenContainer.kt`

| Declaration | Parity note |
|---|---|
| `FullScreenContainer` | Compose `Dialog` with `usePlatformDefaultWidth = false` per decision #2 (the iOS counterpart is `fullScreenCover` + `NavigationStack`). Back-press dismisses without submitting (decision A2 — equivalent to iOS swipe-dismiss). |

### `ui/container/BannerView.kt`

| Declaration | Parity note |
|---|---|
| `BannerHost` / `BannerContent` | iOS `ToastView` counterpart (decision #3: "banner" naming). Overlay-in-host-Box approach per decision #1-toast/A; swipe-down threshold replaces the iOS 50 pt drag threshold. |

### `ui/container/Savers.kt`

| Declaration | Parity note |
|---|---|
| `SurveyTemplateSaver` / `SurveyResponseSaver` | Implements decision #9. No iOS equivalent (no rotation-recreate on iOS). |

### `ui/question/QuestionView.kt`

| Declaration | Parity note |
|---|---|
| `QuestionView` | Compose counterpart of the iOS `TemplateViewBuilder`. |

### `ui/question/QuestionContainer.kt`

| Declaration | Parity note |
|---|---|
| `QuestionContainer` | Port of the iOS `RatingContainerView`; `FlowRow` replaces the iOS `ViewThatFits` for the title/required-tag wrapping. |

### `ui/question/ScaleRatingQuestion.kt`

| Declaration | Parity note |
|---|---|
| `ScaleRatingQuestion` | Adaptive `FlowRow` instead of the iOS fixed two-row split (decision #7). Deselecting removes the answer from the response — a deliberate fix: iOS keeps the stale value in `results` after deselection. Color interpolation matches the iOS `interpolatedColor` math. |

### `ui/question/SymbolRatingQuestion.kt`

| Declaration | Parity note |
|---|---|
| `SymbolRatingQuestion` | `opensStoreReview` decode-and-ignore matches iOS (decision P5). |

### `ui/question/TextFeedbackQuestion.kt`

| Declaration | Parity note |
|---|---|
| `TextFeedbackQuestion` | Truncation-based character cap, 200 default, 80%/90% counter thresholds — all iOS parity. |

### `ui/question/MultipleChoiceQuestion.kt`

| Declaration | Parity note |
|---|---|
| `MultipleChoiceQuestion` | Selection stored as option *ids* (wire parity with iOS). Uses native `selectable`/`toggleable` semantics instead of the manual accessibility announcements the iOS version posts. |

## Manifest & build

| Location | Parity note |
|---|---|
| `whiskrkit/src/main/AndroidManifest.xml` (`ACCESS_NETWORK_STATE`) | Supports the Android-only connectivity-restore retry trigger (decision #19). |
| `sample/build.gradle.kts` (release `minifyEnabled`) | Verifies consumer R8 rules per decision A6. |

## Tests

| Location | Parity note |
|---|---|
| `SurveyResponseSerializationTest` | Asserts the exact wire strings the iOS `SurveyResponse` encoder produces. |
| `SubmissionQueueTest` / `SubmissionRetryCoordinatorTest` / `EligibilityServiceTest` | Port the intent of the iOS test suite (`Tests/WhiskrTests/`). |
| `SnapshotTest` | Scenario names correspond to the iOS `SnapshotTests.swift` suite for side-by-side review. Uses Roborazzi rather than Paparazzi (decision #21, amended: Paparazzi 2.0 alphas failed layoutlib init against this toolchain). |
| `ArchitectureTest` | Enforces the core/ui split from decision #20 (future module split). |
