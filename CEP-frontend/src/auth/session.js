import { reactive } from "vue";

const AUTH_API_BASE = "/api/auth";
const AUTH_STORAGE_KEY = "cep.auth.session";
const REFRESH_AHEAD_MS = 60 * 1000;

const authState = reactive({
  initialized: false,
  user: null,
  accessToken: "",
  refreshToken: "",
  accessTokenExpiresAt: 0,
});

let refreshTimer = null;
let refreshPromise = null;

const normalizeError = (error, fallback) => {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
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
    refreshAccessToken().catch(() => {
      clearAuthSession();
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

const parseAuthResponse = (payload) => {
  const data = payload?.data;
  if (!data?.accessToken || !data?.refreshToken || !data?.userId) {
    throw new Error("登录响应缺少令牌信息");
  }

  return {
    user: {
      userId: data.userId,
      email: data.email,
      username: data.username || "校园用户",
    },
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    accessTokenExpiresAt:
      Date.now() + Number(data.accessTokenExpiresInSeconds || 0) * 1000,
  };
};

const postJson = async (url, payload, headers = {}) => {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: JSON.stringify(payload),
  });

  const body = await response.json().catch(() => ({}));
  if (!response.ok || body.success === false) {
    const message = body?.message || "请求失败";
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return body;
};

const getJson = async (url, headers = {}) => {
  const response = await fetch(url, {
    method: "GET",
    headers,
  });

  const body = await response.json().catch(() => ({}));
  if (!response.ok || body.success === false) {
    const message = body?.message || "请求失败";
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return body;
};

const restoreFromStorage = () => {
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return;

  try {
    const parsed = JSON.parse(raw);
    if (
      !parsed?.user?.userId ||
      !parsed?.accessToken ||
      !parsed?.refreshToken ||
      !parsed?.accessTokenExpiresAt
    ) {
      throw new Error("invalid session payload");
    }

    authState.user = parsed.user;
    authState.accessToken = parsed.accessToken;
    authState.refreshToken = parsed.refreshToken;
    authState.accessTokenExpiresAt = parsed.accessTokenExpiresAt;
    scheduleRefresh();
  } catch {
    clearAuthSession();
  }
};

export const initAuthSession = async () => {
  if (authState.initialized) return;
  restoreFromStorage();
  authState.initialized = true;

  if (!authState.refreshToken) return;

  if (authState.accessTokenExpiresAt <= Date.now() + 15 * 1000) {
    try {
      await refreshAccessToken();
    } catch {
      clearAuthSession();
    }
  }
};

export const saveAuthSession = (responseBody) => {
  const session = parseAuthResponse(responseBody);
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
    refreshPromise = postJson(`${AUTH_API_BASE}/refresh`, {
      refreshToken: authState.refreshToken,
    })
      .then((responseBody) => {
        const session = parseAuthResponse(responseBody);
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
  const responseBody = await getJson(`${AUTH_API_BASE}/me`, {
    Authorization: `Bearer ${authState.accessToken}`,
  });
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

  await refreshAccessToken();
  return authState.accessToken;
};

export const isLoggedIn = () =>
  Boolean(authState.user && authState.refreshToken);

export const logout = async () => {
  const token = authState.refreshToken;
  clearAuthSession();
  if (!token) return;

  try {
    await postJson(`${AUTH_API_BASE}/logout`, { refreshToken: token });
  } catch (error) {
    const message = normalizeError(error, "退出登录失败");
    throw new Error(message);
  }
};

export { authState };
