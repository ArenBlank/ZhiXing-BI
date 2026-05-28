"use client";

import { useState, useEffect, useRef } from "react";
import HistorySidebar from "@/components/HistorySidebar";
import FileUploader from "@/components/FileUploader";
import ChatConsole from "@/components/ChatConsole";
import ParticleBackground from "@/components/ParticleBackground";
import { getAuth, clearAuth, isLoggedIn } from "@/lib/auth";
import { useRouter } from "next/navigation";

import { API_BASE, authHeaders } from "@/lib/api";
const MOCK_MESSAGES = [
  { role: "user", content: "西北大学是211大学吗？" },
  { role: "assistant", content: "是的，西北大学（Northwest University）位于陕西省西安市，是中国国家\"211工程\"重点建设高校之一，也是首批国家\"双一流\"世界一流学科建设高校。" },
  { role: "user", content: "那它有多少学生？" },
  { role: "assistant", content: "西北大学现有在校学生约 25,000 余人，其中本科生约 13,000 人，研究生约 8,000 人（含硕士和博士研究生），留学生约 400 人。" },
];

export default function ChatPage() {
  const router = useRouter();
  const auth = getAuth();
  const [mounted, setMounted] = useState(false);
  const [authorized, setAuthorized] = useState(false);
  const [userId, setUserId] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [webSearchOn, setWebSearchOn] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [sidebarKey, setSidebarKey] = useState(0);
  const chatRef = useRef(null);
  const initRef = useRef(false);

  useEffect(() => {
    if (!isLoggedIn()) { router.replace("/"); return; }
    setAuthorized(true);
  }, []);

  useEffect(() => {
    if (!authorized) return;
    setUserId("user" + btoa(auth.username).slice(0, 12));
    setMounted(true);
  }, [authorized]);
  useEffect(() => { if (!userId || initRef.current) return; initRef.current = true; createSession(); }, [userId]);

  const createSession = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/session/create?userId=${userId}&title=新会话`, { method: "POST", headers: authHeaders() });
      const json = await res.json();
      if (json.code === 200) { setSessionId(json.data.sessionId); setMessages([]); setSidebarKey(k => k + 1); return; }
    } catch {}
    setSessionId("offline-session"); setMessages(MOCK_MESSAGES); setSidebarKey(k => k + 1);
  };

  const switchSession = async (sid) => {
    setSessionId(sid);
    try {
      const res = await fetch(`${API_BASE}/api/session/history?userId=${userId}&sessionId=${sid}`);
      const json = await res.json();
      if (json.code === 200) { setMessages(json.data.map(m => ({ role: m.role, content: m.content }))); return; }
    } catch {}
    setMessages(MOCK_MESSAGES);
  };

  const handleSend = async (prompt) => {
    if (!sessionId || sessionId === "offline-session") {
      setMessages(prev => [...prev, { role: "user", content: prompt }, { role: "assistant", content: "[Mock 模式] 后端未连接，当前为离线演示。" }]);
      return;
    }
    setMessages(prev => [...prev, { role: "user", content: prompt }]);
    const thinkId = Date.now();
    setMessages(prev => [...prev, { role: "assistant", content: "ZhiXing-BI 正在思考中，请稍后...", thinking: true, id: thinkId }]);
    chatRef.current?.scrollToBottom();
    try {
      // 自动注入已上传文件上下文
      let contextPrefix = "";
      if (uploadedFiles.length === 1) {
        contextPrefix = "【用户已上传文件：" + uploadedFiles[0] + "，如用户提问涉及总结、分析、讲一下等模糊指令，默认指的是此文件】";
      } else if (uploadedFiles.length > 1) {
        const needFileHint = /总结|分析|讲一下|讲讲|看看|这个|内容|文件|文档/.test(prompt);
        if (needFileHint) {
          contextPrefix = "【用户已上传以下文件：" + uploadedFiles.join("、") + "。如果用户没有明确指定文件，请先询问用户指的是哪个文件】";
        }
      }
      const finalPrompt = (webSearchOn ? "【用户要求联网搜索，请务必调用webSearch工具搜索最新信息】" : "") + contextPrefix + prompt;
      const fd = new URLSearchParams(); fd.append("userId", userId); fd.append("sessionId", sessionId); fd.append("userPrompt", finalPrompt);
      const res = await fetch(`${API_BASE}/api/agent/chat`, { method: "POST", headers: authHeaders(), body: fd });
      const json = await res.json();
      if (json.code === 200) {
        setMessages(prev => prev.map(m => m.id === thinkId ? { role: "assistant", content: json.data.response } : m));
        setSidebarKey(k => k + 1);
      } else setMessages(prev => prev.map(m => m.id === thinkId ? { role: "assistant", content: `错误: ${json.message}` } : m));
    } catch (e) { setMessages(prev => prev.map(m => m.id === thinkId ? { role: "assistant", content: "请求失败: " + e.message } : m)); }
    chatRef.current?.scrollToBottom();
  };

  const handleFileUploaded = (result) => {
    setUploadedFiles(prev => { if (prev.includes(result.fileName)) return prev; return [...prev, result.fileName]; });
    setSidebarKey(k => k + 1);
  };

  if (!authorized || !mounted) return (
    <div className="flex h-screen overflow-hidden bg-white">
      <ParticleBackground />
      <div className="flex-1 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-blue-200 border-t-blue-500 rounded-full animate-spin" />
      </div>
    </div>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-white">
      <ParticleBackground />
      <HistorySidebar userId={userId} activeSessionId={sessionId} onSwitch={switchSession} onNew={createSession} onDelete={(sid) => { if (sid === sessionId) createSession(); setSidebarKey(k => k + 1); }} refreshKey={sidebarKey} />
      <main className="flex-1 flex flex-col min-w-0">
        <header className="h-14 flex items-center px-6 border-b border-blue-200 glass-panel shrink-0">
          <span className="text-sm font-bold tracking-widest text-blue-600 uppercase">ZhiXing-BI</span>
          <span className="text-xs text-slate-400 mx-2">{auth?.username}</span>
          <span className="ml-auto text-xs text-slate-400 mr-3">{sessionId === "offline-session" ? "⚠ Mock 演示模式" : sessionId ? `会话: ${sessionId.slice(0, 16)}...` : "创建会话中..."}</span>
          <button onClick={() => { clearAuth(); router.push("/"); }} className="text-xs text-slate-400 hover:text-red-500 transition-colors">退出</button>
        </header>
        <div className="flex-1 flex flex-col overflow-hidden px-6 py-4 gap-4">
          <FileUploader userId={userId} sessionId={sessionId} onUploaded={handleFileUploaded} />
          <ChatConsole ref={chatRef} messages={messages} onSend={handleSend} webSearchOn={webSearchOn} setWebSearchOn={setWebSearchOn} />
        </div>
      </main>
    </div>
  );
}
