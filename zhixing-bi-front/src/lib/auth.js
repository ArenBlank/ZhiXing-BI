"use client";

const TOKEN_KEY = "zhixing_session";

export function setAuth(data) {
  localStorage.setItem(TOKEN_KEY, JSON.stringify(data));
}

export function getAuth() {
  if (typeof window === "undefined") return null;
  try { return JSON.parse(localStorage.getItem(TOKEN_KEY)); }
  catch { return null; }
}

export function getToken() {
  return getAuth()?.token || "";
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
}

export function isLoggedIn() {
  const auth = getAuth();
  return !!(auth && auth.token);
}
