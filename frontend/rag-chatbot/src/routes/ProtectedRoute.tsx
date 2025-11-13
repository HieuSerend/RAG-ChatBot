import React, { useEffect, useState, type JSX } from "react";
import { Navigate } from "react-router-dom";
import { getAccessToken, getRefreshToken, isAccessTokenValid } from "../services/authService";
import api from "../services/api";

export default function ProtectedRoute({ children }: { children: JSX.Element }) {
  const [checking, setChecking] = useState(true);
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    (async () => {
      const access = getAccessToken();
      const refresh = getRefreshToken();
        
      // If access token exists and is valid => pass
      if (access && isAccessTokenValid()) {
        setAuthed(true);
        setChecking(false);
        return;
      }

      // If access invalid but have a refresh token, try refresh endpoint directly
      if (refresh) {
        try {
          const res = await api.post("/auth/refresh", { refreshToken: refresh });
          // api interceptor will not set tokens; response body contains tokens
          const data = res.data?.data;
          if (data?.accessToken && data?.refreshToken) {
            // store tokens using authService.setTokens
            // but avoid import cycle; use dynamic import
            const { setTokens } = await import("../services/authService");
            setTokens(data.accessToken, data.refreshToken);
            setAuthed(true);
            setChecking(false);
            return;
          }
        } catch {
          // failed to refresh
        }
      }

      // else not authenticated
      setAuthed(false);
      setChecking(false);
    })();
  }, []);

  if (checking) {
    return <div style={{ padding: 20 }}>Checking authentication...</div>;
  }

  return authed ? children : <Navigate to="/auth/login" replace />;
}
