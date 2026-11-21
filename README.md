# Instincta

An open-source, **Pure White — Editorial** Android client for **Instagram Creator and Business accounts**, built around Meta's official **Instagram API with Instagram Login** (`graph.instagram.com/v22.0`) and a **Rust Core + Axum OAuth Proxy** architecture.

## Why Zero-Secret Client Architecture?

Open-source Android APKs can be decompiled by anyone. Embedding `INSTAGRAM_APP_SECRET` in an Android app would leak your Meta App Secret.

Instincta keeps **zero secrets in the Android APK**:
1. **Direct Instagram Token Sign-In (Fastest / No Server Required):** Generate a User Access Token for your Professional Instagram account in the Meta Developer Dashboard (*App -> Instagram -> API setup with Instagram login -> Generate Token*) and paste it into the app. Instincta verifies the token live against `GET https://graph.instagram.com/v22.0/me`.
2. **Self-Hosted OAuth Login + Rust Axum Proxy (`rust/server/`):** Run the included Rust `axum` server (`POST /oauth/instagram/exchange`), which holds `INSTAGRAM_APP_SECRET` in server environment variables and exchanges OAuth codes for 60-day long-lived tokens on behalf of the Android client.

## Features

- **Grid:** Live `/me` profile summary + 3-column edge-to-edge `/me/media` grid, cursor pagination, carousel pager, and inline video playback.
- **Insights:** Account-level (`/me/insights`) and per-media (`/{mediaId}/insights`) reach, views, likes, comments, saves, and shares.
- **Publish:** Meta container publishing (`POST /me/media` -> poll `status_code` -> `POST /me/media_publish`) for **Photo**, **Carousel** (2–10 items), and **Reels**.
- **Comment Moderation:** List, post, reply, hide/unhide, and delete comments on any published post.
- **Inbox:** Instagram Direct Message threads (`/me/conversations`) and live replies (`POST /me/messages`).
- **Rust Core & Backend Workspace (`rust/`):**
  - `rust/crates/instincta-core`: UniFFI + `reqwest` + `serde` + `tokio` library matching `InstagramCorePort.kt`.
  - `rust/server`: Axum OAuth token exchange & refresh proxy holding `INSTAGRAM_APP_SECRET` off-device.

## Running the Rust Axum OAuth Proxy (`rust/server`)

```bash
cd rust
cp server/.env.example server/.env
# Fill in INSTAGRAM_APP_ID, INSTAGRAM_APP_SECRET, and INSTAGRAM_REDIRECT_URI=instincta://oauth/callback
cargo run -p instincta-auth-server
```

Then in the Android app's **Self-Hosted OAuth Login** section, enter your numeric Meta App ID and your server URL (for example `http://10.0.2.2:8080` in an Android Emulator or `https://auth.yourdomain.com` in production).

## Compiling `instincta-core` for Android (`cargo-ndk`)

See [`docs/RUST_CORE_EXPORT.md`](docs/RUST_CORE_EXPORT.md) for the full `cargo-ndk` + UniFFI bindgen steps to swap `KotlinInstagramCore` with the native Rust library.

## License

MIT — see [`LICENSE`](LICENSE).
