# Instincta — Rust Core Export Blueprint (`instincta-core`)

Because the AI Studio Android build environment compiles Kotlin/Compose/Gradle without a local `cargo`, `rustc`, or `cargo-ndk` toolchain, this APK fulfills the `InstagramCorePort` contract (`app/src/main/java/com/example/data/InstagramCorePort.kt`) using `KotlinInstagramCore` (Retrofit + OkHttp + Moshi + DataStore).

After exporting this project as a ZIP or pushing it to GitHub, you can replace `KotlinInstagramCore` with a compiled native Rust core (`libinstincta_core.so`) using **UniFFI** and **`cargo-ndk`** with zero changes to the Jetpack Compose UI.

---

## 1. Cargo Workspace Layout

```text
instincta-rust/
├── Cargo.toml
├── crates/
│   └── instincta-core/
│       ├── Cargo.toml
│       ├── build.rs
│       └── src/
│           ├── lib.rs          # UniFFI trait & async exports matching InstagramCorePort
│           ├── models.rs       # Serde structs for Meta Graph API v22.0
│           └── client.rs       # reqwest + tokio Instagram API client
└── server/
    ├── Cargo.toml
    └── src/
        └── main.rs             # Axum OAuth token exchange proxy (production secret isolation)
```

---

## 2. `crates/instincta-core/Cargo.toml`

```toml
[package]
name = "instincta-core"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib", "staticlib"]
name = "instincta_core"

[dependencies]
reqwest = { version = "0.12", default-features = false, features = ["json", "rustls-tls"] }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
thiserror = "2.0"
tokio = { version = "1.40", features = ["rt-multi-thread", "time"] }
uniffi = { version = "0.28", features = ["tokio"] }
url = "2.5"
```

---

## 3. `crates/instincta-core/src/lib.rs` (UniFFI Contract)

```rust
use std::sync::Arc;
use thiserror::Error;
use url::Url;

uniffi::setup_scaffolding!();

#[derive(Debug, Error, uniffi::Error)]
pub enum InstinctaError {
    #[error("Network failure: {message}")]
    Network { message: String },
    #[error("Meta Graph API error ({code}): {message}")]
    MetaApi { code: i64, message: String },
    #[error("Invalid OAuth state or code: {message}")]
    Auth { message: String },
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct InstagramSession {
    pub access_token: String,
    pub user_id: String,
    pub expires_at_epoch_sec: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct InstagramProfile {
    pub id: String,
    pub user_id: String,
    pub username: String,
    pub name: String,
    pub account_type: String,
    pub profile_picture_url: Option<String>,
    pub biography: String,
    pub followers_count: i32,
    pub follows_count: i32,
    pub media_count: i32,
}

#[uniffi::export]
pub fn build_authorize_url(app_id: String, redirect_uri: String) -> String {
    let mut url = Url::parse("https://www.instagram.com/oauth/authorize").expect("valid base url");
    url.query_pairs_mut()
        .append_pair("enable_fb_login", "0")
        .append_pair("force_authentication", "1")
        .append_pair("client_id", app_id.trim())
        .append_pair("redirect_uri", redirect_uri.trim())
        .append_pair("response_type", "code")
        .append_pair(
            "scope",
            "instagram_business_basic,instagram_business_content_publish,instagram_business_manage_comments,instagram_business_manage_messages",
        );
    url.to_string()
}

#[derive(uniffi::Object)]
pub struct RustInstagramCore {
    http: reqwest::Client,
}

#[uniffi::export]
impl RustInstagramCore {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            http: reqwest::Client::new(),
        })
    }

    pub async fn get_profile(&self, access_token: String) -> Result<InstagramProfile, InstinctaError> {
        let resp = self
            .http
            .get("https://graph.instagram.com/v22.0/me")
            .query(&[
                ("fields", "id,user_id,username,name,account_type,profile_picture_url,biography,followers_count,follows_count,media_count"),
                ("access_token", access_token.as_str()),
            ])
            .send()
            .await
            .map_err(|e| InstinctaError::Network { message: e.to_string() })?;

        let status = resp.status();
        let text = resp.text().await.map_err(|e| InstinctaError::Network { message: e.to_string() })?;
        if !status.is_success() {
            return Err(InstinctaError::MetaApi { code: status.as_u16() as i64, message: text });
        }

        #[derive(serde::Deserialize)]
        struct WireProfile {
            id: Option<String>,
            user_id: Option<String>,
            username: Option<String>,
            name: Option<String>,
            account_type: Option<String>,
            profile_picture_url: Option<String>,
            biography: Option<String>,
            followers_count: Option<i32>,
            follows_count: Option<i32>,
            media_count: Option<i32>,
        }

        let wire: WireProfile = serde_json::from_str(&text)
            .map_err(|e| InstinctaError::Network { message: e.to_string() })?;
        let user_id = wire.user_id.or(wire.id.clone()).unwrap_or_default();
        Ok(InstagramProfile {
            id: wire.id.unwrap_or_else(|| user_id.clone()),
            user_id,
            username: wire.username.unwrap_or_else(|| "creator".into()),
            name: wire.name.unwrap_or_default(),
            account_type: wire.account_type.unwrap_or_else(|| "BUSINESS".into()),
            profile_picture_url: wire.profile_picture_url,
            biography: wire.biography.unwrap_or_default(),
            followers_count: wire.followers_count.unwrap_or(0),
            follows_count: wire.follows_count.unwrap_or(0),
            media_count: wire.media_count.unwrap_or(0),
        })
    }
}
```

