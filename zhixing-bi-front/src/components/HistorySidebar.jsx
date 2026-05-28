"use client";

import { useState, useEffect, useRef } from "react";
import { API_BASE } from "@/lib/api";

export default function HistorySidebar({ userId, activeSessionId, onSwitch, onNew, onDelete, refreshKey }) {
  const [sessions, setSessions] = useState([]);
  const [collapsed, setCollapsed] = useState(false);
  const listRef = useRef(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/session/list?userId=${userId}`)
      .then(r => r.json()).then(json => { if (json.code === 200) setSessions(json.data); }).catch(() => {});
  }, [userId, refreshKey]);

  useEffect(() => {
    if (!listRef.current || sessions.length === 0) return;
    (async () => {
      const { set, animate, stagger } = await import("animejs");
      const items = listRef.current.querySelectorAll(".session-item");
      set(items, { translateY: -20, opacity: 0 });
      animate(items, { translateY: [0, 0], opacity: [0, 1], delay: stagger(50, { start: 50 }), duration: 500, ease: "spring(1, 80, 10, 0)" });
    })();
  }, [sessions]);

  const handleDelete = async (e, sessionId) => {
    e.stopPropagation();
    await fetch(`${API_BASE}/api/session/clear?userId=${userId}&sessionId=${sessionId}`, { method: "DELETE" });
    setSessions(prev => prev.filter(s => s.sessionId !== sessionId));
    onDelete?.(sessionId);
  };

  return (
    <aside className={`glass-panel shrink-0 flex flex-col transition-all duration-300 ${collapsed ? "w-11" : "w-60"}`}>
      <div className="flex items-center justify-between p-3 border-b border-blue-100">
        {!collapsed && <span className="text-xs font-semibold tracking-widest text-blue-600 uppercase">历史会话</span>}
        <button onClick={() => setCollapsed(!collapsed)} className="text-slate-400 hover:text-blue-600 transition-colors text-sm">{collapsed ? "▶" : "◀"}</button>
      </div>
      {!collapsed && (<>
        <button onClick={onNew} className="mx-3 mt-3 py-2 px-3 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-600 text-xs border border-blue-200 transition-all">+ 新建会话</button>
        <div ref={listRef} className="flex-1 overflow-y-auto px-2 py-3 space-y-1">
          {sessions.map((s) => (
            <div key={s.sessionId}
              className={`session-item group px-3 py-2 rounded-lg cursor-pointer transition-colors text-xs flex items-center ${s.sessionId === activeSessionId ? "bg-blue-100 text-blue-700" : "text-slate-500 hover:bg-slate-50"}`}
              onClick={() => onSwitch(s.sessionId)}>
              <span className="truncate flex-1">{s.title || "新会话"}</span>
              <button
                onClick={(e) => handleDelete(e, s.sessionId)}
                className="ml-1 text-slate-300 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity text-xs leading-none shrink-0"
              >×</button>
            </div>
          ))}
        </div>
      </>)}
    </aside>
  );
}
