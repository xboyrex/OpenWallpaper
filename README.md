# OpenWallpaper

Open-source 4K Android wallpaper app using:
- GitHub raw JSON as the feed/database
- Cloudflare R2 (or any HTTPS CDN) for full-resolution images and thumbnails
- Kotlin + Jetpack Compose
- Coil 3 memory/disk caching
- Android DownloadManager for public Pictures/Wallpapers downloads
- WallpaperManager for Home/Lock/Both application
- WorkManager for reliable background apply operations
- GitHub Actions tag-based unsigned release APKs

## 0. Prerequisites

- Android Studio with JDK 17
- A GitHub repository
- A Cloudflare R2 bucket exposed through an HTTPS custom domain
- Git configured locally

> The GitHub Actions workflow installs Gradle 9.6 automatically, so a local Gradle installation is not required for CI.

## 1. Configure the feed
Edit `app/src/main/java/com/mayankdroid/wallpapers/AppConfig.kt` and replace `FEED_URL` with your repository URL. Example: `https://raw.githubusercontent.com/USERNAME/OpenWallpaper/main/data/wallpapers.json`. Do not leave the placeholder in production.

## 2. Cloudflare R2
Keep full images and thumbnails in separate prefixes. Prefer a custom R2 domain or Cloudflare-managed custom hostname over an exposed development endpoint for production. Use HTTPS URLs in the JSON.

Recommended layout:
```
wallpapers/
  aurora-001.jpg
thumbs/
  aurora-001.webp
```

## 3. JSON
See `data/wallpapers.json`. Every item needs a unique `id`, HTTPS `full_url`, and HTTPS `thumb_url`.

## 4. Push to GitHub

Create an empty GitHub repository, then from this project directory run:

```bash
git init
git branch -M main
git add .
git commit -m "Initial OpenWallpaper release"
git remote add origin https://github.com/YOUR_USERNAME/OpenWallpaper.git
git push -u origin main
```

Before the first tag, update both:

1. `app/src/main/java/com/mayankdroid/wallpapers/AppConfig.kt`
2. `data/wallpapers.json`

with your real GitHub Raw URL and R2 HTTPS URLs. Open the raw JSON URL in a browser and confirm it returns valid JSON.

## 5. First release
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions builds `app-release-unsigned.apk` and publishes it to the GitHub Release attached to the tag.

## Important
The release APK is intentionally unsigned. For Play Store or trusted sideload distribution, configure a signing key in GitHub Actions and publish a signed artifact. Never commit a keystore or passwords to the repository.


## 6. What GitHub Actions does

A `v*.*.*` tag triggers `.github/workflows/build-apk.yml`. The workflow checks out the repo, installs JDK 17 and Gradle 9.6, builds the release variant, calculates SHA-256, and attaches the unsigned APK plus checksum to the GitHub Release.

For production distribution, add signing through GitHub Secrets later. Never commit a keystore or signing password.
