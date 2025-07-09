<template>
  <!-- 保持原有模板不变 -->
</template>

<script>
// 导入FIX解析器
import FixMessageParser from './fix-parser.js';

export default {
  name: 'BondTradingComponent',
  data() {
    return {
      // 保持原有数据结构不变
      fixParser: new FixMessageParser()
    }
  },
  methods: {
    // 保持原有方法不变
    
    // 修改handleMessage方法，直接解析FIX消息
    handleMessage(fixMessage) {
      try {
        const parsedMessage = this.fixParser.autoParse(fixMessage);
        
        switch (parsedMessage.type) {
          case 'quote':
            this.handleParsedQuote(parsedMessage);
            break;
          case 'marketData':
            this.handleParsedMarketData(parsedMessage);
            break;
          case 'execution':
            this.handleParsedExecution(parsedMessage);
            break;
          default:
            console.log('未知消息类型:', parsedMessage);
        }
      } catch (error) {
        console.error('解析FIX消息失败:', error);
        this.$message.error('解析消息失败: ' + error.message);
      }
    },
    
    handleParsedQuote(quote) {
      this.quotes.push({
        symbol: quote.symbol,
        bid: quote.bidPrice,
        offer: quote.offerPrice
      });
      
      this.$message.info(`收到 ${quote.symbol} 的询价响应`);
    },
    
    handleParsedMarketData(marketData) {
      this.marketData.push({
        symbol: marketData.symbol,
        bidPrice: marketData.bids[0]?.price || 0,
        bidSize: marketData.bids[0]?.size || 0,
        offerPrice: marketData.offers[0]?.price || 0,
        offerSize: marketData.offers[0]?.size || 0
      });
      
      this.$message.info(`收到 ${marketData.symbol} 的市场数据`);
    },
    
    handleParsedExecution(execution) {
      this.orders.push({
        symbol: execution.symbol,
        side: execution.side,
        price: execution.price,
        quantity: execution.quantity,
        status: execution.status
      });
      
      this.$message.success(`订单 ${execution.symbol} ${execution.side === 'buy' ? '买入' : '卖出'} 已执行`);
    }
  }
}
</script>

<style scoped>
/* 保持原有样式不变 */
</style>    