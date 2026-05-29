import { Injectable, Logger, BadGatewayException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request, Response } from 'express';
import { createProxyMiddleware, Options } from 'http-proxy-middleware';
import { CORRELATION_HEADER } from '../common/correlation-id.middleware';

type ServiceKey =
  | 'auth'
  | 'customer'
  | 'account'
  | 'product'
  | 'contract'
  | 'transaction'
  | 'interest'
  | 'lifecycle'
  | 'notification';

@Injectable()
export class ProxyService {
  private readonly logger = new Logger('GatewayProxy');
  private readonly targets: Record<ServiceKey, string>;
  private readonly proxies: Partial<Record<ServiceKey, ReturnType<typeof createProxyMiddleware>>> = {};

  constructor(private readonly config: ConfigService) {
    const services = this.config.get<Record<ServiceKey, string>>('services') ?? {};
    this.targets = services as Record<ServiceKey, string>;

    for (const [key, target] of Object.entries(this.targets)) {
      this.proxies[key as ServiceKey] = createProxyMiddleware({
        target,
        changeOrigin: true,
        on: {
          // No proxyReq body fix needed: main.ts sets bodyParser:false so
          // NestJS never consumes the req stream, and http-proxy-middleware
          // pipes it to the downstream service as-is.

          error: (err, _req, res) => {
            const req = _req as Request;
            const corrId = (req.headers?.[CORRELATION_HEADER.toLowerCase()] as string) ?? '-';
            this.logger.error(
              `[${corrId}] ✖ ${key.toUpperCase()} unavailable: ${(err as Error).message}`,
            );
            if (!(res as Response).headersSent) {
              (res as Response).status(502).json({
                success: false,
                message: `Downstream service "${key}" is unavailable`,
                errorCode: 'GW_502',
              });
            }
          },
        },
      } as Options);
    }
  }

  /**
   * Forward the request to the named downstream service.
   * Logs ► on entry and ◄ on response finish (status + duration).
   */
  forward(service: ServiceKey, req: Request, res: Response): void {
    const proxy = this.proxies[service];
    if (!proxy) {
      throw new BadGatewayException(`Unknown service: ${service}`);
    }

    // ── Step-by-step logging ────────────────────────────────────────────────
    const corrId = (req.headers[CORRELATION_HEADER.toLowerCase()] as string) ?? '-';
    const user   = (req as any).user?.sub ?? (req as any).user?.username ?? 'anonymous';
    const start  = Date.now();
    const target = this.targets[service];

    this.logger.log(
      `[${corrId}] ► ${req.method} ${req.path}  →  ${service.toUpperCase()} (${target})  |  user: ${user}`,
    );

    // Hook into the response finish event to log the result
    res.on('finish', () => {
      const ms     = Date.now() - start;
      const status = res.statusCode;
      const label  = `[${corrId}] ◄ ${status}  ←  ${service.toUpperCase()}  |  ${ms}ms`;

      if (status >= 500) {
        this.logger.error(label);
      } else if (status >= 400) {
        this.logger.warn(label);
      } else {
        this.logger.log(label);
      }
    });

    // ── Proxy the request ───────────────────────────────────────────────────
    (proxy as (req: Request, res: Response, next: (err?: Error) => void) => void)(
      req,
      res,
      (err?: Error) => {
        if (err) {
          this.logger.error(`[${corrId}] ${service.toUpperCase()} next() error: ${err.message}`);
          if (!res.headersSent) {
            res.status(502).json({
              success:   false,
              message:   `Service "${service}" error`,
              errorCode: 'GW_502',
            });
          }
        }
      },
    );
  }
}
