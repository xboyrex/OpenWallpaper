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

## 1. Configure the feed
Edit `app/src/main/java/com/mayankdroid/wallpapers/AppConfig.kt` and replace `FEED_URL` with the raw GitHub URL for `data/wallpapers.json`.

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

## 4. First release
```bash
git add .
git commit -m "Initial OpenWallpaper release"
git push origin main
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions builds `app-release-unsigned.apk` and publishes it to the GitHub Release attached to the tag.

## Important
The release APK is intentionally unsigned. For Play Store or trusted sideload distribution, configure a signing key in GitHub Actions and publish a signed artifact. Never commit a keystore or passwords to the repository.
