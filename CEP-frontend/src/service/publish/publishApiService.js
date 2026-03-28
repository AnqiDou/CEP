import { ensureValidAccessToken } from "../common/authSessionService";

const PUBLISH_API_BASE = "/api/publish";

const requestJson = async (url, options) => {
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

const getAuthHeader = async () => {
  const accessToken = await ensureValidAccessToken();
  return {
    Authorization: `Bearer ${accessToken}`,
  };
};

export const uploadPublishImage = async (file) => {
  const authHeader = await getAuthHeader();
  const formData = new FormData();
  formData.append("file", file);

  return requestJson(`${PUBLISH_API_BASE}/images`, {
    method: "POST",
    headers: authHeader,
    body: formData,
  });
};

export const createPublishItem = async (payload) => {
  const authHeader = await getAuthHeader();
  return requestJson(`${PUBLISH_API_BASE}/items`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader,
    },
    body: JSON.stringify(payload),
  });
};

export const fetchMyPublishItems = async () => {
  const authHeader = await getAuthHeader();
  return requestJson(`${PUBLISH_API_BASE}/items/mine`, {
    method: "GET",
    headers: authHeader,
  });
};

export const updateMyPublishItem = async (itemId, payload) => {
  const authHeader = await getAuthHeader();
  return requestJson(`${PUBLISH_API_BASE}/items/${itemId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      ...authHeader,
    },
    body: JSON.stringify(payload),
  });
};

export const deleteMyPublishItem = async (itemId) => {
  const authHeader = await getAuthHeader();
  return requestJson(`${PUBLISH_API_BASE}/items/${itemId}`, {
    method: "DELETE",
    headers: authHeader,
  });
};

export const updateMyPublishItemStatus = async (itemId, status) => {
  const authHeader = await getAuthHeader();
  return requestJson(`${PUBLISH_API_BASE}/items/${itemId}/status`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      ...authHeader,
    },
    body: JSON.stringify({ status }),
  });
};
