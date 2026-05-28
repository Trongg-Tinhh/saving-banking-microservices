import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { ValidationPipe } from '@nestjs/common';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));

  const config = new DocumentBuilder()
    .setTitle('Notification Service API')
    .setDescription('RabbitMQ-driven notification service. Consumes contract events and dispatches alerts.')
    .setVersion('1.0.0')
    .addServer(`http://localhost:${process.env.PORT || 8089}`, 'Local')
    .addServer('http://saving-notification-service:8089', 'Docker')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('swagger-ui.html', app, document);

  const port = process.env.PORT || 8089;
  await app.listen(port);
  console.log(`Notification Service running on port ${port}`);
}

bootstrap();
