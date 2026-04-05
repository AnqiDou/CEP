import { reactive } from "vue";
import { parseAuthSessionResponse } from "../../interface/login/loginInterfaces";
import {
  fetchCurrentUser as fetchCurrentUserApi,
  logoutSession,
  refreshSession,
} from "../login/loginApiService";

const AUTH_STORAGE_KEY = "cep.auth.session";
const REFRESH_AHEAD_MS = 60 * 1000;
const AUTH_EXPIRED_NOTIFY_COOLDOWN_MS = 3000;

const authState = reactive({
  initialized: false,
  user: null,
  accessToken: "",
  refreshToken: "",
  accessTokenExpiresAt: 0,
});

const AUTH_EXPIRED_EVENT = "cep:auth-expired";
const AUTH_STORAGE_SYNC_EVENT = "storage";

let refreshTimer = null;
let refreshPromise = null;
let lastAuthExpiredNotifyAt = 0;

const normalizeError = (error, fallback) => {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
};

const shouldClearSessionOnRefreshFailure = (error) => {
  const message = normalizeError(error, "");
  return message.includes("登录状态已过期") || message.includes("刷新令牌无效");
};

const notifyAuthExpired = (reasonMessage) => {
  const now = Date.now();
  if (now - lastAuthExpiredNotifyAt < AUTH_EXPIRED_NOTIFY_COOLDOWN_MS) {
    return;
  }
  lastAuthExpiredNotifyAt = now;

  window.dispatchEvent(
    new CustomEvent(AUTH_EXPIRED_EVENT, {
      detail: {
        message: reasonMessage || "登录状态已失效，请重新登录",
      },
    }),
  );
};

const clearRefreshTimer = () => {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
};

const scheduleRefresh = () => {
  clearRefreshTimer();
  if (!authState.refreshToken || !authState.accessTokenExpiresAt) {
    return;
  }

  const delay = Math.max(
    authState.accessTokenExpiresAt - Date.now() - REFRESH_AHEAD_MS,
    5000,
  );
  refreshTimer = window.setTimeout(() => {
    refreshAccessToken().catch((error) => {
      if (shouldClearSessionOnRefreshFailure(error)) {
        clearAuthSession();
        return;
      }
      scheduleRefresh();
    });
  }, delay);
};

const persistAuthState = () => {
  if (!authState.user || !authState.accessToken || !authState.refreshToken) {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return;
  }

  window.localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      user: authState.user,
      accessToken: authState.accessToken,
      refreshToken: authState.refreshToken,
      accessTokenExpiresAt: authState.accessTokenExpiresAt,
    }),
  );
};

const applyAuthSession = ({
  user,
  accessToken,
  refreshToken,
  accessTokenExpiresAt,
}) => {
  authState.user = user;
  authState.accessToken = accessToken;
  authState.refreshToken = refreshToken;
  authState.accessTokenExpiresAt = accessTokenExpiresAt;
  persistAuthState();
  scheduleRefresh();
};

const parseStoredSession = (raw) => {
  const parsed = JSON.parse(raw);
  if (
    !parsed?.user?.userId ||
    !parsed?.accessToken ||
    !parsed?.refreshToken ||
    !parsed?.accessTokenExpiresAt
  ) {
    throw new Error("invalid session payload");
  }

  return {
    user: parsed.user,
    accessToken: parsed.accessToken,
    refreshToken: parsed.refreshToken,
    accessTokenExpiresAt: parsed.accessTokenExpiresAt,
  };
};

const restoreFromStorage = () => {
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return;

  try {
    const parsed = parseStoredSession(raw);
    authState.user = parsed.user;
    authState.accessToken = parsed.accessToken;
    authState.refreshToken = parsed.refreshToken;
    authState.accessTokenExpiresAt = parsed.accessTokenExpiresAt;
    scheduleRefresh();
  } catch {
    clearAuthSession();
  }
};

const handleStorageSync = (event) => {
  if (event.key !== AUTH_STORAGE_KEY) {
    return;
  }

  if (!event.newValue) {
    clearRefreshTimer();
    authState.user = null;
    authState.accessToken = "";
    authState.refreshToken = "";
    authState.accessTokenExpiresAt = 0;
    return;
  }

  try {
    const parsed = parseStoredSession(event.newValue);
    authState.user = parsed.user;
    authState.accessToken = parsed.accessToken;
    authState.refreshToken = parsed.refreshToken;
    authState.accessTokenExpiresAt = parsed.accessTokenExpiresAt;
    scheduleRefresh();
  } catch {
    clearRefreshTimer();
    authState.user = null;
    authState.accessToken = "";
    authState.refreshToken = "";
    authState.accessTokenExpiresAt = 0;
  }
};

export const initAuthSession = async () => {
  if (authState.initialized) return;
  restoreFromStorage();
  window.addEventListener(AUTH_STORAGE_SYNC_EVENT, handleStorageSync);
  authState.initialized = true;

  if (!authState.refreshToken) return;

  if (authState.accessTokenExpiresAt <= Date.now() + 15 * 1000) {
    try {
      await refreshAccessToken();
    } catch (error) {
      if (shouldClearSessionOnRefreshFailure(error)) {
        clearAuthSession();
      } else {
        scheduleRefresh();
      }
    }
  }
};

export const saveAuthSession = (responseBody) => {
  const session = parseAuthSessionResponse(responseBody);
  applyAuthSession(session);
};

export const clearAuthSession = () => {
  clearRefreshTimer();
  authState.user = null;
  authState.accessToken = "";
  authState.refreshToken = "";
  authState.accessTokenExpiresAt = 0;
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
};

export const refreshAccessToken = async () => {
  if (!authState.refreshToken) {
    throw new Error("刷新令牌不存在");
  }

  if (!refreshPromise) {
    refreshPromise = refreshSession(authState.refreshToken)
      .then((responseBody) => {
        const session = parseAuthSessionResponse(responseBody);
        applyAuthSession(session);
        return session;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
};

export const fetchCurrentUser = async () => {
  if (!authState.accessToken) {
    throw new Error("尚未登录");
  }
  const responseBody = await fetchCurrentUserApi(authState.accessToken);
  return responseBody.data;
};

export const ensureValidAccessToken = async () => {
  if (!authState.refreshToken) {
    throw new Error("未登录");
  }

  if (
    authState.accessToken &&
    authState.accessTokenExpiresAt > Date.now() + 5000
  ) {
    return authState.accessToken;
  }

  try {
    await refreshAccessToken();
    return authState.accessToken;
  } catch (error) {
    if (shouldClearSessionOnRefreshFailure(error)) {
      clearAuthSession();
      notifyAuthExpired(normalizeError(error, "登录状态已过期，请重新登录"));
      throw new Error("登录状态已过期，请重新登录");
    }
    throw error;
  }
};

export const isLoggedIn = () =>
  Boolean(authState.user && authState.refreshToken);

export const logout = async () => {
  const token = authState.refreshToken;
  clearAuthSession();
  if (!token) return;

  try {
    await logoutSession(token);
  } catch (error) {
    const message = normalizeError(error, "退出登录失败");
    throw new Error(message);
  }
};

export { authState };
export { AUTH_EXPIRED_EVENT, notifyAuthExpired };
