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
