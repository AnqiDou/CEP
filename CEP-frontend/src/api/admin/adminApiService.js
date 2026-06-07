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

export const fetchAdminUsers = async (params = {}) => {
  const normalized =
    typeof params === "string" ? { keyword: params } : { ...(params || {}) };
  const query = new URLSearchParams();
  ["keyword", "username", "phone", "email"].forEach((key) => {
    const value = String(normalized[key] || "").trim();
    if (value) query.set(key, value);
  });
  return requestJson(`${ADMIN_API_BASE}/users?${query.toString()}`, {
    headers: await withAuthHeaders(),
  });
};

export const updateAdminUserStatus = async (userId, disabled) =>
  requestJson(`${ADMIN_API_BASE}/users/${userId}/status`, {
    method: "PATCH",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ disabled }),
  });

export const updateAdminUserCreditScore = async (userId, role, creditScore) =>
  requestJson(`${ADMIN_API_BASE}/users/${userId}/credit-score`, {
    method: "PATCH",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ role, creditScore }),
  });

export const deleteAdminUser = async (userId) =>
  requestJson(`${ADMIN_API_BASE}/users/${userId}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });

export const fetchAdminItems = async (params = {}) => {
  const normalized = {
    keyword: "",
    title: "",
    category: "",
    price: "",
    publisher: "",
    status: "all",
    ...(params || {}),
  };
  const query = new URLSearchParams();
  ["keyword", "title", "category", "price", "publisher", "status"].forEach(
    (key) => {
      const value = String(normalized[key] || "").trim();
      if (value) query.set(key, value);
    },
  );
  return requestJson(`${ADMIN_API_BASE}/items?${query.toString()}`, {
    headers: await withAuthHeaders(),
  });
};

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

export const restoreAdminItem = async (itemId) =>
  requestJson(`${ADMIN_API_BASE}/items/${itemId}/restore`, {
    method: "POST",
    headers: await withAuthHeaders(),
  });

export const deleteAdminItem = async (itemId) =>
  requestJson(`${ADMIN_API_BASE}/items/${itemId}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });

export const fetchAdminOrders = async (params = {}) => {
  const normalized = {
    keyword: "",
    orderNo: "",
    buyer: "",
    seller: "",
    itemTitle: "",
    status: "all",
    ...(params || {}),
  };
  const query = new URLSearchParams();
  ["keyword", "orderNo", "buyer", "seller", "itemTitle", "status"].forEach(
    (key) => {
      const value = String(normalized[key] || "").trim();
      if (value) query.set(key, value);
    },
  );
  return requestJson(`${ADMIN_API_BASE}/orders?${query.toString()}`, {
    headers: await withAuthHeaders(),
  });
};

export const handleAdminOrderAbnormal = async (orderNo) =>
  requestJson(
    `${ADMIN_API_BASE}/orders/${encodeURIComponent(orderNo)}/handle-abnormal`,
    {
      method: "POST",
      headers: await withAuthHeaders(),
    },
  );

export const updateAdminOrder = async (orderNo, payload = {}) =>
  requestJson(`${ADMIN_API_BASE}/orders/${encodeURIComponent(orderNo)}`, {
    method: "PATCH",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({
      status: payload?.status || null,
      refundStatus: payload?.refundStatus || null,
    }),
  });

export const fetchAdminConversations = async () =>
  requestJson(`${ADMIN_API_BASE}/support/conversations`, {
    headers: await withAuthHeaders(),
  });

export const replyAdminConversation = async (conversationId, payload) =>
  requestJson(
    `${ADMIN_API_BASE}/support/conversations/${conversationId}/messages`,
    {
      method: "POST",
      headers: await withAuthHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({
        content: String(payload?.content || "").trim(),
        imageUrl: String(payload?.imageUrl || "").trim(),
      }),
    },
  );

export const resolveAdminConversation = async (conversationId) =>
  requestJson(
    `${ADMIN_API_BASE}/support/conversations/${conversationId}/resolve`,
    {
      method: "POST",
      headers: await withAuthHeaders(),
    },
  );

export const fetchMySupportMessages = async () =>
  requestJson(`${ADMIN_API_BASE}/support/me/messages`, {
    headers: await withAuthHeaders(),
  });

export const sendMySupportMessage = async (content, orderId = null) =>
  requestJson(`${ADMIN_API_BASE}/support/me/messages`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({ content, orderId }),
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

export const fetchAdminSensitiveWords = async () =>
  requestJson(`${ADMIN_API_BASE}/sensitive-words`, {
    headers: await withAuthHeaders(),
  });

export const createAdminSensitiveWord = async (payload = {}) =>
  requestJson(`${ADMIN_API_BASE}/sensitive-words`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({
      category: String(payload?.category || "").trim(),
      word: String(payload?.word || "").trim(),
      enabled: payload?.enabled !== false,
    }),
  });

export const updateAdminSensitiveWord = async (id, payload = {}) =>
  requestJson(`${ADMIN_API_BASE}/sensitive-words/${id}`, {
    method: "PUT",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify({
      category: String(payload?.category || "").trim(),
      word: String(payload?.word || "").trim(),
      enabled: payload?.enabled !== false,
    }),
  });

export const deleteAdminSensitiveWord = async (id) =>
  requestJson(`${ADMIN_API_BASE}/sensitive-words/${id}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });

export const fetchAdminReviews = async (params = {}) => {
  const normalized = {
    keyword: "",
    role: "",
    rating: "",
    ...(params || {}),
  };
  const query = new URLSearchParams();
  ["keyword", "role", "rating"].forEach((key) => {
    const value = String(normalized[key] || "").trim();
    if (value) query.set(key, value);
  });
  return requestJson(`${ADMIN_API_BASE}/reviews?${query.toString()}`, {
    headers: await withAuthHeaders(),
  });
};

export const deleteAdminReview = async (reviewId) =>
  requestJson(`${ADMIN_API_BASE}/reviews/${reviewId}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });
