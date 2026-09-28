# OpenWallpaper

Open-source, production-oriented 4K wallpaper Android app built with Kotlin + Jetpack Compose.

## Architecture

- GitHub Raw JSON = dynamic wallpaper feed / metadata database
- Cloudflare R2 = original 4K images + lightweight thumbnails
- MVVM + Repository
- Coil 3 memory + disk caching
- LazyVerticalStaggeredGrid for the wallpaper feed
- Native DownloadManager → `Pictures/Wallpapers`
- Native WallpaperManager for Home / Lock / Both
- WorkManager for background wallpaper application
- GitHub Actions for unsigned release APKs

## UI

The UI follows a modern gallery/store aesthetic: large rounded surfaces, curated featured carousel, category chips, search, responsive two-column masonry grid, dark/light system theme support, and a focused full-screen preview.

## Feed URL

Configured in `app/src/main/java/com/mayankdroid/wallpapers/AppConfig.kt`:

`https://raw.githubusercontent.com/xboyrex/OpenWallpaper/main/data/wallpapers.json`

## JSON

Each item should contain:

```json
{
  "id": "unique-id",
  "title": "Wallpaper title",
  "category": "Anime",
  "width": 3840,
  "height": 2160,
  "full_url": "https://cdn.example.com/wallpapers/file.jpg",
  "thumb_url": "https://cdn.example.com/thumbs/file.webp",
  "size_bytes": 2000000,
  "updated_at": "2026-09-28T16:00:00Z"
}
```

## GitHub Actions

Manual build: **Actions → Build & Release APK → Run workflow**.

Release build: push a tag such as `v1.0.0`.

The workflow uploads the unsigned APK and SHA-256 checksum as an Actions artifact. Tag builds also publish both files to GitHub Releases.

## Important

Do not commit signing keys or private R2 credentials. Public wallpaper URLs are expected by the client architecture.
