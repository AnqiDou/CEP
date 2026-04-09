import { ensureValidAccessToken } from "../common/authSessionService";

const PAYMENT_API_BASE = "/api/payment/orders";

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

export const createTradeOrder = async (payload) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(PAYMENT_API_BASE, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });
};

export const fetchTradeOrder = (orderId) =>
  requestJson(`${PAYMENT_API_BASE}/${orderId}`);

export const fetchTradeOrderDetail = async (orderId) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PAYMENT_API_BASE}/${orderId}/detail`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
};

export const markTradeOrderPaid = (orderId) =>
  requestJson(`${PAYMENT_API_BASE}/${orderId}/pay-success`, {
    method: "POST",
  });

export const cancelTradeOrder = async (orderId) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PAYMENT_API_BASE}/${orderId}/cancel`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
};

export const confirmSellerDelivered = async (orderId) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(
    `${PAYMENT_API_BASE}/${orderId}/seller-confirm-delivered`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );
};

export const confirmBuyerReceived = async (orderId) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PAYMENT_API_BASE}/${orderId}/buyer-confirm-received`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
};

export const applyTradeOrderRefund = async (orderId, refundType) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PAYMENT_API_BASE}/${orderId}/refund/apply`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ refundType }),
  });
};

export const approveTradeOrderRefund = async (orderId) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PAYMENT_API_BASE}/${orderId}/refund/approve`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
};

export const rejectTradeOrderRefund = async (orderId) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PAYMENT_API_BASE}/${orderId}/refund/reject`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
};
