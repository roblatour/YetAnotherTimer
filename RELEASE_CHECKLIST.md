# Play Store Release Checklist (Required Steps Only)

This checklist covers the mandatory steps to produce and publish a signed Android App Bundle (.aab) to the Google Play Console. Optional automation or enhancements have been intentionally omitted.

---
## 1. Prep Environment
1. Ensure Android SDK command-line tools installed and on PATH (sdkmanager, apksigner, bundletool if you need local testing).
2. Confirm Java 17 (matches Gradle config).

## 2. Versioning
1. Edit `gradle.properties`: update `VERSION_CODE` (increment by 1) and `VERSION_NAME` (semantic version).
2. (Optional helper) Run: `./scripts/bump-version.ps1 -NewName 1.1.0`.
3. Verify: `./gradlew.bat printAppVersion`.

## 3. Upload Keystore (Play App Signing Model)
1. Generate (only once):
   ```powershell
   keytool -genkeypair -v -keystore e:\secure\android_keys\upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Back up the keystore + password in a secure location (password manager + offline copy).
3. Add (NOT committed) to user or project `gradle.properties` (or set env vars):
   ```properties
   RELEASE_STORE_FILE=e:/secure/android_keys/upload-keystore.jks
   RELEASE_STORE_PASSWORD=********
   RELEASE_KEY_ALIAS=upload
   RELEASE_KEY_PASSWORD=********
   ```

## 4. Build Signed Release Bundle
1. Clean (optional): `./gradlew.bat clean`.
2. Build bundle: `./gradlew.bat bundleRelease`.
3. Output: `app/build/outputs/bundle/release/app-release.aab`.
4. (Optional) Build APK for side-load testing: `./gradlew.bat assembleRelease`.

## 5. (Optional but Recommended) Local Sanity Check
If you want to install the artifact EXACTLY as users will get it:
1. Download `bundletool` (if not already) and run:
   ```powershell
   java -jar bundletool-all.jar build-apks --bundle=app\build\outputs\bundle\release\app-release.aab --output=app-release.apks --mode=universal
   java -jar bundletool-all.jar install-apks --apks=app-release.apks
   ```

## 6. Review Manifest & Permissions
Current `AndroidManifest.xml` has no dangerous runtime permissions declared. Data Safety form answers will reflect: no user data collection unless libraries implicitly collect (Material/Compose do not collect personal data directly). Re‑evaluate if you add analytics/crash reporting later.

## 7. Prepare Store Listing Assets (Required)
1. App name (from `@string/app_name`).
2. Short description (80 chars max) & full description (4000 chars max).
3. Feature graphic (1024×500 PNG/JPG no alpha).
4. Screenshots: min 2 phone (1080p class). Tablet screenshots only if targeting tablets (recommended but not strictly enforced for phone-only distribution).
5. App icon (Play Console upload 512×512; in-app adaptive icon already provided via `mipmap`).
6. Content rating questionnaire answers.
7. Data Safety form: declare no data collection (unless changed).
8. Privacy Policy URL (if no data collection and no sensitive permissions it's sometimes optional, but providing a minimal policy avoids rejection). Host a simple static page.

## 8. Internal Testing Track
1. In Play Console: Create app, choose default language & name.
2. Upload `app-release.aab` to Internal testing track.
3. Add tester emails or a Google Group.
4. Wait for review (internal review is usually fast). Install via opt‑in link.
5. Test startup, settings, core timing functionality, dark/light mode, orientation changes.

## 9. Promote to Production
1. Move the same artifact (promote) or re-upload identical `.aab` to Production release.
2. Provide release notes (what changed vs previous version). Minimal is acceptable.
3. (Recommended) Stage rollout: start at 10% -> monitor ANRs/Crashes -> increase to 100%.

## 10. Post-Release
1. Immediately bump `VERSION_CODE` for next dev cycle to avoid confusion.
2. Tag the commit: `git tag -a v1.1.0 -m "Release 1.1.0"` then `git push --tags`.
3. Monitor Play Console: ANRs, crashes, Pre-launch reports.

## 11. Minimal Privacy Policy Template
Host this (customize) at a stable URL and give link in Play Console.

```
<h1>Privacy Policy</h1>
<p>YetAnotherTimer does not collect, store, transmit, or sell any personal data. The app operates entirely on your device and does not send data to external servers.</p>
<p>No analytics, advertising SDKs, or crash reporting services are integrated in this version.</p>
<p>If future versions add features that process personal data, this policy will be updated and users will be notified via the release notes.</p>
<p>Contact: <em>replace_with_contact_email@example.com</em></p>
```

---
## Quick Command Reference (PowerShell)
```powershell
# Increment version code only
./scripts/bump-version.ps1

# Set new version name + increment code
./scripts/bump-version.ps1 -NewName 1.1.0

# Print current app version
./gradlew.bat printAppVersion

# Build release bundle
./gradlew.bat bundleRelease
```

## Validation Checklist Before Upload
- [ ] VERSION_CODE incremented
- [ ] VERSION_NAME correct
- [ ] Signed bundle produced
- [ ] App launches + core flows tested on at least one physical device
- [ ] Screenshots & feature graphic prepared
- [ ] Data Safety + Content Rating forms completed
- [ ] Privacy Policy URL accessible (200 status)

---
Maintained with required steps only. Add automation later if desired.
