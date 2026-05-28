import { Controller, Get } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';

@ApiTags('Gateway')
@Controller()
export class HealthController {

  @Get('health')
  @ApiOperation({ summary: 'Gateway health check' })
  health(): object {
    return {
      success: true,
      message: 'API Gateway is running',
      data: 'UP',
      timestamp: new Date().toISOString(),
    };
  }
}
