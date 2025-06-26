// 模拟数据 - 连接列表
export const mockConnections = [
  { id: 'CONN-001', name: '市场数据-纽约', type: 'market_data', status: 'active', host: 'fixprod-aim.bloomberg.com', port: 8228, lastActive: '2023-11-22 14:32:45', description: '连接到纽约市场数据中心' },
  { id: 'CONN-002', name: '交易执行-伦敦', type: 'execution', status: 'active', host: 'fixprod-aim.bloomberg.com', port: 8228, lastActive: '2023-11-22 14:32:12', description: '连接到伦敦交易执行中心' },
  { id: 'CONN-003', name: '清算-东京', type: 'clearing', status: 'inactive', host: 'fixprod-aim.bloomberg.com', port: 8228, lastActive: '2023-11-21 09:15:30', description: '连接到东京清算中心' },
  { id: 'CONN-004', name: '风险管理-香港', type: 'risk', status: 'warning', host: 'fixprod-aim.bloomberg.com', port: 8228, lastActive: '2023-11-22 14:28:56', description: '连接到香港风险管理中心' },
  { id: 'CONN-005', name: '市场数据-新加坡', type: 'market_data', status: 'active', host: 'fixprod-aim.bloomberg.com', port: 8228, lastActive: '2023-11-22 14:32:18', description: '连接到新加坡市场数据中心' },
];

// 模拟数据 - IP 允许列表
export const mockIps = [
  { id: 1, ip: '192.168.1.100', type: 'private', status: 'active', lastUsed: '2023-11-22 14:32:45', description: '总部办公室' },
  { id: 2, ip: '192.168.2.50', type: 'private', status: 'active', lastUsed: '2023-11-22 14:32:12', description: '分支机构 A' },
  { id: 3, ip: '69.191.198.50', type: 'internet', status: 'active', lastUsed: '2023-11-22 14:28:56', description: '云服务提供商' },
  { id: 4, ip: '160.43.172.30', type: 'private', status: 'inactive', lastUsed: '2023-11-21 09:15:30', description: '灾备中心' },
  { id: 5, ip: '10.0.0.20', type: 'private', status: 'warning', lastUsed: '2023-11-22 14:10:22', description: '开发环境' },
  { id: 6, ip: '69.191.198.75', type: 'internet', status: 'active', lastUsed: '2023-11-22 14:32:18', description: '数据中心 B' },
  { id: 7, ip: '192.168.3.120', type: 'private', status: 'active', lastUsed: '2023-11-22 14:25:40', description: '分支机构 B' },
  { id: 8, ip: '10.0.1.5', type: 'private', status: 'inactive', lastUsed: '2023-11-20 16:45:10', description: '测试环境' },
];

// 模拟数据 - 证书列表
export const mockCertificates = [
  { id: 'CERT-001', name: '纽约市场数据证书', type: 'rsa2048', status: 'active', issuedBy: 'Bloomberg L.P.', issuedAt: '2021-11-15', expiresAt: '2023-11-15', daysLeft: 0, connectionId: 'CONN-001', certificateContent: '-----BEGIN CERTIFICATE-----\nMIIFfjCCBGagAwIBAgIJAKJ...' },
  { id: 'CERT-002', name: '伦敦交易执行证书', type: 'rsa4096', status: 'active', issuedBy: 'Bloomberg L.P.', issuedAt: '2021-12-01', expiresAt: '2023-12-01', daysLeft: 14, connectionId: 'CONN-002', certificateContent: '-----BEGIN CERTIFICATE-----\nMIIGADCCBBygAwIBAgIJAKJ...' },
  { id: 'CERT-003', name: '东京清算证书', type: 'rsa2048', status: 'expired', issuedBy: 'Bloomberg L.P.', issuedAt: '2021-10-20', expiresAt: '2023-10-20', daysLeft: -30, connectionId: 'CONN-003', certificateContent: '-----BEGIN CERTIFICATE-----\nMIIE5TCCAs2gAwIBAgIJAKJ...' },
  { id: 'CERT-004', name: '香港风险管理证书', type: 'rsa4096', status: 'active', issuedBy: 'Bloomberg L.P.', issuedAt: '2022-01-10', expiresAt: '2024-01-10', daysLeft: 55, connectionId: 'CONN-004', certificateContent: '-----BEGIN CERTIFICATE-----\nMIIFfjCCBGagAwIBAgIJAKJ...' },
  { id: 'CERT-005', name: '新加坡市场数据证书', type: 'rsa2048', status: 'active', issuedBy: 'Bloomberg L.P.', issuedAt: '2022-02-15', expiresAt: '2024-02-15', daysLeft: 85, connectionId: 'CONN-005', certificateContent: '-----BEGIN CERTIFICATE-----\nMIIFfjCCBGagAwIBAgIJAKJ...' },
];

