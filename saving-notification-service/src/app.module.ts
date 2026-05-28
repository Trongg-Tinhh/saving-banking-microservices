import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { NotificationModule } from './notification/notification.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),

    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (config: ConfigService) => ({
        type:     'postgres',
        host:     config.get<string>('POSTGRES_HOST', 'localhost'),
        port:     config.get<number>('POSTGRES_PORT', 5432),
        username: config.get<string>('POSTGRES_USER', 'postgres'),
        password: config.get<string>('POSTGRES_PASSWORD', 'postgres'),
        database: config.get<string>('POSTGRES_DB', 'saving_banking'),
        // autoLoadEntities picks up entities registered via TypeOrmModule.forFeature()
        autoLoadEntities: true,
        // synchronize creates/updates tables automatically in non-production envs
        synchronize: config.get<string>('NODE_ENV') !== 'production',
        logging:     config.get<string>('NODE_ENV') === 'development',
      }),
      inject: [ConfigService],
    }),

    NotificationModule,
  ],
})
export class AppModule {}
