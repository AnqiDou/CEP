const AUTH_API_BASE = "/api/auth";

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

export const postAuthJson = (path, payload, headers = {}) =>
  requestJson(`${AUTH_API_BASE}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: JSON.stringify(payload),
  });

export const getAuthJson = (path, headers = {}) =>
  requestJson(`${AUTH_API_BASE}${path}`, {
    method: "GET",
    headers,
  });
