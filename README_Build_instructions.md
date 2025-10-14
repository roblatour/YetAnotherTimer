# Yet Another Timer — Windows 11 build guide (VS Code + Gradle)

This project is an Android app built with Kotlin and Jetpack Compose. These steps explain how to build it on Windows 11 using Visual Studio Code and the Gradle wrapper. Android Studio is optional.

If anything is unclear or doesn’t work on your setup, please open an issue in this repository.

—

## 1) Prerequisites (Windows 11)

You can build using either Option A (Android Studio manages SDK/JDK) or Option B (command-line SDK and a standalone JDK). 

### Option A — Android Studio (easiest)
- Install Android Studio: https://developer.android.com/studio
- First run will install the Android SDK and required components.
- In Android Studio settings, make sure Gradle JDK is set to Java 17 (File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK).

### Option B — Command line tools + JDK 17
1) Install Visual Studio Code: https://code.visualstudio.com/

Recommended VS Code extensions:
- Java Extension Pack (ms-vscode.vscode-java-pack)
- Gradle for Java (vscjava.vscode-gradle)
- Kotlin (fwcd.kotlin)

2) Install Git (if you don’t already have it):

```powershell
winget install -e --id Git.Git
```

3) Install JDK 17 (Eclipse Temurin recommended):

```powershell
winget install -e --id EclipseAdoptium.Temurin.17.JDK
```

Open a NEW PowerShell and verify Java is 17:

```powershell
java -version
```

Tip: If you need to pin JAVA_HOME for Gradle/Android tools, set it to your installed JDK path. Example (adjust to your exact folder name):

```powershell
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17*"
```

4) Install Android SDK (command-line tools):
- Create a folder like `E:\Android\Sdk` (avoid spaces in the path).
- Download “Command line tools (Windows)” from: https://developer.android.com/studio#command-line-tools-only
- Unzip so you have:

```
E:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat
```

Persist environment variables (user level):

```powershell
setx ANDROID_SDK_ROOT E:\Android\Sdk
setx PATH "%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin;%ANDROID_SDK_ROOT%\platform-tools;%PATH%"
```

Open a NEW PowerShell and install the required packages (API 34):

```powershell
sdkmanager.bat --install "platform-tools" "build-tools;34.0.0" "platforms;android-34"
sdkmanager.bat --licenses
```

—

## 2) Project versions and requirements
- Gradle (wrapper): 8.7 (managed by `gradlew.bat`)
- Android Gradle Plugin (AGP): 8.5.2
- Kotlin: 1.9.24
- Java toolchain: 17 (source/targetCompatibility = 17)
- Jetpack Compose Compiler: 1.5.14 (Compose BOM 2024.09.02)
- compileSdk: 34, targetSdk: 34, minSdk: 24

No global Gradle install is required; always use the wrapper (`./gradlew` on Unix, `.\gradlew.bat` on Windows).

—

## 3) Get the code

```powershell
cd E:\Documents
git clone https://github.com/roblatour/YetAnotherTimer.git
cd .\YetAnotherTimer\YetAnotherTimer
```

If your Android SDK path differs from `E:\Android\Sdk`, update `local.properties`:

```
sdk.dir=E:\\Android\\Sdk
```

`local.properties` is not used at runtime; it only tells Gradle where the SDK is.

—

## 4) Build (VS Code task or command line)

### Option A — VS Code task
- In VS Code, open the folder `YetAnotherTimer/YetAnotherTimer`.
- Run the build task: Terminal > Run Task… > “Gradle assembleDebug”.
- The first build will download dependencies and may take several minutes.

### Option B — PowerShell command line

```powershell
cd E:\Documents\YetAnotherTimer\YetAnotherTimer
.\gradlew.bat --no-daemon assembleDebug
```

Other useful tasks:

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat :app:dependencies    # optional: inspect dependency graph
```

APK output path:

```
app\build\outputs\apk\debug\app-debug.apk
```

—

## 5) Install/run on a device
- Enable Developer Options and USB debugging on your Android device.
- Connect via USB and trust the computer prompt on the device.
- Verify the device is visible:

```powershell
adb devices
```

- Install the debug APK:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

See also: `README_Install_via_USB_instructions.md` and `README_Install_from_Github_instructions.md`.

—

## 6) Optional: Android Studio workflow
- Open the project at the folder `YetAnotherTimer/YetAnotherTimer`.
- Ensure Gradle JDK is Java 17.
- Use the Run/Debug button to deploy to a device or emulator.

—

## 7) Optional: Release build and signing (advanced)
Debug builds are automatically signed with a debug key and are fine for local installs. For publishing, create a release keystore and configure signing:
- Create a keystore with `keytool`.
- Add a `signingConfigs { release { … } }` block and wire it in `buildTypes.release`.
- Use `./gradlew assembleRelease` to produce `app-release-unsigned.apk` or a signed AAB depending on your config.

Keep keystore files and passwords outside of version control (e.g., in `~/.gradle/gradle.properties`).

—

## 8) Troubleshooting on Windows 11
- gradlew.bat not recognized
  - Run commands from the project root and include the `.\` prefix on Windows.
- Android SDK not found
  - Update `local.properties` with your SDK path (e.g., `sdk.dir=E:\\Android\\Sdk`).
  - Check `ANDROID_SDK_ROOT` and run `sdkmanager.bat --list` in a NEW terminal.
- License or component errors
  - Run `sdkmanager.bat --licenses` and accept all; ensure `platforms;android-34` and `build-tools;34.0.0` are installed.
- Wrong Java version (e.g., “Unsupported class file major version”)
  - Install JDK 17 and ensure Gradle uses it. In Android Studio, set Gradle JDK to 17. On CLI, set `JAVA_HOME` to your JDK 17 folder.
- Device not detected by ADB
  - Use a quality USB cable/port, enable USB debugging, accept the RSA prompt, and install OEM USB drivers via Windows Device Manager.
- Slow or flaky first build
  - The first run downloads many artifacts. You can increase memory in `gradle.properties` (e.g., `org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8`).
- Corporate proxy
  - Configure proxy settings in `%USERPROFILE%\.gradle\gradle.properties`.

—

## 9) Project at a glance
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Min SDK 24, Target/Compile SDK 34
- Key deps: Compose BOM 2024.09.02, Material3, Lifecycle, DataStore, AppCompat, Kotlin coroutines

—

## 10) License
See the `LICENSE` file in the repository.
