import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import * as childProcess from 'child_process';
import { promisify } from 'util';

const exec = promisify(childProcess.exec);

export interface ConnectionStats {
  timestamp: number;
  totalConnections: number;
  connectionStates: { [state: string]: number };
  rttStats: {
    min: number;
    avg: number;
    max: number;
    mdev: number;
  };
  topApplications: { name: string; count: number }[];
}

@Injectable()
export class TcpStatsService {
  private readonly logger = new Logger(TcpStatsService.name);
  private stats: ConnectionStats;

  constructor() {
    this.initializeStats();
  }

  private initializeStats() {
    this.stats = {
      timestamp: Date.now(),
      totalConnections: 0,
      connectionStates: {},
      rttStats: { min: 0, avg: 0, max: 0, mdev: 0 },
      topApplications: [],
    };
  }

  @Cron(CronExpression.EVERY_5_SECONDS)
  async collectStats() {
    try {
      const [netstatOutput, ssOutput, rttOutput] = await Promise.all([
        exec('netstat -tulpn'),
        exec('ss -s'),
        exec('ping -c 5 8.8.8.8 | tail -1'),
      ]);

      this.parseConnectionStates(netstatOutput.stdout);
      this.parseTotalConnections(ssOutput.stdout);
      this.parseRttStats(rttOutput.stdout);
      this.updateTopApplications(netstatOutput.stdout);
      
      this.stats.timestamp = Date.now();
      this.logger.debug('Stats updated');
    } catch (error) {
      this.logger.error('Failed to collect stats:', error.message);
    }
  }

  private parseConnectionStates(output: string) {
    const states = {};
    const lines = output.split('\n').slice(2); // Skip header lines
    
    lines.forEach(line => {
      const parts = line.trim().split(/\s+/);
      if (parts.length >= 6 && parts[5] !== '-') {
        const state = parts[5];
        states[state] = (states[state] || 0) + 1;
      }
    });
    
    this.stats.connectionStates = states;
  }

  private parseTotalConnections(output: string) {
    const match = output.match(/TCP:.*?(\d+)\s+estab/);
    if (match && match[1]) {
      this.stats.totalConnections = parseInt(match[1], 10);
    }
  }

  private parseRttStats(output: string) {
    const match = output.match(/rtt min\/avg\/max\/mdev = (\d+\.\d+)\/(\d+\.\d+)\/(\d+\.\d+)\/(\d+\.\d+)/);
    if (match) {
      this.stats.rttStats = {
        min: parseFloat(match[1]),
        avg: parseFloat(match[2]),
        max: parseFloat(match[3]),
        mdev: parseFloat(match[4]),
      };
    }
  }

  private updateTopApplications(output: string) {
    const appMap = new Map();
    const lines = output.split('\n').slice(2);
    
    lines.forEach(line => {
      const parts = line.trim().split(/\s+/);
      if (parts.length >= 7 && parts[6] !== '-') {
        const appInfo = parts[6].split('/');
        const appName = appInfo.length > 1 ? appInfo[1] : appInfo[0];
        appMap.set(appName, (appMap.get(appName) || 0) + 1);
      }
    });
    
    this.stats.topApplications = Array.from(appMap.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([name, count]) => ({ name, count }));
  }

  getStats(): ConnectionStats {
    return this.stats;
  }
}  