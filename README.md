# G3SPY
<!-- GitAds-Verify: YYLPKAKG3ABMRC7SR5XXAAQYF6DHMZ6D -->
G3SPY is a multi-module Android project consisting of two main modules: `G3SPYParent` and `G3SPYChild`. Each module is structured as a standalone Android application with its own Gradle configuration and source code.

## Project Structure

- `G3SPYParent/` - Main parent Android app module
  - `app/` - Contains source code, resources, and build outputs
  - `build.gradle`, `settings.gradle`, etc. - Gradle configuration files
- `G3SPYChild/` - Child Android app module
  - `app/` - Contains source code, resources, and build outputs
  - `build.gradle`, `settings.gradle`, etc. - Gradle configuration files

## Features
- Modular Android project setup
- Firebase integration (see `google-services.json` in each module)
- Separate build and configuration for parent and child apps

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/RafalW3bCraft/G3SPY.git
   ```
2. **Open in Android Studio:**
   - Open the root folder in Android Studio.
   - Sync Gradle when prompted.
3. **Configure Firebase:**
   - Ensure `google-services.json` is present in each module's `app/` directory.
   - Follow instructions in `FIREBASE_SETUP.md` for Firebase setup.
4. **Build and Run:**
   - Select the desired module (`G3SPYParent` or `G3SPYChild`) and run on an emulator or device.

## GitAds Sponsored
[![Sponsored by GitAds](https://gitads.dev/v1/ad-serve?source=rafalw3bcraft/g3spy@github)](https://gitads.dev/v1/ad-track?source=rafalw3bcraft/g3spy@github)


## Requirements
- Android Studio (latest recommended)
- Android SDK
- Java 8+
- Firebase account (for cloud features)

## License
This project is licensed under the MIT License.

## Author
RafalW3bCraft
