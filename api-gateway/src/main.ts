import { NestFactory, Reflector } from '@nestjs/core';
import { AppModule } from './app.module';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    // Disable NestJS body-parser so the raw request stream stays unconsumed.
    // http-proxy-middleware pipes req → proxyReq directly without needing
    // fixRequestBody. Enabling bodyParser would consume the stream first and
    // cause socket hang-up because pipe can never call proxyReq.end().
    bodyParser: false,
  });

  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));

  // CORS — allow all origins in development; tighten in production
  app.enableCors({
    origin: process.env.CORS_ORIGINS?.split(',') ?? '*',
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Correlation-ID'],
  });

  const config = new DocumentBuilder()
    .setTitle('Saving Banking System — API Gateway')
    .setDescription(
      'Unified entry point for all 9 backend microservices.\n\n' +
      '**Route → Service mapping:**\n' +
      '- `/api/v1/auth/**` → Auth Service (8081)\n' +
      '- `/api/v1/customers/**` → Customer Service (8082)\n' +
      '- `/api/v1/accounts/**` → Account Service (8083)\n' +
      '- `/api/v1/products/**` → Saving Product Service (8084)\n' +
      '- `/api/v1/contracts/**` → Saving Contract Service (8085)\n' +
      '- `/api/v1/transactions/**` → Transaction Service (8086)\n' +
      '- `/api/v1/interest/**` → Interest Calculation Service (8087)\n' +
      '- `/api/v1/lifecycle/**` → Saving Lifecycle Service (8088)\n' +
      '- `/api/v1/notifications/**` → Notification Service (8089)\n\n' +
      'JWT is validated at the gateway level. Rate limiting: 100 req/min per IP.',
    )
    .setVersion('1.0.0')
    .addBearerAuth(
      { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
      'BearerAuth',
    )
    .addServer(`http://localhost:3000`, 'Local')
    .addServer(`http://api-gateway:3000`, 'Docker')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('swagger-ui.html', app, document, {
    swaggerOptions: { persistAuthorization: true },
  });

  const configService = app.get(ConfigService);
  const port = configService.get<number>('port') ?? 3000;

  await app.listen(port);
  console.log(`API Gateway running on port ${port}`);
  console.log(`Gateway Swagger (self): http://localhost:${port}/swagger-ui.html`);
  console.log(`All Services Swagger:   http://localhost:${port}/api-docs`);
}

bootstrap();
