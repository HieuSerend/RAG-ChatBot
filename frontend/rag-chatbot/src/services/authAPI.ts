import api from "./api";
import { setTokens, clearTokens } from "./authService";

type LoginRequest = { username: string; password: string };
type RegisterRequest = { username: string; password: string };

export async function login(request: LoginRequest) {
  const res = await api.post("/auth/login", request);
  // According to your swagger, response.data.data contains accessToken & refreshToken
  const data = res.data?.data;
  if (data?.accessToken && data?.refreshToken) {
    setTokens(data.accessToken, data.refreshToken);
  }
  return res.data;
}

export async function register(request: RegisterRequest) {
  const res = await api.post("/user/register", request);
  return res.data;
}

export async function logout(accessToken?: string, refreshToken?: string) {
  try {
    // call logout endpoint to invalidate tokens server-side (spec requires both tokens)
    await api.post("/auth/logout", { accessToken, refreshToken });
  } catch {
    // ignore server error for logout
  } finally {
    clearTokens();
  }
}
