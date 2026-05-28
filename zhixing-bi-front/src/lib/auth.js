"use client";

const USERS_KEY = "zhixing_users";
const SESSION_KEY = "zhixing_session";

function getUsers() {
  if (typeof window === "undefined") return {};
  try { return JSON.parse(localStorage.getItem(USERS_KEY)) || {}; }
  catch { return {}; }
}

function saveUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users));
}

export function register(username, password) {
  const users = getUsers();
  if (users[username]) return { ok: false, error: "账号已存在" };
  users[username] = { password, createdAt: Date.now() };
  saveUsers(users);
  setAuth({ username });
  return { ok: true };
}

export function login(username, password) {
  const users = getUsers();
  const user = users[username];
  if (!user) return { ok: false, error: "账号不存在" };
  if (user.password !== password) return { ok: false, error: "密码错误" };
  setAuth({ username });
  return { ok: true };
}

export function getAuth() {
  if (typeof window === "undefined") return null;
  try {
    const data = localStorage.getItem(SESSION_KEY);
    return data ? JSON.parse(data) : null;
  } catch {
    return null;
  }
}

function setAuth(user) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
}

export function clearAuth() {
  localStorage.removeItem(SESSION_KEY);
}

export function isLoggedIn() {
  return !!getAuth();
}
