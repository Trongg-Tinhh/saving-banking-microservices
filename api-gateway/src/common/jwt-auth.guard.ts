import {
  CanActivate,
  ExecutionContext,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Reflector } from '@nestjs/core';
import * as jwt from 'jsonwebtoken';

export const IS_PUBLIC_KEY = 'isPublic';
export const Public = () => (target: any, key?: string, descriptor?: any) => {
  // Simple decorator to mark routes as public
  Reflect.defineMetadata(IS_PUBLIC_KEY, true, descriptor?.value ?? target);
};

@Injectable()
export class JwtAuthGuard implements CanActivate {
  private readonly logger = new Logger(JwtAuthGuard.name);
  private readonly jwtSecret: string;

  constructor(
    private readonly config: ConfigService,
    private readonly reflector: Reflector,
  ) {
    this.jwtSecret = this.config.get<string>('jwtSecret') ?? '';
  }

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();

    // Skip auth for health endpoints and public paths
    const path: string = request.path ?? '';
    if (
      path === '/health' ||
      path.startsWith('/swagger-ui') ||
      path.startsWith('/v3/api-docs') ||
      path.startsWith('/api-docs') ||          // aggregated Swagger UI + spec proxies
      path.startsWith('/api/v1/auth/login') ||
      path.startsWith('/api/v1/auth/refresh-token') ||
      path.startsWith('/api/v1/auth/validate') ||
      path.startsWith('/api/v1/auth/health')
    ) {
      return true;
    }

    const authHeader: string = request.headers['authorization'] ?? '';
    if (!authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing or malformed Authorization header');
    }

    const token = authHeader.substring(7);
    try {
      const payload = jwt.verify(token, this.jwtSecret, { algorithms: ['HS384'] }) as jwt.JwtPayload;
      request.user = payload;
      return true;
    } catch (err) {
      this.logger.warn(`JWT verification failed: ${err}`);
      throw new UnauthorizedException('Invalid or expired token');
    }
  }
}
