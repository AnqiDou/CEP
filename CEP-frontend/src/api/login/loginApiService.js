import { getAuthJson, postAuthJson } from "../common/httpClient";

export const sendRegisterCode = (email) =>
  postAuthJson("/send-register-code", { email });

export const verifyRegisterCode = (email, code) =>
  postAuthJson("/verify-register-code", { email, code });

export const sendResetPasswordCode = (email) =>
  postAuthJson("/send-reset-password-code", { email });

export const verifyResetPasswordCode = (email, code) =>
  postAuthJson("/verify-reset-password-code", { email, code });

export const registerUser = ({
  email,
  code,
  username,
  password,
  name,
  phone,
  address,
}) =>
  postAuthJson("/register", {
    email,
    code,
    username,
    password,
    name,
    phone,
    address,
  });

export const loginUser = (email, password) =>
  postAuthJson("/login", { email, password });

export const resetPassword = (email, code, password) =>
  postAuthJson("/reset-password", { email, code, password });

export const refreshSession = (refreshToken) =>
  postAuthJson("/refresh", { refreshToken });

export const logoutSession = (refreshToken) =>
  postAuthJson("/logout", { refreshToken });

export const fetchCurrentUser = (accessToken) =>
  getAuthJson("/me", {
    Authorization: `Bearer ${accessToken}`,
  });
