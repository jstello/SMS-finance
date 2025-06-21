# Publishing to the Google Play Store

This document outlines the steps required to publish the SMS Finance application to the Google Play Store.

## 1. Prepare Your App for Release

- [ ] Finalize the app's code and features.
- [ ] Thoroughly test the app for bugs and performance issues.
- [x] **Create a release-ready Android App Bundle (AAB).**

  The Android App Bundle (AAB) is the standard format for publishing apps on Google Play. To generate it, run the following command in your project's root terminal:

  ```bash
  ./gradlew bundleRelease
  ```

  Once the command finishes, you will find the generated AAB file at `app/build/outputs/bundle/release/app-release.aab`. This is the file you will upload to the Play Console.
- [x] **Sign the app with a release key.**

  To publish on the Google Play Store, your app must be signed with a release key. This key serves as your digital signature and ensures that all updates come from you.

  **1. Generate a Signing Key:**

  We will use the `keytool` command to generate a new keystore. Run the following command in your terminal:

  ```bash
  keytool -genkey -v -keystore sms-finance-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sms-finance-key
  ```

  You will be prompted to create passwords for the keystore and the key alias. **Remember these passwords.**

  **2. Move the Keystore File:**

  After generating the key, move the `sms-finance-key.jks` file to the `app` directory of your project.

  **3. Configure Build Gradle:**

  Your `app/build.gradle.kts` is already configured to use a `keystore.properties` file for signing.

  **4. Create `keystore.properties`:**

  Create a file named `keystore.properties` in the root of your project with the following content. **Replace the placeholder values with the actual passwords you created.**

  ```properties
  storePassword=YOUR_STORE_PASSWORD
  keyPassword=YOUR_KEY_PASSWORD
  keyAlias=sms-finance-key
  storeFile=app/sms-finance-key.jks
  ```

  **5. Secure Your Credentials:**

  To prevent your sensitive key credentials from being exposed, you must add `keystore.properties` to your `.gitignore` file. This ensures it will not be committed to version control. We have already added this for you.

## 2. Set Up Your Google Play Developer Account

- [ ] **Create a Google Play Developer account.**

  This is done on the Google Play Console website. You will need a Google account to proceed.

  1.  Navigate to the [Google Play Console](https://play.google.com/console).
  2.  Follow the steps to sign up for a developer account.
  3.  You will need to pay a one-time registration fee of $25.
  4.  Complete your developer profile with all the required information.
- [ ] Pay the one-time registration fee.
- [ ] Complete your account details.

## 3. Create the App Listing in Google Play Console

- [ ] Create a new app in the Play Console.
- [ ] Fill out the store listing details (title, descriptions, etc.).
- [ ] Prepare and upload graphic assets (app icon, feature graphic, screenshots).

## 4. Configure the App Release

- [ ] Upload the signed App Bundle or APK.
- [ ] Complete the content rating questionnaire.
- [ ] Set up pricing and distribution options.
- [ ] Configure privacy policy.

## 5. Roll Out and Publish

- [ ] Start a release on a testing track (Internal, Closed, or Open testing).
- [ ] Gather feedback and make necessary improvements.
- [ ] Promote the release to production.
- [ ] Wait for Google's review and approval.

---

We will go through each of these steps together. Let's start with the first section: **Prepare Your App for Release**.
