# IIT Patna — M.Tech AI & DSE Timetable

Public timetable dashboard and Android app for the IIT Patna M.Tech 2026–27 batch, Specialization in Artificial Intelligence & Data Science and Engineering (AI & DSE), Semester 1.

## Features

- Weekly timetable from Monday to Sunday
- Light blue for regular courses
- Light yellow for elective courses
- One-click Moodle course links
- Moodle links stay inside the Android app WebView
- Android app remembers the email ID entered on Moodle login pages
- Elective selector with localStorage persistence
- Today view
- Regular / elective filters
- Responsive mobile layout
- No backend required

## Run locally

Open `index.html` in a browser.

## NPM commands

This project has no web build step, but npm scripts are available for the Android app workflow:

```sh
npm install
npm run android:sync
npm run android:build
npm run android:install
npm run release:local
```

- `android:sync` copies the web files into `android/app/src/main/assets/www/`.
- `android:build` creates a debug APK.
- `android:install` installs the debug app on a connected emulator or Android device.
- `release:local` creates a local release APK.

The APK outputs are generated under:

`android/app/build/outputs/apk/`

The Android commands use `android/gradlew` if a Gradle wrapper is present. Otherwise they use a locally installed `gradle` command.

## Android app

The Android project lives in `android/` and loads the same timetable UI from:

`android/app/src/main/assets/www/`

To build or run it:

1. Open the `android/` folder in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Run the `app` configuration on an emulator or Android device.

When a Moodle login page is opened inside the app, the email or username field is watched locally. If the entered value is an email ID, it is saved in Android `SharedPreferences` and filled back into Moodle login fields the next time they appear.

After changing the web timetable files, sync them into the Android app assets:

```sh
npm run android:sync
```

## Deploy with GitHub Pages

1. Create a public GitHub repository.
2. Upload all files and folders from this project.
3. Go to **Settings → Pages**.
4. Under **Build and deployment**, choose **Deploy from a branch**.
5. Select `main` and `/ (root)`.
6. Save.
7. GitHub will provide the public Pages URL.

## Updating the timetable

Edit:

`js/courses.js`

The `COURSES` array contains course information and Moodle links.

The `SCHEDULE` array contains day, time, course and lab information.

## Important

This timetable is based on the class schedule and Moodle links supplied for the project. Verify any timetable changes with the official IIT Patna academic/class communication before relying on them.
