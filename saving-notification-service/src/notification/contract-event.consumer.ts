import { Controller, Logger } from '@nestjs/common';
import { EventPattern, Payload, Ctx, RmqContext } from '@nestjs/microservices';
import { NotificationService } from './notification.service';

/**
 * Consumes contract lifecycle events from RabbitMQ and triggers notifications.
 *
 * Routing keys (from saving.contract.events exchange):
 *   saving.contract.opened.event  → CONTRACT_OPENED
 *   saving.contract.closed.event  → CONTRACT_CLOSED
 *   saving.contract.matured.event → CONTRACT_MATURED
 */
@Controller()
export class ContractEventConsumer {
  private readonly logger = new Logger(ContractEventConsumer.name);

  constructor(private readonly notificationService: NotificationService) {}

  @EventPattern('saving.contract.opened.event')
  async handleContractOpened(
    @Payload() data: Record<string, unknown>,
    @Ctx() context: RmqContext,
  ): Promise<void> {
    const channel = context.getChannelRef();
    const message = context.getMessage();
    const correlationId = (data['correlationId'] as string) ?? 'N/A';

    this.logger.log(`[${correlationId}] CONTRACT_OPENED received for ${data['contractNo']}`);

    try {
      await this.notificationService.dispatch({
        type:          'CONTRACT_OPENED',
        contractNo:    data['contractNo'] as string,
        cif:           data['cif'] as string,
        correlationId,
        details:       data,
      });
      channel.ack(message);
    } catch (err) {
      this.logger.error(`[${correlationId}] Failed to process CONTRACT_OPENED: ${err}`);
      channel.nack(message, false, false); // send to DLQ
    }
  }

  @EventPattern('saving.contract.closed.event')
  async handleContractClosed(
    @Payload() data: Record<string, unknown>,
    @Ctx() context: RmqContext,
  ): Promise<void> {
    const channel = context.getChannelRef();
    const message = context.getMessage();
    const correlationId = (data['correlationId'] as string) ?? 'N/A';

    this.logger.log(`[${correlationId}] CONTRACT_CLOSED received for ${data['contractNo']}`);

    try {
      await this.notificationService.dispatch({
        type:          'CONTRACT_CLOSED',
        contractNo:    data['contractNo'] as string,
        cif:           data['cif'] as string,
        correlationId,
        details:       data,
      });
      channel.ack(message);
    } catch (err) {
      this.logger.error(`[${correlationId}] Failed to process CONTRACT_CLOSED: ${err}`);
      channel.nack(message, false, false);
    }
  }

  @EventPattern('saving.contract.matured.event')
  async handleContractMatured(
    @Payload() data: Record<string, unknown>,
    @Ctx() context: RmqContext,
  ): Promise<void> {
    const channel = context.getChannelRef();
    const message = context.getMessage();
    const correlationId = (data['correlationId'] as string) ?? 'N/A';

    this.logger.log(`[${correlationId}] CONTRACT_MATURED received for ${data['contractNo']}`);

    try {
      await this.notificationService.dispatch({
        type:          'CONTRACT_MATURED',
        contractNo:    data['contractNo'] as string,
        cif:           data['cif'] as string,
        correlationId,
        details:       data,
      });
      channel.ack(message);
    } catch (err) {
      this.logger.error(`[${correlationId}] Failed to process CONTRACT_MATURED: ${err}`);
      channel.nack(message, false, false);
    }
  }
}
