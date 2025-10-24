import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
  type AxiosRequestHeaders,
} from "axios";
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
} from "../services/authService";

// In development proxy /api to the backend to avoid CORS (see vite.config.ts).
// Use VITE_API_BASE_URL to override in production if needed.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 20000,
});

// Refresh token lock to avoid race conditions
let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (isRefreshing && refreshPromise) return refreshPromise;
  isRefreshing = true;
  refreshPromise = (async () => {
    try {
      const refreshToken = getRefreshToken();
      if (!refreshToken) throw new Error("No refresh token");
      const res = await axios.post(
        `${BASE_URL}/auth/refresh`,
        { refreshToken },
        { headers: { "Content-Type": "application/json" } },
      );
      const data = res.data?.data;
      if (data?.accessToken && data?.refreshToken) {
        setTokens(data.accessToken, data.refreshToken);
        return data.accessToken as string;
      }
      throw new Error("Invalid refresh response");
    } catch {
      // refresh failed -> clear tokens
      clearTokens();
      return null;
    } finally {
      isRefreshing = false;
      refreshPromise = null;
    }
  })();
  return refreshPromise;
}

// Public endpoints that should not receive Authorization header
const PUBLIC_ENDPOINTS = [
  "/user/register",
  "/auth/login",
  "/auth/refresh",
  "/auth/logout",
];

function isPublicRequest(
  config?: InternalAxiosRequestConfig | AxiosRequestConfig,
): boolean {
  if (!config || !config.url) return false;
  // If url is absolute, extract pathname
  let path = config.url as string;
  try {
    if (path.startsWith("http")) {
      path = new URL(path).pathname;
    }
  } catch {
    // ignore
  }
  // If baseURL is set and url is relative, use url as-is (it will start with '/')
  return PUBLIC_ENDPOINTS.some((ep) => path === ep || path.endsWith(ep));
}

// Request interceptor: add Authorization header (but skip for public endpoints or when explicitly skipped)
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // allow callers to explicitly skip auth by setting X-Skip-Auth
    const headers = config.headers as AxiosRequestHeaders | undefined;
    if (
      headers &&
      (headers["X-Skip-Auth"] === "true" || headers["X-Skip-Auth"] === true)
    ) {
      // remove the helper header so it doesn't get sent to server
      delete (headers as unknown as Record<string, unknown>)["X-Skip-Auth"];
      return config;
    }

    if (isPublicRequest(config)) return config;

    const token = getAccessToken();
    if (token && config.headers) {
      // merge to avoid type incompatibilities with AxiosRequestHeaders implementation
      config.headers = Object.assign(
        {},
        config.headers as unknown as Record<string, string>,
        {
          Authorization: `Bearer ${token}`,
        },
      ) as unknown as AxiosRequestHeaders;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Response interceptor: on 401 try refresh then retry original request once
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        // set authorization header and retry
        if (!originalRequest.headers)
          originalRequest.headers = {} as AxiosRequestHeaders;
        originalRequest.headers = Object.assign(
          {},
          originalRequest.headers as unknown as Record<string, string>,
          {
            Authorization: `Bearer ${newToken}`,
          },
        ) as unknown as AxiosRequestHeaders;
        return api(originalRequest);
      }
      // refresh failed, propagate error (frontend will redirect to /auth according to ProtectedRoute)
    }
    return Promise.reject(error);
  },
);

export default api;
