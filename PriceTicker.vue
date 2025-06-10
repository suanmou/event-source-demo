<template>
  <div class="price-ticker">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>实时价格</span>
        <el-button style="float: right; padding: 3px 0" type="text">刷新</el-button>
      </div>
      <div class="price-container">
        <div class="price-item">
          <span class="price-label">买价:</span>
          <span :class="{'price-up': buyPriceChange > 0, 'price-down': buyPriceChange < 0}" class="price-value">
            {{ buyPrice.toFixed(2) }}
          </span>
          <span v-if="buyPriceChange !== 0" :class="{'price-up': buyPriceChange > 0, 'price-down': buyPriceChange < 0}" class="price-change">
            {{ buyPriceChange > 0 ? '+' : '' }}{{ buyPriceChange.toFixed(2) }}
          </span>
        </div>
        <div class="price-item">
          <span class="price-label">卖价:</span>
          <span :class="{'price-up': sellPriceChange > 0, 'price-down': sellPriceChange < 0}" class="price-value">
            {{ sellPrice.toFixed(2) }}
          </span>
          <span v-if="sellPriceChange !== 0" :class="{'price-up': sellPriceChange > 0, 'price-down': sellPriceChange < 0}" class="price-change">
            {{ sellPriceChange > 0 ? '+' : '' }}{{ sellPriceChange.toFixed(2) }}
          </span>
        </div>
      </div>
      <div class="last-update">
        最后更新: {{ lastUpdate }}
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'PriceTicker',
  data() {
    return {
      buyPrice: 100.00,
      prevBuyPrice: 100.00,
      sellPrice: 101.00,
      prevSellPrice: 101.00,
      buyPriceChange: 0,
      sellPriceChange: 0,
      lastUpdate: new Date().toLocaleTimeString(),
      timer: null
    }
  },
  created() {
    this.startPriceUpdates();
  },
  beforeDestroy() {
    this.stopPriceUpdates();
  },
  methods: {
    startPriceUpdates() {
      // 每2秒更新一次价格
      this.timer = setInterval(() => {
        this.updatePrices();
      }, 2000);
    },
    stopPriceUpdates() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
    },
    updatePrices() {
      // 记录之前的价格
      this.prevBuyPrice = this.buyPrice;
      this.prevSellPrice = this.sellPrice;
      
      // 生成随机价格变动 (-1 到 1 之间)
      const buyChange = (Math.random() * 2 - 1) * 2;
      const sellChange = (Math.random() * 2 - 1) * 2;
      
      // 更新价格
      this.buyPrice = this.prevBuyPrice + buyChange;
      this.sellPrice = this.prevSellPrice + sellChange;
      
      // 计算价格变动
      this.buyPriceChange = buyChange;
      this.sellPriceChange = sellChange;
      
      // 更新最后更新时间
      this.lastUpdate = new Date().toLocaleTimeString();
    }
  }
}
</script>

<style scoped>
.price-ticker {
  max-width: 400px;
  margin: 0 auto;
  padding: 20px;
}

.price-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.price-item {
  display: flex;
  align-items: center;
  font-size: 18px;
}

.price-label {
  width: 80px;
  font-weight: bold;
}

.price-value {
  margin-right: 10px;
  font-weight: bold;
}

.price-up {
  color: #67C23A; /* ElementUI 绿色 */
}

.price-down {
  color: #F56C6C; /* ElementUI 红色 */
}

.last-update {
  margin-top: 15px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
</style>    