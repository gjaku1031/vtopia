import type { NextConfig } from "next";

/**
 * 브라우저에 직접 노출하지 않는 API 원점
 */
const apiOrigin = process.env.API_ORIGIN ?? "http://localhost:8080";

/**
 * 같은 출처 API 전달과 독립 실행 산출물 설정
 */
const nextConfig: NextConfig = {
  output: "standalone",

  /**
   * localhost 외 개발 자원 요청 허용 호스트. VS Code 포트 포워딩의 127.0.0.1 접속용, 운영 빌드에는 영향 없음
   */
  allowedDevOrigins: ["127.0.0.1"],

  /**
   * 상태 점검은 Actuator로, 업무 API는 동일 경로로 전달
   */
  async rewrites() {
    return [
      { source: "/api/health", destination: `${apiOrigin}/actuator/health` },
      { source: "/api/:path*", destination: `${apiOrigin}/api/:path*` },
    ];
  },
};

export default nextConfig;
