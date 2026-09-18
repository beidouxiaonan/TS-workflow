import type { Metadata } from 'next';import './globals.css';
export const metadata:Metadata={title:'知行台｜知识库工单系统',description:'打通工单处置与知识沉淀链路，让每一次问题解决都成为可复用的组织知识。'};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="zh-CN"><body>{children}</body></html>}
