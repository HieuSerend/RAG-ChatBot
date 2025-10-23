// Simple token helpers: store tokens, parse exp from JWT
const ACCESS_KEY = "rag_access_token";
const REFRESH_KEY = "rag_refresh_token";

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

// Decode JWT payload (no signature verification) to check exp
export function decodeJwt(token: string | null) {
  if (!token) return null;
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")));
    return payload;
  } catch {
    return null;
  }
}

export function isAccessTokenValid(): boolean {
  const token = getAccessToken();
  const payload = decodeJwt(token);
  if (!payload || !payload.exp) return false;
  // exp is in seconds since epoch
  const now = Math.floor(Date.now() / 1000);
  // small buffer (e.g., 10s)
  return payload.exp > now + 10;
}
