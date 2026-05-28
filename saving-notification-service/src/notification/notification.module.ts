import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { NotificationService } from './notification.service';
import { NotificationController } from './notification.controller';
import { ContractEventConsumer } from './contract-event.consumer';
import { NotificationEntity } from './notification.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([NotificationEntity]),

    ClientsModule.registerAsync([
      {
        name: 'RABBITMQ_CLIENT',
        imports: [ConfigModule],
        useFactory: (config: ConfigService) => ({
          transport: Transport.RMQ,
          options: {
            urls: [
              `amqp://${config.get('RABBITMQ_USER', 'guest')}:${config.get('RABBITMQ_PASS', 'guest')}@${config.get('RABBITMQ_HOST', 'localhost')}:${config.get('RABBITMQ_PORT', 5672)}`,
            ],
            queue: 'notif.contract.events',
            queueOptions: { durable: true },
            noAck: false,
          },
        }),
        inject: [ConfigService],
      },
    ]),
  ],
  controllers: [NotificationController],
  providers: [NotificationService, ContractEventConsumer],
})
export class NotificationModule {}
