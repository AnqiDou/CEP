const HOME_API_BASE = "/api/home";

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

const buildQuery = (params) => {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === "") {
      return;
    }
    searchParams.append(key, String(value));
  });
  const query = searchParams.toString();
  return query ? `?${query}` : "";
};

export const fetchHomeCategories = () =>
  requestJson(`${HOME_API_BASE}/categories`);

export const fetchHomeItems = ({
  keyword,
  categoryId,
  opsColumn,
  sortBy,
  sortOrder,
  page = 1,
  size = 12,
}) =>
  requestJson(
    `${HOME_API_BASE}/items${buildQuery({
      keyword,
      categoryId,
      opsColumn,
      sortBy,
      sortOrder,
      page,
      size,
    })}`,
  );

export const fetchHotItems = (limit = 8) =>
  requestJson(`${HOME_API_BASE}/hot-items${buildQuery({ limit })}`);

export const fetchHotKeywords = (limit = 10) =>
  requestJson(`${HOME_API_BASE}/hot-keywords${buildQuery({ limit })}`);
