![](https://github.com/DrewStephensCoding/AFIExplorer/blob/master/screenshots/AFI%20Explorer.png)

## Purpose
A minimal Android application built to easily reference U.S Air Force guidance and publications using Modern Android development tools.

## App Link
##### You can directly download the latest version of the app from the play store. 🎯
<a href='https://play.google.com/store/apps/details?id=io.github.drewstephenscoding.afiexplorer'><img align='center' height='85' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'></a>

## Built With 🛠
- [Kotlin](https://kotlinlang.org/) - First class and official programming language for Android development.
- [Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) - For asynchronous and more..
- [Toasty](https://github.com/GrenderG/Toasty) - The usual Toast, but with steroids
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture) - Collection of libraries that help you design robust, testable, and maintainable apps.
  - [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) - Data objects that notify views when the underlying database changes.
  - [Room](https://developer.android.com/topic/libraries/architecture/room) - SQLite object mapping library.
- [Material Components for Android](https://github.com/material-components/material-components-android) - Modular and customizable Material Design UI components for Android.

## Change Log 
To see the list of changes and updates to AFI Explorer, please see the [Change Log](https://github.com/DrewStephensCoding/AFIExplorer/blob/master/CHANGE_LOG.md).  This list will be kept up to date as the app grows.

## Permissions 🔒
The following permissions are utilized in the app, nothing more.
```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
- android.permission.INTERNET is required for network operations 
- android.permission.ACCESS_WIFI_STATE allows applications to access information about Wi-Fi networks
- android.permission.READ_EXTERNAL_STORAGE allows an application to read from external storage.
- android.permission.WRITE_EXTERNAL_STORAGE allows an application to write to external storage.

Additional information for these permissions can be found at https://developer.android.com/reference/android/Manifest.permission

## MAD Scorecard
What is the MAD Scorecard? MAD scorecard uses Android Studio to tell you interesting things like how much size savings your app is seeing through the Android App Bundle. It spotlights each of the key MAD technologies, including specific Jetpack libraries and Kotlin features used in your app.  AFI Explorer's MAD Scorecard can be found below. More information on Modern Android Architecture (MAD) can be found at https://developer.android.com/modern-android-development/scorecard
![](https://github.com/DrewStephensCoding/AFIExplorer/blob/master/screenshots/summary.png)

## Download
- AFI Explorer is available for both iOS and Android.  Download the app at [AFI Explorer](https://afiexplorer.com/)

## Getting Started
This project uses the Gradle build system. To build this project, use the `gradlew build` command or use "Import Project" in Android Studio.

To run tests, run `gradlew test`

To learn more about Android accessibility, visit the Android accessibility page. To learn more about developer facing aspects of Android accessibility, read the accessibility developer guide.

## Suggestions
We've been provided several great suggestions to improve the app.  While some are unfortunately not doable do to Android/Java architecture, we are researching the best approaches to continue development.  For the current list of suggestions, please see [App Suggestions](https://github.com/DrewStephensCoding/AFIExplorer/blob/master/SUGGESTIONS.md).  This list will be kept up to date.

## Issues
We are continuously working to improve the app and add new features.  If you have suggestions for improvements or if you encounter a problem, please provide feedback to us via in-app feedback.  When creating an issue, try to include the following:
-  The device manufacturer and model
-  The Android build running on the device
-  A description of the issue; include screenshots/screencaps if you think they'll help us understand the problem

## Contribute
If you want to contribute to this library, you're always welcome! See [Contributing Guideline](https://github.com/DrewStephensCoding/AFIExplorer/blob/master/CONTRIBUTION.md)

## Code of Conduct
This [Code of Conduct](https://github.com/DrewStephensCoding/AFIExplorer/blob/master/CODE_OF_CONDUCT.md) outlines our expectations for participants within the Airmen Coders community, as well as steps to reporting unacceptable behavior. We are committed to providing a welcoming and inspiring community for all and expect our code of conduct to be honored.

## Donation
If AFI Explorer has helped make your daily life easier, please consider supporting future updates.

<a href="https://www.buymeacoffee.com/drewcodesit" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 174px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a>
