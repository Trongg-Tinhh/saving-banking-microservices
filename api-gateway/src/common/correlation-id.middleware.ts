import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';

export const CORRELATION_HEADER = 'X-Correlation-ID';

@Injectable()
export class CorrelationIdMiddleware implements NestMiddleware {
  use(req: Request, res: Response, next: NextFunction): void {
    const id = (req.headers[CORRELATION_HEADER.toLowerCase()] as string) || uuidv4();
    req.headers[CORRELATION_HEADER.toLowerCase()] = id;
    res.setHeader(CORRELATION_HEADER, id);
    next();
  }
}
