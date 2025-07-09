/**
 * 增强版FIX消息解析器 - 专门处理市场数据消息
 */
class FixMarketDataParser {
  constructor() {
    // 定义常用FIX标签
    this.TAGS = {
      MsgType: "35",
      Symbol: "55",
      MDReqID: "262",
      NoMDEntries: "268",      // 市场数据条目计数
      MDEntryType: "269",      // 条目类型
      MDEntryPx: "270",        // 价格
      MDEntrySize: "271",      // 数量
      MDEntryPositionNo: "279" // 位置（用于增量更新）
    };
    
    // 定义消息类型
    this.MSG_TYPES = {
      MarketDataSnapshot: "W",
      MarketDataIncremental: "X"
    };
    
    // 定义条目类型
    this.MD_ENTRY_TYPES = {
      Bid: "0",
      Offer: "1",
      Trade: "2",
      OpeningPrice: "4",
      ClosingPrice: "5",
      HighPrice: "6",
      LowPrice: "7",
      TradingSessionHighPrice: "8",
      TradingSessionLowPrice: "9"
    };
  }
  
  /**
   * 解析FIX消息字符串为字段对象
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object} - 解析后的字段对象
   */
  parse(fixMessage) {
    // 处理使用"|"替代SOH的情况
    const fields = fixMessage.split(/[\x01|]/).filter(field => field.trim() !== '');
    
    const parsedFields = {};
    let currentGroup = null;
    let groupTag = null;
    let groupCount = 0;
    
    fields.forEach(field => {
      const [tag, value] = field.split('=');
      
      // 处理重复组
      if (tag === groupTag) {
        groupCount++;
        currentGroup = parsedFields[groupTag] = parsedFields[groupTag] || [];
        currentGroup[groupCount - 1] = currentGroup[groupCount - 1] || {};
      } else if (currentGroup && currentGroup[groupCount - 1]) {
        currentGroup[groupCount - 1][tag] = value;
        return;
      }
      
      // 非重复组字段
      parsedFields[tag] = value;
      
      // 检测新的重复组 (FIX协议中，重复组计数标签通常以"8"结尾)
      if (tag.endsWith('8') && value && !isNaN(parseInt(value))) {
        groupTag = (parseInt(tag) + 1).toString();
        groupCount = 0;
        currentGroup = null;
      }
    });
    
    return parsedFields;
  }
  
  /**
   * 解析MarketDataSnapshotFullRefresh消息 (MsgType=W)
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的市场数据对象
   */
  parseMarketDataSnapshot(fixMessage) {
    const fields = this.parse(fixMessage);
    
    if (fields[this.TAGS.MsgType] !== this.MSG_TYPES.MarketDataSnapshot) {
      return null;
    }
    
    return {
      type: 'snapshot',
      requestId: fields[this.TAGS.MDReqID],
      symbol: fields[this.TAGS.Symbol],
      entries: this._parseMDEntries(fields)
    };
  }
  
  /**
   * 解析MarketDataIncrementalRefresh消息 (MsgType=X)
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的增量市场数据对象
   */
  parseMarketDataIncremental(fixMessage) {
    const fields = this.parse(fixMessage);
    
    if (fields[this.TAGS.MsgType] !== this.MSG_TYPES.MarketDataIncremental) {
      return null;
    }
    
    return {
      type: 'incremental',
      requestId: fields[this.TAGS.MDReqID],
      symbol: fields[this.TAGS.Symbol],
      entries: this._parseMDEntries(fields)
    };
  }
  
  /**
   * 解析市场数据条目 (NoMDEntries重复组)
   * @param {Object} fields - 解析后的字段对象
   * @returns {Array} - 解析后的市场数据条目数组
   */
  _parseMDEntries(fields) {
    const entries = fields[this.TAGS.NoMDEntries] || [];
    
    return entries.map(entry => {
      const entryType = entry[this.TAGS.MDEntryType];
      const position = entry[this.TAGS.MDEntryPositionNo];
      
      return {
        type: this._getEntryTypeText(entryType),
        rawType: entryType,
        price: parseFloat(entry[this.TAGS.MDEntryPx] || 0),
        size: parseFloat(entry[this.TAGS.MDEntrySize] || 0),
        position: position ? parseInt(position) : null,
        action: this._getActionType(entry)
      };
    });
  }
  
  /**
   * 获取条目类型文本描述
   * @param {string} entryType - 条目类型代码
   * @returns {string} - 类型描述
   */
  _getEntryTypeText(entryType) {
    switch (entryType) {
      case this.MD_ENTRY_TYPES.Bid: return '买入';
      case this.MD_ENTRY_TYPES.Offer: return '卖出';
      case this.MD_ENTRY_TYPES.Trade: return '成交';
      case this.MD_ENTRY_TYPES.OpeningPrice: return '开盘价';
      case this.MD_ENTRY_TYPES.ClosingPrice: return '收盘价';
      default: return '未知类型(' + entryType + ')';
    }
  }
  
  /**
   * 获取增量更新的操作类型 (仅用于增量消息)
   * @param {Object} entry - 市场数据条目
   * @returns {string} - 操作类型 ('新增'、'修改'、'删除')
   */
  _getActionType(entry) {
    // MDEntryPositionNo=0 表示新增
    // MDEntryPositionNo>0 表示修改
    // 没有MDEntryPositionNo且size=0 表示删除
    if (entry[this.TAGS.MDEntryPositionNo] === '0') {
      return '新增';
    } else if (entry[this.TAGS.MDEntryPositionNo]) {
      return '修改';
    } else if (entry[this.TAGS.MDEntrySize] === '0') {
      return '删除';
    }
    return '新增';
  }
  
  /**
   * 根据消息类型自动解析市场数据消息
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的对象
   */
  autoParse(fixMessage) {
    const fields = this.parse(fixMessage);
    const msgType = fields[this.TAGS.MsgType];
    
    switch (msgType) {
      case this.MSG_TYPES.MarketDataSnapshot:
        return this.parseMarketDataSnapshot(fixMessage);
      case this.MSG_TYPES.MarketDataIncremental:
        return this.parseMarketDataIncremental(fixMessage);
      default:
        return null;
    }
  }
}

// 使用示例
const parser = new FixMarketDataParser();

// 示例1: 解析MarketDataSnapshot消息
const snapshotMessage = "8=FIX.4.4|9=156|35=W|262=MDR123|55=123456|268=2|269=0|270=100.5|271=100|269=1|270=100.7|271=200|10=256";
const snapshot = parser.parseMarketDataSnapshot(snapshotMessage);
console.log("解析后的市场数据快照:", snapshot);

// 示例2: 解析MarketDataIncremental消息
const incrementalMessage = "8=FIX.4.4|9=180|35=X|262=MDR123|55=123456|268=2|279=0|269=0|270=100.6|271=150|279=1|269=1|270=100.8|271=250|10=270";
const incremental = parser.parseMarketDataIncremental(incrementalMessage);
console.log("解析后的市场数据增量:", incremental);    