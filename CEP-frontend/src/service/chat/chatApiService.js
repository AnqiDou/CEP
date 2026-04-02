import { ensureValidAccessToken } from "../common/authSessionService";

const MESSAGE_API_BASE = "/api/messages";

const requestJson = async (url, options = {}) => {
  try {
    const response = await fetch(url, options);
    const body = await response.json().catch(() => ({}));

    if (!response.ok || body.success === false) {
      throw new Error(body?.message || "请求失败");
    }

    return body;
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error("无法连接后端服务，请确认后端已启动且前端通过 Vite 访问");
    }
    throw error;
  }
};

const withAuthHeaders = async (extraHeaders = {}) => {
  const accessToken = await ensureValidAccessToken();
  return {
    Authorization: `Bearer ${accessToken}`,
    ...extraHeaders,
  };
};

export const fetchMessageConversations = async (filter = "all") =>
  requestJson(
    `${MESSAGE_API_BASE}/conversations?filter=${encodeURIComponent(filter)}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const fetchConversationMessages = async (conversationId) =>
  requestJson(`${MESSAGE_API_BASE}/conversations/${conversationId}/messages`, {
    headers: await withAuthHeaders(),
  });

export const createOrGetDirectConversation = async ({ peerUserId, itemId }) =>
  requestJson(`${MESSAGE_API_BASE}/conversations/direct`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ peerUserId, itemId }),
  });

export const markConversationRead = async (conversationId) =>
  requestJson(`${MESSAGE_API_BASE}/conversations/${conversationId}/read`, {
    method: "POST",
    headers: await withAuthHeaders(),
  });

export const buildMessageWebSocketUrl = async () => {
  const accessToken = await ensureValidAccessToken();
  const wsBaseUrl = (import.meta.env.VITE_WS_BASE_URL || "").trim();
  if (wsBaseUrl) {
    const base = wsBaseUrl.endsWith("/")
      ? wsBaseUrl.slice(0, wsBaseUrl.length - 1)
      : wsBaseUrl;
    return `${base}/ws/messages?accessToken=${encodeURIComponent(accessToken)}`;
  }

  if (import.meta.env.PROD) {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const host = window.location.host;
    return `${protocol}//${host}/ws/messages?accessToken=${encodeURIComponent(accessToken)}`;
  }

  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const host = window.location.hostname;
  const port = import.meta.env.VITE_BACKEND_PORT || "8080";
  const path = "/ws/messages";
  return `${protocol}//${host}:${port}${path}?accessToken=${encodeURIComponent(accessToken)}`;
};
