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
    #[error("Invalid OAuth input: {message}")]
    Auth { message: String },
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

#[derive(Debug, Clone, uniffi::Record)]
pub struct InstagramMediaItem {
    pub id: String,
    pub caption: String,
    pub media_type: String,
    pub media_url: Option<String>,
    pub thumbnail_url: Option<String>,
    pub permalink: Option<String>,
    pub timestamp: String,
    pub like_count: i32,
    pub comments_count: i32,
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
    #[allow(clippy::new_without_default)]
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
                (
                    "fields",
                    "id,user_id,username,name,account_type,profile_picture_url,biography,followers_count,follows_count,media_count",
                ),
                ("access_token", access_token.trim()),
            ])
            .send()
            .await
            .map_err(|e| InstinctaError::Network {
                message: e.to_string(),
            })?;

        let status = resp.status();
        let body = resp.text().await.map_err(|e| InstinctaError::Network {
            message: e.to_string(),
        })?;
        if !status.is_success() {
            return Err(InstinctaError::MetaApi {
                code: status.as_u16() as i64,
                message: body,
            });
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

        let wire: WireProfile = serde_json::from_str(&body).map_err(|e| InstinctaError::Network {
            message: e.to_string(),
        })?;
        let user_id = wire.user_id.or_else(|| wire.id.clone()).unwrap_or_default();
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

    pub async fn get_media(
        &self,
        access_token: String,
        limit: u32,
    ) -> Result<Vec<InstagramMediaItem>, InstinctaError> {
        let limit_str = limit.clamp(1, 50).to_string();
        let resp = self
            .http
            .get("https://graph.instagram.com/v22.0/me/media")
            .query(&[
                (
                    "fields",
                    "id,caption,media_type,media_url,thumbnail_url,permalink,timestamp,like_count,comments_count",
                ),
                ("limit", limit_str.as_str()),
                ("access_token", access_token.trim()),
            ])
            .send()
            .await
            .map_err(|e| InstinctaError::Network {
                message: e.to_string(),
            })?;

        let status = resp.status();
        let body = resp.text().await.map_err(|e| InstinctaError::Network {
            message: e.to_string(),
        })?;
        if !status.is_success() {
            return Err(InstinctaError::MetaApi {
                code: status.as_u16() as i64,
                message: body,
            });
        }

        #[derive(serde::Deserialize)]
        struct WireMedia {
            id: String,
            caption: Option<String>,
            media_type: Option<String>,
            media_url: Option<String>,
            thumbnail_url: Option<String>,
            permalink: Option<String>,
            timestamp: Option<String>,
            like_count: Option<i32>,
            comments_count: Option<i32>,
        }
        #[derive(serde::Deserialize)]
        struct WireList {
            data: Vec<WireMedia>,
        }

        let parsed: WireList = serde_json::from_str(&body).map_err(|e| InstinctaError::Network {
            message: e.to_string(),
        })?;
        Ok(parsed
            .data
            .into_iter()
            .map(|m| InstagramMediaItem {
                id: m.id,
                caption: m.caption.unwrap_or_default(),
                media_type: m.media_type.unwrap_or_else(|| "IMAGE".into()),
                media_url: m.media_url,
                thumbnail_url: m.thumbnail_url,
                permalink: m.permalink,
                timestamp: m.timestamp.unwrap_or_default(),
                like_count: m.like_count.unwrap_or(0),
                comments_count: m.comments_count.unwrap_or(0),
            })
            .collect())
    }
}
