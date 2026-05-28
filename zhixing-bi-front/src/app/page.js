"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import ParticleBackground from "@/components/ParticleBackground";
import { setAuth } from "@/lib/auth";
import { API_BASE } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPwd, setConfirmPwd] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!username.trim() || !password.trim()) {
      setError("请填写用户名和密码");
      return;
    }
    if (mode === "register" && password !== confirmPwd) {
      setError("两次密码不一致");
      return;
    }
    if (password.length < 4) {
      setError("密码至少4位");
      return;
    }

    try {
      const endpoint = mode === "login" ? "/api/user/login" : "/api/user/register";
      const fd = new URLSearchParams();
      fd.append("username", username.trim());
      fd.append("password", password);
      const res = await fetch(`${API_BASE}${endpoint}`, { method: "POST", body: fd });
      const json = await res.json();
      if (json.code === 200) {
        setAuth({ username: json.data.username, token: json.data.token });
        router.push("/chat");
      } else {
        setError(json.message);
      }
    } catch (e) {
      setError("网络错误，请确认后端服务已启动");
    }
  };

  return (
    <div className="flex h-screen items-center justify-center bg-white relative overflow-hidden">
      <ParticleBackground />

      <div className="relative z-10 w-full max-w-sm mx-4">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-blue-600 tracking-widest">ZhiXing-BI</h1>
          <p className="text-xs text-slate-400 mt-2">智能商业数据分析平台</p>
        </div>

        <div className="glass-panel rounded-2xl p-8">
          <div className="flex mb-6 border-b border-blue-100">
            <button
              onClick={() => { setMode("login"); setError(""); }}
              className={`flex-1 pb-2 text-sm font-medium transition-colors ${mode === "login" ? "text-blue-600 border-b-2 border-blue-500" : "text-slate-400"}`}
            >登录</button>
            <button
              onClick={() => { setMode("register"); setError(""); }}
              className={`flex-1 pb-2 text-sm font-medium transition-colors ${mode === "register" ? "text-blue-600 border-b-2 border-blue-500" : "text-slate-400"}`}
            >注册</button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="用户名"
                className="w-full px-4 py-2.5 bg-slate-50 border border-blue-200 rounded-xl text-sm text-slate-700 placeholder-slate-400 focus:outline-none focus:border-blue-400 transition-colors"
              />
            </div>
            <div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="密码"
                className="w-full px-4 py-2.5 bg-slate-50 border border-blue-200 rounded-xl text-sm text-slate-700 placeholder-slate-400 focus:outline-none focus:border-blue-400 transition-colors"
              />
            </div>
            {mode === "register" && (
              <div>
                <input
                  type="password"
                  value={confirmPwd}
                  onChange={(e) => setConfirmPwd(e.target.value)}
                  placeholder="确认密码"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-blue-200 rounded-xl text-sm text-slate-700 placeholder-slate-400 focus:outline-none focus:border-blue-400 transition-colors"
                />
              </div>
            )}

            {error && <p className="text-xs text-red-500">{error}</p>}

            <button
              type="submit"
              className="w-full py-2.5 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-xl transition-all"
            >
              {mode === "login" ? "登录" : "注册"}
            </button>
          </form>

          <p className="text-[10px] text-slate-400 text-center mt-4">
            {mode === "login" ? "没有账号？" : "已有账号？"}
            <button onClick={() => setMode(mode === "login" ? "register" : "login")} className="text-blue-500 ml-1 hover:underline">
              {mode === "login" ? "立即注册" : "去登录"}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
