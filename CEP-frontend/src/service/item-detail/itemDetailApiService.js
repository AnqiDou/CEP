import { ensureValidAccessToken } from "../common/authSessionService";

const ITEM_DETAIL_API_BASE = "/api/items";

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

export const fetchItemDetail = (itemId) =>
  requestJson(`${ITEM_DETAIL_API_BASE}/${itemId}/detail`);

const withAuthHeaders = async (extraHeaders = {}) => {
  const accessToken = await ensureValidAccessToken();
  return {
    Authorization: `Bearer ${accessToken}`,
    ...extraHeaders,
  };
};

export const fetchItemFavoriteStatus = async (itemId) =>
  requestJson(`${ITEM_DETAIL_API_BASE}/${itemId}/favorite`, {
    headers: await withAuthHeaders(),
  });

export const addItemFavorite = async (itemId) =>
  requestJson(`${ITEM_DETAIL_API_BASE}/${itemId}/favorite`, {
    method: "POST",
    headers: await withAuthHeaders(),
  });

export const removeItemFavorite = async (itemId) =>
  requestJson(`${ITEM_DETAIL_API_BASE}/${itemId}/favorite`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });

export const reportItem = async (itemId, payload) =>
  requestJson(`${ITEM_DETAIL_API_BASE}/${itemId}/reports`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(payload || {}),
  });
