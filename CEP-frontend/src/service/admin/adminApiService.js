import { ensureValidAccessToken } from "../common/authSessionService";

const ADMIN_API_BASE = "/api/admin";

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
      throw new Error("无法连接后端服务，请确认后端已启动");
    }
    throw error;
  }
};

const withAuthHeaders = async (extra = {}) => {
  const accessToken = await ensureValidAccessToken();
  return {
    Authorization: `Bearer ${accessToken}`,
    ...extra,
  };
};

export const fetchAdminDashboard = async () =>
  requestJson(`${ADMIN_API_BASE}/dashboard`, {
    headers: await withAuthHeaders(),
  });

export const fetchAdminUsers = async (keyword = "") =>
  requestJson(
    `${ADMIN_API_BASE}/users?keyword=${encodeURIComponent(keyword.trim())}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const updateAdminUserStatus = async (userId, disabled) =>
  requestJson(`${ADMIN_API_BASE}/users/${userId}/status`, {
    method: "PATCH",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ disabled }),
  });

export const deleteAdminUser = async (userId) =>
  requestJson(`${ADMIN_API_BASE}/users/${userId}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });

export const fetchAdminItems = async ({ keyword = "", status = "all" } = {}) =>
  requestJson(
    `${ADMIN_API_BASE}/items?keyword=${encodeURIComponent(keyword.trim())}&status=${encodeURIComponent(status)}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const approveAdminItem = async (itemId) =>
  requestJson(`${ADMIN_API_BASE}/items/${itemId}/approve`, {
    method: "POST",
    headers: await withAuthHeaders(),
  });

export const offlineAdminItem = async (itemId) =>
  requestJson(`${ADMIN_API_BASE}/items/${itemId}/offline`, {
    method: "POST",
    headers: await withAuthHeaders(),
  });

export const deleteAdminItem = async (itemId) =>
  requestJson(`${ADMIN_API_BASE}/items/${itemId}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });

export const fetchAdminOrders = async ({ keyword = "", status = "all" } = {}) =>
  requestJson(
    `${ADMIN_API_BASE}/orders?keyword=${encodeURIComponent(keyword.trim())}&status=${encodeURIComponent(status)}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const handleAdminOrderAbnormal = async (orderNo) =>
  requestJson(
    `${ADMIN_API_BASE}/orders/${encodeURIComponent(orderNo)}/handle-abnormal`,
    {
      method: "POST",
      headers: await withAuthHeaders(),
    },
  );

export const fetchAdminConversations = async () =>
  requestJson(`${ADMIN_API_BASE}/support/conversations`, {
    headers: await withAuthHeaders(),
  });

export const replyAdminConversation = async (conversationId, content) =>
  requestJson(
    `${ADMIN_API_BASE}/support/conversations/${conversationId}/messages`,
    {
      method: "POST",
      headers: await withAuthHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ content }),
    },
  );

export const updateAdminConversationStatus = async (conversationId, status) =>
  requestJson(
    `${ADMIN_API_BASE}/support/conversations/${conversationId}/status`,
    {
      method: "PATCH",
      headers: await withAuthHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ status }),
    },
  );

export const fetchMySupportMessages = async () =>
  requestJson(`${ADMIN_API_BASE}/support/me/messages`, {
    headers: await withAuthHeaders(),
  });

export const sendMySupportMessage = async (content) =>
  requestJson(`${ADMIN_API_BASE}/support/me/messages`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ content }),
  });

export const fetchAdminNotices = async () =>
  requestJson(`${ADMIN_API_BASE}/notices`, {
    headers: await withAuthHeaders(),
  });

export const createAdminNotice = async (content) =>
  requestJson(`${ADMIN_API_BASE}/notices`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ content }),
  });

export const deleteAdminNotice = async (noticeId) =>
  requestJson(`${ADMIN_API_BASE}/notices/${noticeId}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });
