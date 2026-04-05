import { ensureValidAccessToken } from "../common/authSessionService";

const PROFILE_API_BASE = "/api/profile";

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

export const fetchProfileOverview = async () =>
  requestJson(`${PROFILE_API_BASE}/overview`, {
    headers: await withAuthHeaders(),
  });

export const fetchProfileReviews = async (rating = "all") =>
  requestJson(
    `${PROFILE_API_BASE}/reviews?rating=${encodeURIComponent(rating)}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const fetchPublishedItems = async () =>
  requestJson(`${PROFILE_API_BASE}/trades/published`, {
    headers: await withAuthHeaders(),
  });

export const fetchSoldItems = async (status = "all") =>
  requestJson(
    `${PROFILE_API_BASE}/trades/sold?status=${encodeURIComponent(status)}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const fetchBoughtItems = async (status = "all") =>
  requestJson(
    `${PROFILE_API_BASE}/trades/bought?status=${encodeURIComponent(status)}`,
    {
      headers: await withAuthHeaders(),
    },
  );

export const fetchFavoriteItems = async () =>
  requestJson(`${PROFILE_API_BASE}/favorites`, {
    headers: await withAuthHeaders(),
  });

export const fetchPendingPaymentTrades = async () =>
  requestJson(`${PROFILE_API_BASE}/trades/pending-payment`, {
    headers: await withAuthHeaders(),
  });

export const updateProfileBasic = async (payload) =>
  requestJson(`${PROFILE_API_BASE}/basic`, {
    method: "PUT",
    headers: await withAuthHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(payload),
  });

export const uploadProfileAvatar = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return requestJson(`${PROFILE_API_BASE}/avatar`, {
    method: "POST",
    headers: await withAuthHeaders(),
    body: formData,
  });
};

const buildOtherProfileQuery = ({ userId, username }) => {
  const query = new URLSearchParams();
  if (Number.isInteger(userId) && userId > 0) {
    query.set("userId", String(userId));
  }
  if (typeof username === "string" && username.trim()) {
    query.set("username", username.trim());
  }
  return query.toString();
};

export const fetchOtherProfileOverview = async ({ userId, username }) => {
  const query = buildOtherProfileQuery({ userId, username });
  return requestJson(`${PROFILE_API_BASE}/other/overview?${query}`, {
    headers: await withAuthHeaders(),
  });
};

export const fetchOtherProfileItems = async ({
  userId,
  username,
  status = "all",
  sort = "price-desc",
}) => {
  const query = new URLSearchParams(
    buildOtherProfileQuery({ userId, username }),
  );
  query.set("status", status);
  query.set("sort", sort);
  return requestJson(`${PROFILE_API_BASE}/other/items?${query.toString()}`, {
    headers: await withAuthHeaders(),
  });
};

export const fetchOtherProfileReviews = async ({
  userId,
  username,
  rating = "all",
}) => {
  const query = new URLSearchParams(
    buildOtherProfileQuery({ userId, username }),
  );
  query.set("rating", rating);
  return requestJson(`${PROFILE_API_BASE}/other/reviews?${query.toString()}`, {
    headers: await withAuthHeaders(),
  });
};

export const followOtherProfile = async ({ userId, username }) => {
  const query = buildOtherProfileQuery({ userId, username });
  return requestJson(`${PROFILE_API_BASE}/other/follow?${query}`, {
    method: "POST",
    headers: await withAuthHeaders(),
  });
};

export const unfollowOtherProfile = async ({ userId, username }) => {
  const query = buildOtherProfileQuery({ userId, username });
  return requestJson(`${PROFILE_API_BASE}/other/follow?${query}`, {
    method: "DELETE",
    headers: await withAuthHeaders(),
  });
};
