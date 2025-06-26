<template>
  <div class="status-indicator" :class="`status-${status}`">
    <span class="status-dot" :style="{ backgroundColor: getColor() }"></span>
    <span class="status-text">{{ getText() }}</span>
  </div>
</template>

<script>
export default {
  name: 'StatusIndicator',
  props: {
    status: {
      type: String,
      default: 'unknown',
      validator: value => ['active', 'inactive', 'warning', 'danger', 'unknown'].includes(value)
    },
    size: {
      type: String,
      default: 'normal',
      validator: value => ['small', 'normal', 'large'].includes(value)
    }
  },
  methods: {
    getColor() {
      const colors = {
        active: '#67c23a',
        inactive: '#909399',
        warning: '#e6a23c',
        danger: '#f56c6c',
        unknown: '#909399'
      };
      return colors[this.status] || colors.unknown;
    },
    getText() {
      const texts = {
        active: '活跃',
        inactive: '非活跃',
        warning: '警告',
        danger: '危险',
        unknown: '未知'
      };
      return texts[this.status] || texts.unknown;
    }
  }
};
</script>

<style scoped>
.status-indicator {
  display: flex;
  align-items: center;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
}

.status-text {
  font-size: 12px;
}

.status-indicator.status-small .status-dot {
  width: 8px;
  height: 8px;
}

.status-indicator.status-small .status-text {
  font-size: 10px;
}

.status-indicator.status-large .status-dot {
  width: 12px;
  height: 12px;
}

.status-indicator.status-large .status-text {
  font-size: 14px;
}
</style>
    