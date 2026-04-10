import { ensureValidAccessToken } from "../common/authSessionService";

const REVIEW_API_BASE = "/api/reviews/orders";

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

export const fetchReviewOrderDetail = async (orderId) =>
  requestJson(`${REVIEW_API_BASE}/${orderId}`, {
    headers: await withAuthHeaders(),
  });

export const submitTradeReview = async (orderId, payload) =>
  requestJson(`${REVIEW_API_BASE}/${orderId}`, {
    method: "POST",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(payload),
  });
