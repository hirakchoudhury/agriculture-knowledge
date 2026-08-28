"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiFetch, apiPost, refreshSession } from "./api";
import { onAccessTokenChange, setAccessToken } from "./token-store";
import type { AuthResponse, User } from "./types";

type Status = "loading" | "authenticated" | "anonymous";

type AuthContextValue = {
  user: User | null;
  status: Status;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Used by the Google callback page, which receives a token rather than credentials. */
  adoptToken: (token: string) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<Status>("loading");

  // On first load the access token is gone (it only ever lived in memory), but the
  // refresh cookie may still be valid — so a reload silently restores the session.
  useEffect(() => {
    let cancelled = false;

    (async () => {
      const token = await refreshSession();
      if (cancelled) return;

      if (!token) {
        setStatus("anonymous");
        return;
      }

      try {
        const me = await apiFetch<User>("/api/v1/users/me");
        if (cancelled) return;
        setUser(me);
        setStatus("authenticated");
      } catch {
        if (!cancelled) setStatus("anonymous");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  // If the API layer gives up on a session mid-flight, the UI must follow.
  useEffect(() => {
    return onAccessTokenChange((token) => {
      if (!token) {
        setUser(null);
        setStatus("anonymous");
      }
    });
  }, []);

  const adoptSession = useCallback((response: AuthResponse) => {
    setAccessToken(response.accessToken);
    setUser(response.user);
    setStatus("authenticated");
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      adoptSession(await apiPost<AuthResponse>("/api/v1/auth/login", { email, password }));
    },
    [adoptSession],
  );

  const register = useCallback(
    async (email: string, password: string, name: string) => {
      adoptSession(
        await apiPost<AuthResponse>("/api/v1/auth/register", { email, password, name }),
      );
    },
    [adoptSession],
  );

  const adoptToken = useCallback(async (token: string) => {
    setAccessToken(token);
    const me = await apiFetch<User>("/api/v1/users/me");
    setUser(me);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiPost<void>("/api/v1/auth/logout");
    } finally {
      // Even if the call fails, drop the local session: staying "signed in" after
      // the user asked to leave is worse than a stale row in the database.
      setAccessToken(null);
      setUser(null);
      setStatus("anonymous");
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAdmin: user?.role === "ADMIN",
      login,
      register,
      logout,
      adoptToken,
    }),
    [user, status, login, register, logout, adoptToken],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return context;
}
