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

export const uploadPublishImage = async (file) => {
  const accessToken = await ensureValidAccessToken();
  const formData = new FormData();
  formData.append("file", file);

  return requestJson(`${PUBLISH_API_BASE}/images`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: formData,
  });
};

export const createPublishItem = async (payload) => {
  const accessToken = await ensureValidAccessToken();
  return requestJson(`${PUBLISH_API_BASE}/items`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });
};