// 模拟数据 - 会话列表
export const mockSessions = [
  { id: 'SESS-001', connectionId: 'CONN-001', connectionName: '市场数据-纽约', status: 'active', messagesReceived: 12543, messagesSent: 0, messagesPerSecond: 24, lastMessageTime: '2023-11-22 14:32:45', sessionStartTime: '2023-11-22 09:00:00', latency: 15, duration: 5 * 60 * 60 + 32 * 60 + 45,
    messages: [
      { direction: 'in', timestamp: '2023-11-22 14:32:45', content: '8=FIX.4.4|9=142|35=W|49=NYMD|56=CLIENT|34=12543|52=20231122-14:32:45|268=3|269=0|270=150.25|271=100|269=1|270=150.30|271=200|269=2|270=150.20|271=150|10=123' },
      { direction: 'in', timestamp: '2023-11-22 14:32:44', content: '8=FIX.4.4|9=142|35=W|49=NYMD|56=CLIENT|34=12542|52=20231122-14:32:44|268=3|269=0|270=150.25|271=100|269=1|270=150.30|271=200|269=2|270=150.20|271=150|10=124' },
      { direction: 'in', timestamp: '2023-11-22 14:32:43', content: '8=FIX.4.4|9=142|35=W|49=NYMD|56=CLIENT|34=12541|52=20231122-14:32:43|268=3|269=0|270=150.25|271=100|269=1|270=150.30|271=200|269=2|270=150.20|271=150|10=125' },
    ]
  },
  { id: 'SESS-002', connectionId: 'CONN-002', connectionName: '交易执行-伦敦', status: 'active', messagesReceived: 432, messagesSent: 321, messagesPerSecond: 3, lastMessageTime: '2023-11-22 14:32:12', sessionStartTime: '2023-11-22 08:30:00', latency: 28, duration: 6 * 60 * 60 + 2 * 60 + 12,
    messages: [
      { direction: 'out', timestamp: '2023-11-22 14:32:12', content: '8=FIX.4.4|9=108|35=D|49=CLIENT|56=LONDON|34=321|52=20231122-14:32:12|11=ORDER123|55=AAPL|54=1|38=100|40=2|44=150.25|59=0|10=156' },
      { direction: 'in', timestamp: '2023-11-22 14:32:11', content: '8=FIX.4.4|9=123|35=8|49=LONDON|56=CLIENT|34=432|52=20231122-14:32:11|11=ORDER123|37=ORDER123A|150=0|39=0|55=AAPL|14=0|32=0|6=0.0|151=100|10=178' },
      { direction: 'out', timestamp: '2023-11-22 14:32:05', content: '8=FIX.4.4|9=108|35=D|49=CLIENT|56=LONDON|34=320|52=20231122-14:32:05|11=ORDER124|55=MSFT|54=1|38=50|40=2|44=300.50|59=0|10=157' },
    ]
  },
  { id: 'SESS-003', connectionId: 'CONN-003', connectionName: '清算-东京', status: 'inactive', messagesReceived: 210, messagesSent: 180, messagesPerSecond: 0, lastMessageTime: '2023-11-21 09:15:30', sessionStartTime: '2023-11-21 08:00:00', latency: 45, duration: 1 * 60 * 60 + 15 * 60 + 30,
    messages: [
      { direction: 'out', timestamp: '2023-11-21 09:15:30', content: '8=FIX.4.4|9=98|35=J|49=CLIENT|56=TOKYO|34=180|52=20231121-09:15:30|102=CLORDID123|55=AAPL|54=1|38=100|15=USD|44=150.25|17=TRADE123|32=100|31=150.25|10=165' },
      { direction: 'in', timestamp: '2023-11-21 09:15:25', content: '8=FIX.4.4|9=88|35=Z|49=TOKYO|56=CLIENT|34=210|52=20231121-09:15:25|102=CLORDID123|17=TRADE123|150=2|10=172' },
    ]
  },
  { id: 'SESS-004', connectionId: 'CONN-004', connectionName: '风险管理-香港', status: 'warning', messagesReceived: 876, messagesSent: 215, messagesPerSecond: 2, lastMessageTime: '2023-11-22 14:28:56', sessionStartTime: '2023-11-22 09:15:00', latency: 62, duration: 5 * 60 * 60 + 13 * 60 + 56,
    messages: [
      { direction: 'in', timestamp: '2023-11-22 14:28:56', content: '8=FIX.4.4|9=112|35=V|49=HKRISK|56=CLIENT|34=876|52=20231122-14:28:56|167=CS|55=AAPL|453=2|448=ACCT1|447=D|452=1|448=ACCT2|447=D|452=1|701=100|702=150.25|10=182' },
      { direction: 'out', timestamp: '2023-11-22 14:28:50', content: '8=FIX.4.4|9=102|35=U|49=CLIENT|56=HKRISK|34=215|52=20231122-14:28:50|167=CS|55=AAPL|453=2|448=ACCT1|447=D|452=1|448=ACCT2|447=D|452=1|701=100|702=150.25|10=174' },
    ]
  },
  { id: 'SESS-005', connectionId: 'CONN-005', connectionName: '市场数据-新加坡', status: 'active', messagesReceived: 9876, messagesSent: 0, messagesPerSecond: 18, lastMessageTime: '2023-11-22 14:32:18', sessionStartTime: '2023-11-22 08:45:00', latency: 22, duration: 5 * 60 * 60 + 47 * 60 + 18,
    messages: [
      { direction: 'in', timestamp: '2023-11-22 14:32:18', content: '8=FIX.4.4|9=142|35=W|49=SGMD|56=CLIENT|34=9876|52=20231122-14:32:18|268=3|269=0|270=150.25|271=100|269=1|270=150.30|271=200|269=2|270=150.20|271=150|10=126' },
      { direction: 'in', timestamp: '2023-11-22 14:32:17', content: '8=FIX.4.4|9=142|35=W|49=SGMD|56=CLIENT|34=9875|52=20231122-14:32:17|268=3|269=0|270=150.25|271=100|269=1|270=150.30|271=200|269=2|270=150.20|271=150|10=127' },
    ]
  },
];

