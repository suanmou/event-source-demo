<template>
  <div class="bond-trading-container">
    <!-- 保持原有模板不变 -->
    
    <!-- 增强市场数据表格显示多档行情 -->
    <el-tab-pane label="市场数据" name="marketData">
      <!-- 表单部分保持不变 -->
      
      <el-table :data="marketData" stripe style="width: 100%">
        <el-table-column prop="symbol" label="债券代码"></el-table-column>
        <el-table-column label="买盘">
          <template #default="scope">
            <div v-for="(bid, index) in scope.row.bids" :key="index">
              <el-tag>{{ bid.price }}</el-tag>
              <el-tag>{{ bid.size }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="卖盘">
          <template #default="scope">
            <div v-for="(offer, index) in scope.row.offers" :key="index">
              <el-tag>{{ offer.price }}</el-tag>
              <el-tag>{{ offer.size }}</el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-tab-pane>
  </div>
</template>

<script>
import FixMarketDataParser from './fix-market-data-parser.js';

export default {
  name: 'BondTradingComponent',
  data() {
    return {
      // 保持原有数据结构不变
      marketDataParser: new FixMarketDataParser()
    }
  },
  methods: {
    // 保持原有方法不变
    
    // 增强handleMessage方法，处理市场数据消息
    handleMessage(fixMessage) {
      try {
        // 先尝试作为市场数据解析
        const marketData = this.marketDataParser.autoParse(fixMessage);
        
        if (marketData) {
          this.handleMarketData(marketData);
          return;
        }
        
        // 再尝试其他类型的消息解析 (保持原有逻辑不变)
        // ...
      } catch (error) {
        console.error('解析FIX消息失败:', error);
        this.$message.error('解析消息失败: ' + error.message);
      }
    },
    
    handleMarketData(marketData) {
      if (marketData.type === 'snapshot') {
        this.handleMarketDataSnapshot(marketData);
      } else if (marketData.type === 'incremental') {
        this.handleMarketDataIncremental(marketData);
      }
    },
    
    handleMarketDataSnapshot(snapshot) {
      // 提取买卖盘数据
      const bids = snapshot.entries
        .filter(entry => entry.rawType === '0')
        .sort((a, b) => b.price - a.price); // 买盘按价格从高到低排序
      
      const offers = snapshot.entries
        .filter(entry => entry.rawType === '1')
        .sort((a, b) => a.price - b.price); // 卖盘按价格从低到高排序
      
      // 更新市场数据
      this.marketData.push({
        symbol: snapshot.symbol,
        bids,
        offers
      });
      
      this.$message.info(`收到 ${snapshot.symbol} 的市场数据快照`);
    },
    
    handleMarketDataIncremental(incremental) {
      // 找到对应的市场数据条目
      const index = this.marketData.findIndex(item => item.symbol === incremental.symbol);
      
      if (index === -1) {
        // 如果没有找到，当作新的快照处理
        this.handleMarketDataSnapshot({
          ...incremental,
          type: 'snapshot'
        });
        return;
      }
      
      const marketData = this.marketData[index];
      
      // 处理增量更新
      incremental.entries.forEach(entry => {
        if (entry.rawType === '0') { // 买盘
          this.applyIncrementalUpdate(marketData.bids, entry);
        } else if (entry.rawType === '1') { // 卖盘
          this.applyIncrementalUpdate(marketData.offers, entry);
        }
      });
      
      // 重新排序
      marketData.bids.sort((a, b) => b.price - a.price);
      marketData.offers.sort((a, b) => a.price - b.price);
      
      this.$message.info(`收到 ${incremental.symbol} 的市场数据更新`);
    },
    
    applyIncrementalUpdate(entries, update) {
      if (update.action === '删除') {
        // 找到并删除对应的条目
        const index = entries.findIndex(item => item.price === update.price);
        if (index !== -1) entries.splice(index, 1);
      } else if (update.action === '修改') {
        // 找到并更新对应的条目
        const index = entries.findIndex(item => item.price === update.price);
        if (index !== -1) entries[index] = update;
      } else if (update.action === '新增') {
        // 添加新条目
        entries.push(update);
      }
    }
  }
}
</script>

<style scoped>
/* 保持原有样式不变 */
</style>    