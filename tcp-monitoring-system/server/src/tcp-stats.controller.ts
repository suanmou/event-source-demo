import { Controller, Get } from '@nestjs/common';
import { TcpStatsService, ConnectionStats } from './tcp-stats.service';

@Controller('stats')
export class TcpStatsController {
  constructor(private readonly statsService: TcpStatsService) {}

  @Get()
  getStats(): ConnectionStats {
    return this.statsService.getStats();
  }
}  