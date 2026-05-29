"use client";

import { useState, useRef, useEffect, forwardRef, useImperativeHandle } from "react";
import ReactMarkdown from "react-markdown";

const ChatConsole = forwardRef(function ChatConsole({ messages, onSend, onRetry, webSearchOn, setWebSearchOn }, ref) {
  const [input, setInput] = useState("");
  const [copiedId, setCopiedId] = useState(null);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);
  const prevCountRef = useRef(0);

  const copyText = async (text, msgId) => {
    try { await navigator.clipboard.writeText(text); setCopiedId(msgId); setTimeout(() => setCopiedId(null), 1500); } catch {}
  };

  useImperativeHandle(ref, () => ({ scrollToBottom: () => bottomRef.current?.scrollIntoView({ behavior: "smooth" }) }));
  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages]);

  useEffect(() => {
    if (messages.length === 0 || prevCountRef.current >= messages.length) return;
    (async () => {
      const { animate } = await import("animejs");
      const bubbles = document.querySelectorAll(".msg-bubble");
      if (bubbles.length === 0) return;
      animate(bubbles[bubbles.length - 1], { translateX: [30, 0], opacity: [0, 1], duration: 300, ease: "easeOutQuad" });
    })();
    prevCountRef.current = messages.length;
  }, [messages]);

  const handleSubmit = (e) => { e.preventDefault(); if (!input.trim()) return; onSend(input.trim()); setInput(""); inputRef.current?.focus(); };

  return (
    <div className="flex-1 flex flex-col min-h-0 glass-panel rounded-xl overflow-hidden">
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {messages.length === 0 && (
          <div className="flex items-center justify-center h-full"><p className="text-sm text-slate-400">上传文件或输入问题，开启智能分析</p></div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
            <div className={`msg-bubble max-w-[75%] px-4 py-3 rounded-2xl text-sm leading-relaxed ${msg.role === "user" ? "bg-blue-500 text-white rounded-br-md" : msg.thinking ? "bg-slate-50 text-slate-400 rounded-bl-md border border-slate-200" : "bg-slate-100 text-slate-700 rounded-bl-md border border-slate-200"}`}>
              {msg.role === "assistant" ? (
                <div className="prose prose-sm max-w-none prose-p:my-1 prose-strong:text-slate-800 prose-strong:font-semibold">
                  <ReactMarkdown>{msg.content}</ReactMarkdown>
                </div>
              ) : (
                <div className="whitespace-pre-wrap">{msg.content}</div>
              )}
              {msg.thinking && <span className="text-[10px] text-slate-300 mt-1 block">思考中...</span>}
              {msg.role === "assistant" && i === messages.length - 1 && !msg.thinking && <span className="inline-block w-2 h-4 bg-blue-500 rounded-sm ml-0.5 cursor-blink align-text-bottom" />}
            </div>
            {!msg.thinking && (
              <div className={`flex gap-1 mt-1 ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
                <button onClick={() => copyText(msg.content, msg.id || i)} className="text-[10px] text-slate-300 hover:text-slate-500 transition-colors">
                  {copiedId === (msg.id || i) ? "已复制" : "复制"}
                </button>
                {msg.role === "assistant" && onRetry && (
                  <button onClick={onRetry} className="text-[10px] text-slate-300 hover:text-blue-500 transition-colors ml-1">重新生成</button>
                )}
              </div>
            )}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      <form onSubmit={handleSubmit} className="p-3 border-t border-blue-100">
        <div className="flex items-center gap-2 mb-2">
          <button
            type="button"
            onClick={() => setWebSearchOn(!webSearchOn)}
            className={`text-xs px-3 py-1 rounded-full border transition-all select-none ${
              webSearchOn
                ? "bg-blue-50 border-blue-300 text-blue-600"
                : "bg-white border-slate-200 text-slate-400 hover:border-slate-300"
            }`}
          >
            {webSearchOn ? "✅" : "○"} 联网搜索
          </button>
        </div>
        <div className="flex gap-2">
          <input ref={inputRef} value={input} onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSubmit(e); } }}
            placeholder="输入你的问题..." className="flex-1 bg-slate-50 border border-blue-200 rounded-xl px-4 py-2.5 text-sm text-slate-700 placeholder-slate-400 focus:outline-none focus:border-blue-400 transition-colors" />
          <button type="submit" disabled={!input.trim()} className="px-5 py-2.5 rounded-xl bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium disabled:opacity-30 transition-all">发送</button>
        </div>
      </form>
    </div>
  );
});

export default ChatConsole;
