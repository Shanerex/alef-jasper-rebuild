import type { NextConfig } from "next";

/**
 * Next.js configuration.
 *
 * output: "standalone" produces a minimal server bundle suitable for the
 * Docker runtime stage, keeping the image small.
 */
const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    // Allow images from the API service in Docker network.
    remotePatterns: [
      {
        protocol: "http",
        hostname: "api",
        port: "8080",
      },
    ],
  },
};

export default nextConfig;