// 模拟数据 - 警报列表
export const mockAlerts = [
  { id: 'ALERT-001', title: '证书即将过期', level: 'warning', source: '证书管理系统', timestamp: '2023-11-22 14:30:00', status: 'pending', connectionId: 'CONN-002',
    message: '伦敦交易执行证书 (CERT-002) 将在 14 天后过期，请及时更新。',
    recommendedActions: [
      '在 Enterprise Console 中生成新证书',
      '将新证书部署到相关连接',
      '确认连接使用新证书正常工作'
    ]
  },
  { id: 'ALERT-002', title: '连接断开', level: 'danger', source: '会话监控系统', timestamp: '2023-11-22 14:25:00', status: 'pending', connectionId: 'CONN-003',
    message: '东京清算中心连接已断开，请检查网络连接或证书状态。',
    recommendedActions: [
      '检查网络连接状态',
      '确认证书是否有效',
      '尝试重新连接',
      '联系彭博技术支持'
    ]
  },
  { id: 'ALERT-003', title: '消息延迟异常', level: 'warning', source: '会话监控系统', timestamp: '2023-11-22 14:20:00', status: 'resolved', resolvedBy: '管理员', resolvedAt: '2023-11-22 14:22:30', connectionId: 'CONN-004',
    message: '香港风险管理连接的消息延迟超过阈值 (62ms > 50ms)，请检查网络状况。',
    recommendedActions: [
      '检查网络延迟和带宽',
      '确认服务器负载情况',
      '考虑使用备用连接'
    ]
  },
  { id: 'ALERT-004', title: 'IP 登录尝试失败', level: 'danger', source: '安全系统', timestamp: '2023-11-22 14:15:00', status: 'pending',
    message: '检测到来自未授权 IP 地址 192.168.4.100的多次登录尝试，可能存在安全威胁。',
    recommendedActions: [
      '检查防火墙日志确认攻击来源',
      '确认是否需要添加该 IP 到允许列表',
      '增加账户安全措施，如启用双因素认证'
    ]
  },
  { id: 'ALERT-005', title: '证书已过期', level: 'danger', source: '证书管理系统', timestamp: '2023-11-22 14:10:00', status: 'pending', connectionId: 'CONN-001',
    message: '纽约市场数据证书 (CERT-001) 已过期，请立即更新以避免连接中断。',
    recommendedActions: [
      '在 Enterprise Console 中生成新证书',
      '将新证书部署到相关连接',
      '确认连接使用新证书正常工作',
      '检查是否有其他证书即将过期'
    ]
  },
  { id: 'ALERT-006', title: '系统维护通知', level: 'info', source: '彭博系统', timestamp: '2023-11-22 10:00:00', status: 'resolved', resolvedBy: '系统', resolvedAt: '2023-11-22 10:00:00',
    message: '彭博 FIX 基础设施将于本周六 6:00（东部时间）至周日 18:00 进行例行维护，期间可能有短暂中断。',
    recommendedActions: [
      '安排系统维护窗口',
      '通知相关团队可能的服务中断',
      '确保关键业务流程有备用方案'
    ]
  },
];

