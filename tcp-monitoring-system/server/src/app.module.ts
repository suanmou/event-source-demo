import { Module } from '@nestjs/common';
import { TcpStatsService } from './tcp-stats.service';
import { TcpStatsController } from './tcp-stats.controller';
import { ScheduleModule } from '@nestjs/schedule';

@Module({
  imports: [ScheduleModule.forRoot()],
  providers: [TcpStatsService],
  controllers: [TcpStatsController],
})
export class AppModule {}  