export const mapAuthUser = (rawUser) => ({
  userId: rawUser?.userId || "",
  email: rawUser?.email || "",
  username: rawUser?.username || "校园用户",
});

export const parseAuthSessionResponse = (payload) => {
  const data = payload?.data;
  if (!data?.accessToken || !data?.refreshToken || !data?.userId) {
    throw new Error("登录响应缺少令牌信息");
  }

  return {
    user: mapAuthUser(data),
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    accessTokenExpiresAt:
      Date.now() + Number(data.accessTokenExpiresInSeconds || 0) * 1000,
  };
};
