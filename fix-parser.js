/**
 * FIX消息解析器 - 用于解析FIX协议消息并提取关键数据
 */
class FixMessageParser {
  constructor() {
    // 定义常用FIX标签
    this.TAGS = {
      MsgType: "35",
      Symbol: "55",
      QuoteReqID: "131",
      QuoteID: "117",
      BidPx: "132",
      OfferPx: "133",
      MDReqID: "262",
      MDEntryType: "269",
      MDEntryPx: "270",
      MDEntrySize: "271",
      OrdStatus: "39",
      ExecType: "150",
      AvgPx: "6",
      CumQty: "14"
    };
    
    // 定义消息类型
    this.MSG_TYPES = {
      QuoteRequest: "R",
      Quote: "S",
      MarketDataRequest: "V",
      MarketDataSnapshot: "W",
      ExecutionReport: "8"
    };
    
    // 定义条目类型
    this.MD_ENTRY_TYPES = {
      Bid: "0",
      Offer: "1",
      Trade: "2"
    };
    
    // 定义订单状态
    this.ORDER_STATUSES = {
      "0": "新建",
      "1": "部分成交",
      "2": "全部成交",
      "3": "已取消",
      "4": "已替换",
      "5": "待取消",
      "6": "待替换",
      "7": "待提交",
      "8": "拒绝",
      "F": "全部成交"
    };
  }
  
  /**
   * 解析FIX消息字符串为字段对象
   * @param {string} fixMessage - 原始FIX消息字符串（使用SOH或|分隔）
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
      
      // 检测新的重复组
      if (tag.endsWith('8') && value && !isNaN(parseInt(value))) {
        groupTag = (parseInt(tag) + 1).toString();
        groupCount = 0;
        currentGroup = null;
      }
    });
    
    return parsedFields;
  }
  
  /**
   * 解析Quote消息 (MsgType=S)
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的报价对象
   */
  parseQuote(fixMessage) {
    const fields = this.parse(fixMessage);
    
    if (fields[this.TAGS.MsgType] !== this.MSG_TYPES.Quote) {
      return null;
    }
    
    return {
      type: 'quote',
      quoteId: fields[this.TAGS.QuoteID],
      symbol: fields[this.TAGS.Symbol],
      bidPrice: parseFloat(fields[this.TAGS.BidPx]),
      offerPrice: parseFloat(fields[this.TAGS.OfferPx])
    };
  }
  
  /**
   * 解析MarketDataSnapshot消息 (MsgType=W)
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的市场数据对象
   */
  parseMarketData(fixMessage) {
    const fields = this.parse(fixMessage);
    
    if (fields[this.TAGS.MsgType] !== this.MSG_TYPES.MarketDataSnapshot) {
      return null;
    }
    
    const entries = fields["268"] || []; // NoMDEntries字段
    const bids = [];
    const offers = [];
    
    entries.forEach(entry => {
      const type = entry[this.TAGS.MDEntryType];
      const price = parseFloat(entry[this.TAGS.MDEntryPx]);
      const size = parseFloat(entry[this.TAGS.MDEntrySize]);
      
      if (type === this.MD_ENTRY_TYPES.Bid) {
        bids.push({ price, size });
      } else if (type === this.MD_ENTRY_TYPES.Offer) {
        offers.push({ price, size });
      }
    });
    
    // 按价格排序 (买价从高到低，卖价从低到高)
    bids.sort((a, b) => b.price - a.price);
    offers.sort((a, b) => a.price - b.price);
    
    return {
      type: 'marketData',
      symbol: fields[this.TAGS.Symbol],
      bids,
      offers
    };
  }
  
  /**
   * 解析ExecutionReport消息 (MsgType=8)
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的执行报告对象
   */
  parseExecutionReport(fixMessage) {
    const fields = this.parse(fixMessage);
    
    if (fields[this.TAGS.MsgType] !== this.MSG_TYPES.ExecutionReport) {
      return null;
    }
    
    return {
      type: 'execution',
      symbol: fields[this.TAGS.Symbol],
      side: fields["54"] === "1" ? "buy" : "sell",
      price: parseFloat(fields[this.TAGS.AvgPx]),
      quantity: parseFloat(fields[this.TAGS.CumQty]),
      status: fields[this.TAGS.OrdStatus],
      statusText: this.ORDER_STATUSES[fields[this.TAGS.OrdStatus]] || "未知"
    };
  }
  
  /**
   * 根据消息类型自动解析消息
   * @param {string} fixMessage - 原始FIX消息字符串
   * @returns {Object|null} - 解析后的对象
   */
  autoParse(fixMessage) {
    const fields = this.parse(fixMessage);
    const msgType = fields[this.TAGS.MsgType];
    
    switch (msgType) {
      case this.MSG_TYPES.Quote:
        return this.parseQuote(fixMessage);
      case this.MSG_TYPES.MarketDataSnapshot:
        return this.parseMarketData(fixMessage);
      case this.MSG_TYPES.ExecutionReport:
        return this.parseExecutionReport(fixMessage);
      default:
        return {
          type: 'unknown',
          rawFields: fields
        };
    }
  }
}

// 使用示例
const parser = new FixMessageParser();

// 示例1: 解析Quote消息
const quoteMessage = "8=FIX.4.4|9=123|35=S|49=ISSUER|56=TRADER|34=123|52=20250708-14:30:00|117=Q12345|55=123456|132=100.5|133=100.7|10=234";
const quote = parser.parseQuote(quoteMessage);
console.log("解析后的报价:", quote);

// 示例2: 解析MarketDataSnapshot消息
const marketDataMessage = "8=FIX.4.4|9=156|35=W|49=EXCHANGE|56=TRADER|34=456|52=20250708-14:30:00|262=MDR123|55=123456|268=2|269=0|270=100.5|271=100|269=1|270=100.7|271=200|10=256";
const marketData = parser.parseMarketData(marketDataMessage);
console.log("解析后的市场数据:", marketData);

// 示例3: 解析ExecutionReport消息
const executionMessage = "8=FIX.4.4|9=145|35=8|49=BROKER|56=TRADER|34=789|52=20250708-14:30:00|11=CL12345|37=ORD12345|150=2|39=2|55=123456|54=1|6=100.5|14=100|151=0|10=245";
const execution = parser.parseExecutionReport(executionMessage);
console.log("解析后的执行报告:", execution);    