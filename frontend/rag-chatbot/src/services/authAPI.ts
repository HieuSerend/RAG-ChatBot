import api from "./api";
import { setTokens, clearTokens } from "./authService";

type LoginRequest = { username: string; password: string };
type RegisterRequest = { username: string; password: string };

export async function login(request: LoginRequest) {
  // ensure no Authorization header is attached for this public endpoint
  const res = await api.post("/auth/login", request, { headers: { "X-Skip-Auth": "true" } });
  // According to your swagger, response.data.data contains accessToken & refreshToken
  const data = res.data?.data;
  if (data?.accessToken && data?.refreshToken) {
    setTokens(data.accessToken, data.refreshToken);
  }
  return res.data;
}

export async function register(request: RegisterRequest) {
  // explicitly skip attaching Authorization for register (public)
  const res = await api.post("/user/register", request, { headers: { "X-Skip-Auth": "true" } });
  return res.data;
}

export async function logout(accessToken?: string, refreshToken?: string) {
  try {
    // call logout endpoint to invalidate tokens server-side (spec requires both tokens)
    await api.post("/auth/logout", { accessToken, refreshToken }, { headers: { "X-Skip-Auth": "true" } });
  } catch {
    // ignore server error for logout
  } finally {
    clearTokens();
  }
}
