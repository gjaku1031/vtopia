import { Agentation } from "agentation";
import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

/**
 * Agentation MCP 서버 주소. 서버 미기동 시 브라우저 로컬 저장으로 동작함
 */
const agentationEndpoint = "http://localhost:4747";

/**
 * 기본 페이지 제목과 검색 설명
 */
export const metadata: Metadata = {
  title: "Vtopia",
  description: "Vtopia의 새로운 시작",
};

/**
 * 루트 레이아웃에 전달되는 페이지 영역
 */
type RootLayoutProps = {
  /**
   * 현재 경로의 페이지
   */
  children: ReactNode;
};

/**
 * 한국어 문서와 공통 페이지 레이아웃
 */
export default function RootLayout({ children }: Readonly<RootLayoutProps>) {
  return (
    <html lang="ko">
      <body>
        {children}
        {/* 개발 서버에서만 UI 주석 도구를 표시하고 운영 빌드에서는 제외 */}
        {process.env.NODE_ENV === "development" && <Agentation endpoint={agentationEndpoint} />}
      </body>
    </html>
  );
}