// 模拟数据 - 审计日志
export const mockLogs = [
  { id: 'LOG-001', timestamp: '2023-11-22 14:32:45', type: 'connection', user: '管理员', action: '测试连接', objectType: 'connection', objectId: 'CONN-001', ipAddress: '192.168.1.100',
    details: '测试连接 CONN-001 (市场数据-纽约) 成功，响应时间 15ms。\n\n连接信息:\n- 服务器: fixprod-aim.bloomberg.com:8228\n- 协议: FIX.4.4\n- 会话参数: BeginString=FIX.4.4, SenderCompID=CLIENT, TargetCompID=NYMD'
  },
  { id: 'LOG-002', timestamp: '2023-11-22 14:30:00', type: 'alert', user: '系统', action: '生成警报', objectType: 'alert', objectId: 'ALERT-001', ipAddress: '192.168.1.100',
    details: '生成警报 ALERT-001 (证书即将过期)。\n\n警报详情:\n- 证书 ID: CERT-002\n- 证书名称: 伦敦交易执行证书\n- 剩余天数: 14 天\n- 触发条件: 证书过期时间 < 30 天'
  },
  { id: 'LOG-003', timestamp: '2023-11-22 14:25:00', type: 'security', user: '系统', action: '检测到异常', objectType: 'ip', objectId: '192.168.4.100', ipAddress: '192.168.4.100',
    details: '检测到来自未授权 IP 地址 192.168.4.100的多次登录尝试。\n\n尝试详情:\n- 尝试次数: 5 次\n- 时间范围: 2023-11-22 14:10:00 至 2023-11-22 14:15:00\n- 响应: 拒绝访问'
  },
  { id: 'LOG-004', timestamp: '2023-11-22 14:22:30', type: 'alert', user: '管理员', action: '解决警报', objectType: 'alert', objectId: 'ALERT-003', ipAddress: '192.168.1.100',
    details: '解决警报 ALERT-003 (消息延迟异常)。\n\n解决措施:\n- 重启网络设备\n- 优化服务器配置\n- 切换至备用连接\n\n结果:\n- 延迟恢复正常 (45ms)\n- 确认系统稳定性'
  },
  { id: 'LOG-005', timestamp: '2023-11-22 14:20:00', type: 'session', user: '系统', action: '监控到异常', objectType: 'session', objectId: 'SESS-004', ipAddress: '192.168.1.100',
    details: '监控到会话 SESS-004 (风险管理-香港) 异常。\n\n异常详情:\n- 延迟: 62ms (阈值: 50ms)\n- 消息率: 2 msg/s (正常: 5-10 msg/s)\n- 状态: 警告\n\n建议操作:\n- 检查网络状况\n- 确认服务器负载'
  },
  { id: 'LOG-006', timestamp: '2023-11-22 14:15:00', type: 'security', user: '管理员', action: '添加 IP', objectType: 'ip', objectId: '192.168.5.100', ipAddress: '192.168.1.100',
    details: '添加新 IP 地址 192.168.5.100 到允许列表。\n\nIP 详情:\n- 类型: private\n- 描述: 新分支机构 C\n- 状态: 激活\n- 备注: 该 IP 将用于分支机构 C 的交易系统连接'
  },
  { id: 'LOG-007', timestamp: '2023-11-22 14:10:00', type: 'certificate', user: '系统', action: '检测到过期', objectType: 'certificate', objectId: 'CERT-001', ipAddress: '192.168.1.100',
    details: '检测到证书 CERT-001 (纽约市场数据证书) 已过期。\n\n证书详情:\n- 类型: rsa2048\n- 颁发者: Bloomberg L.P.\n- 颁发日期: 2021-11-15\n- 过期日期: 2023-11-15\n- 状态: 过期\n\n影响:\n- 连接 CONN-001 可能无法正常工作\n- 建议立即更新证书'
  },
  { id: 'LOG-008', timestamp: '2023-11-22 10:00:00', type: 'system', user: '彭博系统', action: '发布通知', objectType: 'notification', objectId: 'NOTIF-001', ipAddress: '0.0.0.0',
    details: '发布系统维护通知。\n\n通知详情:\n- 标题: 彭博 FIX 基础设施例行维护\n- 内容: 彭博 FIX 基础设施将于本周六 6:00（东部时间）至周日 18:00 进行例行维护，期间可能有短暂中断。\n- 影响: 所有彭博 FIX 连接\n- 建议: 安排系统维护窗口，通知相关团队可能的服务中断'
  },
];
    