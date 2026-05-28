"use client";

import { useState, useRef, useEffect } from "react";
import { API_BASE, authHeaders } from "@/lib/api";

const SUPPORTED = ".pdf,.docx,.xlsx,.pptx,.md,.txt";
const MAX_FILES = 7;

export default function FileUploader({ sessionId, onUploaded }) {
  const [dragOver, setDragOver] = useState(false);
  const [files, setFiles] = useState([]);
  const [collapsed, setCollapsed] = useState(false);
  const animRef = useRef(null);
  const lottieRef = useRef(null);

  useEffect(() => {
    if (!animRef.current) return;
    let cancelled = false;
    const c = animRef.current;
    fetch("/animations/quantum-ball.json").then(r => r.json()).then(async (d) => {
      if (cancelled || !c) return;
      const l = (await import("lottie-web")).default;
      if (cancelled || !c) return;
      lottieRef.current = l.loadAnimation({ container: c, renderer: "svg", loop: true, autoplay: true, animationData: d });
    }).catch(() => {});
    return () => { cancelled = true; lottieRef.current?.destroy(); lottieRef.current = null; };
  }, []);

  const startUpload = (fileList) => {
    if (!sessionId) return;
    const remaining = MAX_FILES - files.length;
    const toUpload = Array.from(fileList).slice(0, remaining);
    if (toUpload.length === 0) return;

    const newFiles = toUpload.map(f => ({ id: Date.now() + Math.random(), name: f.name, size: f.size, status: "uploading" }));
    setFiles(prev => [...prev, ...newFiles]);

    toUpload.forEach((file, idx) => {
      const fileId = newFiles[idx].id;
      const fd = new FormData(); fd.append("file", file);
      fetch(`${API_BASE}/api/file/upload-and-index?sessionId=${sessionId}`, { method: "POST", headers: authHeaders(), body: fd })
        .then(r => r.json())
        .then(json => {
          setFiles(prev => prev.map(f => f.id === fileId ? { ...f, status: json.code === 200 ? "done" : "error", type: json.data?.fileType } : f));
          if (json.code === 200) onUploaded?.(json.data);
        })
        .catch(() => setFiles(prev => prev.map(f => f.id === fileId ? { ...f, status: "error" } : f)));
    });
  };

  const removeFile = (id) => setFiles(prev => prev.filter(f => f.id !== id));
  const onDrop = (e) => { e.preventDefault(); setDragOver(false); startUpload(e.dataTransfer.files); };
  const onFileSelect = (e) => { startUpload(e.target.files); e.target.value = ""; };
  const uploadingCount = files.filter(f => f.status === "uploading").length;

  if (collapsed) return <div className="glass-panel rounded-xl px-4 py-2 text-xs text-slate-400 cursor-pointer hover:text-blue-600 transition-colors" onClick={() => setCollapsed(false)}>▼ 上传资料 {files.length > 0 && `(${files.length}/${MAX_FILES})`} {uploadingCount > 0 && "⏳"}</div>;

  return (
    <div className={`glass-panel rounded-xl p-4 transition-all duration-500 ${dragOver ? "border-blue-400 shadow-lg" : ""}`}
      onDragOver={(e) => { e.preventDefault(); setDragOver(true); }} onDragLeave={() => setDragOver(false)} onDrop={onDrop}>
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-semibold tracking-widest text-blue-600 uppercase">上传资料 ({files.length}/{MAX_FILES})</span>
        <button onClick={() => setCollapsed(true)} className="text-slate-400 hover:text-slate-600 text-sm">▲ 收起</button>
      </div>
      {files.length > 0 && (<div className="mb-2 space-y-0.5 max-h-32 overflow-y-auto">
        {files.map((f) => (<div key={f.id} className="flex items-center justify-between px-2 py-1 bg-slate-50 rounded text-xs">
          <span className={`truncate flex-1 ${f.status === "done" ? "text-slate-700" : f.status === "error" ? "text-red-400 line-through" : "text-blue-500"}`}>{f.name}</span>
          {f.status === "uploading" ? <span className="text-blue-400 shrink-0 mx-1 animate-pulse">⏳</span>
           : f.status === "error" ? <span className="text-red-400 shrink-0 mx-1">✕</span>
           : <span className="text-emerald-500 shrink-0 mx-1">✓</span>}
          <button onClick={() => removeFile(f.id)} className="text-slate-300 hover:text-red-400 ml-1 shrink-0">✕</button>
        </div>))}
      </div>)}
      <div className="relative flex flex-col items-center justify-center py-4 border-2 border-dashed rounded-xl transition-all duration-300"
        style={{ borderColor: dragOver ? "rgba(59,130,246,0.5)" : "rgba(59,130,246,0.15)" }}>
        <div ref={animRef} className="w-16 h-16 lottie-container" />
        {files.length < MAX_FILES && (<>
          <p className="text-xs text-slate-400 mt-1">拖拽/点击上传文档 · PDF Word Excel PPT Markdown TXT · 最多 {MAX_FILES} 个</p>
          <input type="file" accept={SUPPORTED} onChange={onFileSelect} className="absolute inset-0 opacity-0 cursor-pointer" />
        </>)}
        {files.length >= MAX_FILES && <p className="text-xs text-slate-400">已满 ({MAX_FILES})</p>}
      </div>
    </div>
  );
}
