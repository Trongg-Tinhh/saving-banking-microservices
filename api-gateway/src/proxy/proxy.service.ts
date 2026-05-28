import { Injectable, Logger, BadGatewayException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request, Response } from 'express';
import { createProxyMiddleware, Options } from 'http-proxy-middleware';

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
  private readonly logger = new Logger(ProxyService.name);
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
            this.logger.error(`[${key}] Proxy error: ${(err as Error).message}`);
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
   * Path is forwarded as-is — each downstream service is mounted at /api/v1/...
   */
  forward(service: ServiceKey, req: Request, res: Response): void {
    const proxy = this.proxies[service];
    if (!proxy) {
      throw new BadGatewayException(`Unknown service: ${service}`);
    }

    (proxy as (req: Request, res: Response, next: (err?: Error) => void) => void)(
      req,
      res,
      (err?: Error) => {
        if (err) {
          this.logger.error(`[${service}] Proxy next() error: ${err.message}`);
          if (!res.headersSent) {
            res.status(502).json({
              success: false,
              message: `Service "${service}" error`,
              errorCode: 'GW_502',
            });
          }
        }
      },
    );
  }
}
