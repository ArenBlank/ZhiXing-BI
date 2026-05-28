import "./globals.css";

export const metadata = {
  title: "ZhiXing-BI | 知行智能分析平台",
  description: "下一代商业智能分析智能体 — 企业级 AI Agent 决策引擎",
};

export default function RootLayout({ children }) {
  return (
    <html lang="zh-CN" className="dark">
      <body className="bg-white text-slate-800 antialiased h-full"
        style={{ fontFamily: 'Inter, system-ui, -apple-system, sans-serif' }}>
        {children}
      </body>
    </html>
  );
}
