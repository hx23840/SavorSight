mod app_state;
mod error;
mod models;
mod routes;
mod services;

use std::net::SocketAddr;

use app_state::AppState;
use tower_http::{cors::CorsLayer, trace::TraceLayer};

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let state = AppState::new("data");
    let app = routes::router()
        .layer(CorsLayer::permissive())
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    let addr = SocketAddr::from(([127, 0, 0, 1], 8080));
    let listener = tokio::net::TcpListener::bind(addr)
        .await
        .expect("failed to bind server address");

    tracing::info!("listening on http://{addr}");

    axum::serve(listener, app).await.expect("server failed");
}
