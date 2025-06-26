<template>
  <div class="metric-card" :class="size">
    <div class="metric-header">
      <div class="metric-title">{{ title }}</div>
      <div class="metric-icon" :class="iconClass"></div>
    </div>
    <div class="metric-value">
      <span class="value-text">{{ value }}</span>
      <span class="value-unit">{{ unit }}</span>
    </div>
    <div class="metric-change" :class="getChangeClass()">
      <span v-if="change > 0" class="change-up">
        <i class="el-icon-arrow-up"></i> {{ Math.abs(change) }}%
      </span>
      <span v-else-if="change < 0" class="change-down">
        <i class="el-icon-arrow-down"></i> {{ Math.abs(change) }}%
      </span>
      <span v-else class="change-equal">
        <i class="el-icon-minus"></i> 0%
      </span>
      <span class="change-text">较昨日</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MetricCard',
  props: {
    title: {
      type: String,
      default: '指标名称'
    },
    value: {
      type: [String, Number],
      default: '0'
    },
    unit: {
      type: String,
      default: ''
    },
    change: {
      type: Number,
      default: 0
    },
    icon: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: 'normal',
      validator: value => ['small', 'normal', 'large'].includes(value)
    }
  },
  computed: {
    iconClass() {
      return this.icon ? `el-icon-${this.icon}` : '';
    },
    getChangeClass() {
      if (this.change > 0) {
        return 'change-positive';
      } else if (this.change < 0) {
        return 'change-negative';
      } else {
        return 'change-neutral';
      }
    }
  }
};
</script>

<style scoped>
.metric-card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.metric-title {
  font-size: 14px;
  color: #606266;
}

.metric-icon {
  font-size: 20px;
  color: #909399;
}

.metric-value {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.value-text {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.value-unit {
  margin-left: 4px;
  font-size: 12px;
  color: #909399;
}

.metric-change {
  display: flex;
  align-items: center;
  font-size: 12px;
}

.change-up {
  color: #f56c6c;
  margin-right: 4px;
}

.change-down {
  color: #67c23a;
  margin-right: 4px;
}

.change-equal {
  color: #909399;
  margin-right: 4px;
}

.change-text {
  color: #909399;
}

.metric-card.small {
  padding: 12px;
}

.metric-card.small .metric-title {
  font-size: 12px;
}

.metric-card.small .metric-icon {
  font-size: 16px;
}

.metric-card.small .value-text {
  font-size: 18px;
}

.metric-card.small .metric-change {
  font-size: 10px;
}

.metric-card.large {
  padding: 24px;
}

.metric-card.large .metric-title {
  font-size: 16px;
}

.metric-card.large .metric-icon {
  font-size: 24px;
}

.metric-card.large .value-text {
  font-size: 32px;
}

.metric-card.large .metric-change {
  font-size: 14px;
}
</style>
    