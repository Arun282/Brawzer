# Arun Browser — Android WebView app

A native Android browser shell using Android System WebView, with pages loaded inside the app rather than sent to Chrome for ordinary HTTP/HTTPS links.

## Features
- Google search/address bar
- Website rendering inside the app using Android System WebView
- Multiple tabs: add tabs, switch tabs, close tabs
- Back, forward, reload, home and Android back navigation
- Loading progress and page title display
- Save bookmarks and open saved bookmarks
- Basic incognito toggle for new tabs
- DownloadManager downloads and basic download history

## Build APK with GitHub Actions
1. Open the **Actions** tab and enable workflows if GitHub asks.
2. Run **Build Arun Browser APK** or push a commit to `main`.
3. Open the completed workflow run and download the `Arun-Browser-APK` artifact.
4. Extract the artifact ZIP to get `app-debug.apk`.

## Important limitations
- Rendering uses Android System WebView, not a newly written independent Chromium engine.
- Incognito mode is basic and is not a hardened privacy browser.
- Some sites may block embedded browsers or restrict DRM media.