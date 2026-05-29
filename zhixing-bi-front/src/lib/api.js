import { getToken } from "./auth";

export const API_BASE = typeof window !== "undefined" && window.location.hostname === "localhost"
  ? "http://localhost:8099"
  : "https://zhixingbi.vip.cpolar.cn";

export function authHeaders() {
  const token = getToken();
  return token ? { Authorization: "Bearer " + token } : {};
}