---

## 4. Building Android `.so` Libraries (`cargo-ndk`)

Install the Android Rust targets and `cargo-ndk`, then compile directly into `app/src/main/jniLibs/`:

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk

cargo ndk \
  -t arm64-v8a \
  -t armeabi-v7a \
  -t x86_64 \
  -o ../app/src/main/jniLibs \
  build --release

cargo run --bin uniffi-bindgen generate \
  --library target/aarch64-linux-android/release/libinstincta_core.so \
  --language kotlin \
  --out-dir ../app/src/main/java
```

Then implement `InstagramCorePort` with a `RustInstagramCoreAdapter` delegating to the generated UniFFI `RustInstagramCore` class and swap it in `MainActivity.kt`.

---

## 5. Production Security: Axum Token Exchange Proxy (`server/src/main.rs`)

In prototype builds, `INSTAGRAM_APP_SECRET` is read from `BuildConfig`. **Before shipping a production APK**, move the `POST https://api.instagram.com/oauth/access_token` and `ig_exchange_token` calls onto an Axum server so `INSTAGRAM_APP_SECRET` never resides in the mobile binary:

```rust
use axum::{extract::State, http::StatusCode, routing::post, Json, Router};
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Clone)]
struct AppState {
    client: reqwest::Client,
    app_id: String,
    app_secret: String,
    redirect_uri: String,
}

#[derive(Deserialize)]
struct ExchangeRequest {
    code: String,
}

#[derive(Serialize)]
struct SessionResponse {
    access_token: String,
    user_id: String,
    expires_in: u64,
}

async fn exchange_instagram_code(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<ExchangeRequest>,
) -> Result<Json<SessionResponse>, (StatusCode, String)> {
    let short_resp = state
        .client
        .post("https://api.instagram.com/oauth/access_token")
        .form(&[
            ("client_id", state.app_id.as_str()),
            ("client_secret", state.app_secret.as_str()),
            ("grant_type", "authorization_code"),
            ("redirect_uri", state.redirect_uri.as_str()),
            ("code", payload.code.trim()),
        ])
        .send()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    let body: serde_json::Value = short_resp
        .json()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    let short_token = body["access_token"]
        .as_str()
        .ok_or((StatusCode::BAD_REQUEST, body.to_string()))?;
    let user_id = body["user_id"].to_string();

    let long_resp: serde_json::Value = state
        .client
        .get("https://graph.instagram.com/access_token")
        .query(&[
            ("grant_type", "ig_exchange_token"),
            ("client_secret", state.app_secret.as_str()),
            ("access_token", short_token),
        ])
        .send()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?
        .json()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    Ok(Json(SessionResponse {
        access_token: long_resp["access_token"].as_str().unwrap_or(short_token).to_string(),
        user_id,
        expires_in: long_resp["expires_in"].as_u64().unwrap_or(5_184_000),
    }))
}

#[tokio::main]
async fn main() {
    let state = Arc::new(AppState {
        client: reqwest::Client::new(),
        app_id: std::env::var("INSTAGRAM_APP_ID").expect("INSTAGRAM_APP_ID"),
        app_secret: std::env::var("INSTAGRAM_APP_SECRET").expect("INSTAGRAM_APP_SECRET"),
        redirect_uri: std::env::var("INSTAGRAM_REDIRECT_URI").expect("INSTAGRAM_REDIRECT_URI"),
    });
    let app = Router::new()
        .route("/oauth/instagram/exchange", post(exchange_instagram_code))
        .with_state(state);
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8080").await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
```
