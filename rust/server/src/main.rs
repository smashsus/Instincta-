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
    redirect_uri: Option<String>,
}

#[derive(Deserialize)]
struct RefreshRequest {
    access_token: String,
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
    let redirect = payload
        .redirect_uri
        .as_deref()
        .filter(|s| !s.trim().is_empty())
        .unwrap_or(state.redirect_uri.as_str());

    let short_resp = state
        .client
        .post("https://api.instagram.com/oauth/access_token")
        .form(&[
            ("client_id", state.app_id.as_str()),
            ("client_secret", state.app_secret.as_str()),
            ("grant_type", "authorization_code"),
            ("redirect_uri", redirect),
            ("code", payload.code.trim()),
        ])
        .send()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    let status = short_resp.status();
    let body: serde_json::Value = short_resp
        .json()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    if !status.is_success() {
        return Err((StatusCode::BAD_REQUEST, body.to_string()));
    }

    let short_token = body["access_token"]
        .as_str()
        .or_else(|| body["data"][0]["access_token"].as_str())
        .ok_or_else(|| (StatusCode::BAD_REQUEST, body.to_string()))?;

    let user_id = body["user_id"]
        .as_str()
        .map(|s| s.to_string())
        .or_else(|| body["user_id"].as_u64().map(|n| n.to_string()))
        .or_else(|| body["data"][0]["user_id"].as_u64().map(|n| n.to_string()))
        .unwrap_or_else(|| "me".to_string());

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
        access_token: long_resp["access_token"]
            .as_str()
            .unwrap_or(short_token)
            .to_string(),
        user_id,
        expires_in: long_resp["expires_in"].as_u64().unwrap_or(5_184_000),
    }))
}

async fn refresh_instagram_token(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<RefreshRequest>,
) -> Result<Json<SessionResponse>, (StatusCode, String)> {
    let resp: serde_json::Value = state
        .client
        .get("https://graph.instagram.com/refresh_access_token")
        .query(&[
            ("grant_type", "ig_refresh_token"),
            ("access_token", payload.access_token.trim()),
        ])
        .send()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?
        .json()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    let access_token = resp["access_token"]
        .as_str()
        .ok_or_else(|| (StatusCode::BAD_REQUEST, resp.to_string()))?
        .to_string();

    Ok(Json(SessionResponse {
        access_token,
        user_id: String::new(),
        expires_in: resp["expires_in"].as_u64().unwrap_or(5_184_000),
    }))
}

#[tokio::main]
async fn main() {
    let _ = dotenvy::dotenv();
    let state = Arc::new(AppState {
        client: reqwest::Client::new(),
        app_id: std::env::var("INSTAGRAM_APP_ID").expect("INSTAGRAM_APP_ID must be set"),
        app_secret: std::env::var("INSTAGRAM_APP_SECRET").expect("INSTAGRAM_APP_SECRET must be set"),
        redirect_uri: std::env::var("INSTAGRAM_REDIRECT_URI")
            .unwrap_or_else(|_| "instincta://oauth/callback".to_string()),
    });
    let port: u16 = std::env::var("PORT")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(8080);
    let app = Router::new()
        .route("/oauth/instagram/exchange", post(exchange_instagram_code))
        .route("/oauth/instagram/refresh", post(refresh_instagram_token))
        .with_state(state);
    let listener = tokio::net::TcpListener::bind(("0.0.0.0", port))
        .await
        .expect("bind port");
    axum::serve(listener, app).await.expect("serve axum");
}
