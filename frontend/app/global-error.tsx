"use client"

// Next 16: global-error replaces the root layout when it fails, so it must
// render its own <html> and <body> (globals.css is not loaded here — styles inline).
export default function GlobalError({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return (
    <html lang="en">
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: "system-ui, -apple-system, sans-serif",
          background: "#fafafa",
          color: "#111",
        }}
      >
        <div style={{ maxWidth: "28rem", padding: "2rem", textAlign: "center" }}>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, margin: 0 }}>Something went wrong</h2>
          <p style={{ marginTop: "0.5rem", fontSize: "0.875rem", color: "#555" }}>
            A critical error occurred while loading ELMKUSOMA. You can try again or return to the homepage.
          </p>
          <div style={{ marginTop: "1.5rem", display: "flex", gap: "0.75rem", justifyContent: "center" }}>
            <button
              onClick={() => retry()}
              style={{
                height: "2.5rem",
                padding: "0 1rem",
                borderRadius: "0.5rem",
                border: "none",
                background: "#111",
                color: "#fff",
                fontSize: "0.875rem",
                cursor: "pointer",
              }}
            >
              Try again
            </button>
            <a
              href="/"
              style={{
                height: "2.5rem",
                padding: "0 1rem",
                borderRadius: "0.5rem",
                border: "1px solid #ddd",
                background: "#fff",
                color: "#111",
                fontSize: "0.875rem",
                display: "inline-flex",
                alignItems: "center",
                textDecoration: "none",
              }}
            >
              Homepage
            </a>
          </div>
          {error.digest && <p style={{ marginTop: "1rem", fontSize: "0.625rem", color: "#999" }}>Reference: {error.digest}</p>}
        </div>
      </body>
    </html>
  )
}
