import { All, Controller, Logger, Req, Res, UseGuards } from '@nestjs/common';
import { ApiExcludeController } from '@nestjs/swagger';
import { Request, Response } from 'express';
import { Reflector } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import { ProxyService } from './proxy.service';
import { JwtAuthGuard } from '../common/jwt-auth.guard';
import { ThrottlerGuard } from '@nestjs/throttler';

/**
 * Catch-all proxy controller.
 *
 * Route → downstream service mapping:
 *   /api/v1/auth/**         → auth-service:8081
 *   /api/v1/customers/**    → customer-service:8082
 *   /api/v1/accounts/**     → account-service:8083
 *   /api/v1/products/**     → saving-product-service:8084
 *   /api/v1/contracts/**    → saving-contract-service:8085
 *   /api/v1/transactions/** → saving-transaction-service:8086
 *   /api/v1/interest/**     → saving-interest-service:8087
 *   /api/v1/lifecycle/**    → saving-lifecycle-service:8088
 *   /api/v1/notifications/**→ saving-notification-service:8089
 */
@ApiExcludeController()
@Controller()
@UseGuards(ThrottlerGuard, JwtAuthGuard)
export class ProxyController {
  private readonly logger = new Logger(ProxyController.name);

  constructor(private readonly proxyService: ProxyService) {}

  // ── Auth Service ──────────────────────────────────────────────────────────
  @All('/api/v1/auth*')
  proxyAuth(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('auth', req, res);
  }

  // ── Customer Service ──────────────────────────────────────────────────────
  @All('/api/v1/customers*')
  proxyCustomer(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('customer', req, res);
  }

  // ── Account Service ───────────────────────────────────────────────────────
  @All('/api/v1/accounts*')
  proxyAccount(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('account', req, res);
  }

  // ── Saving Product Service ────────────────────────────────────────────────
  @All('/api/v1/products*')
  proxyProduct(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('product', req, res);
  }

  // ── Saving Contract Service ───────────────────────────────────────────────
  @All('/api/v1/contracts*')
  proxyContract(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('contract', req, res);
  }

  // ── Transaction Service ───────────────────────────────────────────────────
  @All('/api/v1/transactions*')
  proxyTransaction(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('transaction', req, res);
  }

  // ── Interest Calculation Service ──────────────────────────────────────────
  @All('/api/v1/interest*')
  proxyInterest(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('interest', req, res);
  }

  // ── Lifecycle Service ─────────────────────────────────────────────────────
  @All('/api/v1/lifecycle*')
  proxyLifecycle(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('lifecycle', req, res);
  }

  // ── Notification Service ──────────────────────────────────────────────────
  @All('/api/v1/notifications*')
  proxyNotification(@Req() req: Request, @Res() res: Response): void {
    this.proxyService.forward('notification', req, res);
  }
}
