import type { ProxyOptions } from 'vite';
import type { IncomingMessage } from 'node:http';
import type { Socket } from 'node:net';

type ProxyReq = {
  removeHeader: (name: string) => void;
};

type HttpProxy = {
  on: (
    event: 'proxyReq',
    listener: (proxyReq: ProxyReq, req: IncomingMessage, res: Socket) => void,
  ) => void;
};

/**
 * Vite `/api` proxy for local development.
 *
 * Phone browsers open the Mac LAN origin (e.g. http://192.168.5.24:3000) and call
 * same-origin `/api`. Vite forwards to Spring on localhost:8080, but still sends the
 * browser's Origin header. Spring CORS only allowlists localhost by default, so LAN
 * Origins get `403 Invalid CORS request` even though the browser treats `/api` as
 * same-origin.
 *
 * Strip Origin on the proxied request so Spring does not apply CORS to traffic that
 * the browser already considers same-origin via the Vite proxy. Production never uses
 * this proxy.
 */
export function createDevelopmentApiProxy(): ProxyOptions {
  return {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    configure: (proxy) => {
      const httpProxy = proxy as unknown as HttpProxy;
      httpProxy.on('proxyReq', (proxyReq) => {
        proxyReq.removeHeader('origin');
        proxyReq.removeHeader('Origin');
      });
    },
  };
}

/** Test helper: apply the same Origin stripping the Vite proxy uses. */
export function stripProxiedBrowserOrigin(proxyReq: ProxyReq): void {
  proxyReq.removeHeader('origin');
  proxyReq.removeHeader('Origin');
}
