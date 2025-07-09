<template>
  <div class="bond-trading-container">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="clearfix">
          <span>债券交易系统</span>
          <el-button style="float: right; padding: 3px 0" type="text" @click="connect">
            {{ connected ? '已连接' : '连接' }}
          </el-button>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane label="询价" name="quote">
          <el-form :model="quoteForm" label-width="80px">
            <el-form-item label="债券代码">
              <el-input v-model="quoteForm.symbol"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="requestQuote">提交询价</el-button>
            </el-form-item>
          </el-form>
          
          <el-table :data="quotes" stripe style="width: 100%">
            <el-table-column prop="symbol" label="债券代码"></el-table-column>
            <el-table-column prop="bid" label="买入价"></el-table-column>
            <el-table-column prop="offer" label="卖出价"></el-table-column>
            <el-table-column label="操作">
              <template #default="scope">
                <el-button size="mini" @click="createOrder(scope.row, 'buy')">买入</el-button>
                <el-button size="mini" @click="createOrder(scope.row, 'sell')">卖出</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="市场数据" name="marketData">
          <el-form :model="marketDataForm" label-width="80px">
            <el-form-item label="债券代码">
              <el-input v-model="marketDataForm.symbol"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="requestMarketData">获取行情</el-button>
            </el-form-item>
          </el-form>
          
          <el-table :data="marketData" stripe style="width: 100%">
            <el-table-column prop="symbol" label="债券代码"></el-table-column>
            <el-table-column label="买盘">
              <template #default="scope">
                <el-tag>{{ scope.row.bidPrice }}</el-tag>
                <el-tag>{{ scope.row.bidSize }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="卖盘">
              <template #default="scope">
                <el-tag>{{ scope.row.offerPrice }}</el-tag>
                <el-tag>{{ scope.row.offerSize }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="订单" name="orders">
          <el-form :model="orderForm" label-width="80px">
            <el-form-item label="债券代码">
              <el-input v-model="orderForm.symbol"></el-input>
            </el-form-item>
            <el-form-item label="交易方向">
              <el-radio-group v-model="orderForm.side">
                <el-radio label="buy">买入</el-radio>
                <el-radio label="sell">卖出</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="价格">
              <el-input v-model.number="orderForm.price"></el-input>
            </el-form-item>
            <el-form-item label="数量">
              <el-input v-model.number="orderForm.quantity"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="placeOrder">提交订单</el-button>
            </el-form-item>
          </el-form>
          
          <el-table :data="orders" stripe style="width: 100%">
            <el-table-column prop="symbol" label="债券代码"></el-table-column>
            <el-table-column prop="side" label="方向">
              <template #default="scope">
                {{ scope.row.side === 'buy' ? '买入' : '卖出' }}
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格"></el-table-column>
            <el-table-column prop="quantity" label="数量"></el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="scope">
                <el-tag
                  :type="scope.row.status === 'F' ? 'success' : 
                         scope.row.status === 'P' ? 'warning' : 'info'">
                  {{ statusMap[scope.row.status] || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'BondTradingComponent',
  data() {
    return {
      activeTab: 'quote',
      connected: false,
      socket: null,
      quoteForm: {
        symbol: '123456'
      },
      marketDataForm: {
        symbol: '123456'
      },
      orderForm: {
        symbol: '123456',
        side: 'buy',
        price: 100.0,
        quantity: 100
      },
      quotes: [],
      marketData: [],
      orders: [],
      statusMap: {
        '0': '新建',
        '1': '部分成交',
        '2': '全部成交',
        '3': '已取消',
        '4': '已替换',
        '5': '待取消',
        '6': '待替换',
        '7': '待提交',
        '8': '拒绝',
        'F': '全部成交'
      }
    }
  },
  created() {
    this.connect();
  },
  methods: {
    connect() {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.close();
      }
      
      this.socket = new WebSocket('ws://localhost:8080');
      
      this.socket.onopen = () => {
        this.connected = true;
        this.$message.success('WebSocket连接已建立');
      };
      
      this.socket.onmessage = (event) => {
        this.handleMessage(event.data);
      };
      
      this.socket.onclose = () => {
        this.connected = false;
        this.$message.warning('WebSocket连接已关闭');
      };
      
      this.socket.onerror = (error) => {
        this.$message.error('WebSocket连接错误: ' + error.message);
      };
    },
    
    handleMessage(message) {
      const parts = message.split('|');
      const type = parts[0];
      
      switch (type) {
        case 'LOGON_SUCCESS':
          this.$message.success('FIX会话已建立');
          break;
        case 'LOGOUT':
          this.$message.warning('FIX会话已关闭');
          break;
        case 'QUOTE':
          this.handleQuoteMessage(parts);
          break;
        case 'MARKET_DATA':
          this.handleMarketDataMessage(parts);
          break;
        case 'EXECUTION':
          this.handleExecutionMessage(parts);
          break;
        case 'ERROR':
          this.$message.error('错误: ' + parts[1]);
          break;
        default:
          console.log('未知消息类型: ', message);
      }
    },
    
    handleQuoteMessage(parts) {
      const symbol = parts[1];
      const bid = parseFloat(parts[2]);
      const offer = parseFloat(parts[3]);
      
      this.quotes.push({
        symbol,
        bid,
        offer
      });
      
      this.$message.info(`收到 ${symbol} 的询价响应`);
    },
    
    handleMarketDataMessage(parts) {
      const symbol = parts[1];
      let bidPrice = 0, bidSize = 0, offerPrice = 0, offerSize = 0;
      
      for (let i = 2; i < parts.length; i++) {
        const entry = parts[i].split(',');
        const type = entry[0];
        const price = parseFloat(entry[1]);
        const size = parseFloat(entry[2]);
        
        if (type === '0') { // BID
          bidPrice = price;
          bidSize = size;
        } else if (type === '1') { // OFFER
          offerPrice = price;
          offerSize = size;
        }
      }
      
      this.marketData.push({
        symbol,
        bidPrice,
        bidSize,
        offerPrice,
        offerSize
      });
      
      this.$message.info(`收到 ${symbol} 的市场数据`);
    },
    
    handleExecutionMessage(parts) {
      const symbol = parts[1];
      const side = parts[2] === '1' ? 'buy' : 'sell';
      const price = parseFloat(parts[3]);
      const quantity = parseFloat(parts[4]);
      const status = parts[5];
      
      this.orders.push({
        symbol,
        side,
        price,
        quantity,
        status
      });
      
      this.$message.success(`订单 ${symbol} ${side === 'buy' ? '买入' : '卖出'} 已执行`);
    },
    
    requestQuote() {
      if (!this.connected) {
        this.$message.error('请先连接WebSocket');
        return;
      }
      
      const message = `QUOTE|${this.quoteForm.symbol}`;
      this.socket.send(message);
    },
    
    requestMarketData() {
      if (!this.connected) {
        this.$message.error('请先连接WebSocket');
        return;
      }
      
      const message = `MARKET_DATA|${this.marketDataForm.symbol}`;
      this.socket.send(message);
    },
    
    placeOrder() {
      if (!this.connected) {
        this.$message.error('请先连接WebSocket');
        return;
      }
      
      const side = this.orderForm.side === 'buy' ? '1' : '2';
      const message = `ORDER|${this.orderForm.symbol}|${side}|${this.orderForm.price}|${this.orderForm.quantity}`;
      this.socket.send(message);
    },
    
    createOrder(quote, side) {
      this.orderForm.symbol = quote.symbol;
      this.orderForm.side = side;
      this.orderForm.price = side === 'buy' ? quote.bid : quote.offer;
      this.activeTab = 'orders';
    }
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.close();
    }
  }
}
</script>

<style scoped>
.bond-trading-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.box-card {
  margin-bottom: 16px;
}

.el-tabs__content {
  padding-top: 16px;
}
</style>    