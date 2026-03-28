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
