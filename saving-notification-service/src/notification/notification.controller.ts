import {
  Controller,
  Get,
  Patch,
  Param,
  Query,
  ParseIntPipe,
  DefaultValuePipe,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiOperation, ApiQuery, ApiTags } from '@nestjs/swagger';
import { NotificationService } from './notification.service';

@ApiTags('Notifications')
@Controller('api/v1/notifications')
export class NotificationController {

  constructor(private readonly notificationService: NotificationService) {}

  // ── Health ──────────────────────────────────────────────────────────────────

  @Get('health')
  @ApiOperation({ summary: 'Health check' })
  health(): object {
    return {
      success:   true,
      message:   'Notification Service is running',
      data:      'UP',
      timestamp: new Date().toISOString(),
    };
  }

  // ── List notifications ──────────────────────────────────────────────────────

  @Get()
  @ApiOperation({ summary: 'List notifications (paginated)' })
  @ApiQuery({ name: 'page', required: false, type: Number })
  @ApiQuery({ name: 'size', required: false, type: Number })
  async list(
    @Query('page', new DefaultValuePipe(0), ParseIntPipe) page: number,
    @Query('size', new DefaultValuePipe(20), ParseIntPipe) size: number,
  ): Promise<object> {
    const data = await this.notificationService.list(page, size);
    return {
      success:       true,
      message:       'Notifications retrieved',
      data,
      timestamp:     new Date().toISOString(),
    };
  }

  // ── Unread count ────────────────────────────────────────────────────────────

  @Get('unread-count')
  @ApiOperation({ summary: 'Get unread notification count' })
  @ApiQuery({ name: 'cif', required: false })
  async unreadCount(@Query('cif') cif?: string): Promise<object> {
    const count = await this.notificationService.unreadCount(cif);
    return { success: true, data: { count }, timestamp: new Date().toISOString() };
  }

  // ── Mark one read ───────────────────────────────────────────────────────────

  @Patch(':id/read')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Mark a notification as read' })
  async markRead(@Param('id') id: string): Promise<object> {
    await this.notificationService.markRead(id);
    return { success: true, message: 'Notification marked as read', timestamp: new Date().toISOString() };
  }

  // ── Mark all read ───────────────────────────────────────────────────────────

  @Patch('read-all')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Mark all notifications as read' })
  @ApiQuery({ name: 'cif', required: false })
  async markAllRead(@Query('cif') cif?: string): Promise<object> {
    await this.notificationService.markAllRead(cif);
    return { success: true, message: 'All notifications marked as read', timestamp: new Date().toISOString() };
  }
}
