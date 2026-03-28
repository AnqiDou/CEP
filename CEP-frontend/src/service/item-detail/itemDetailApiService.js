const ITEM_DETAIL_API_BASE = "/api/items";

const requestJson = async (url) => {
  try {
    const response = await fetch(url);
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
