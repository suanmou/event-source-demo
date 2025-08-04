const fix = require('node-fix');
const fs = require('fs');
const path = require('path');

// 1. 配置文件路径
const configPath = path.join(__dirname, 'fix-client.cfg');

// 2. 创建配置文件（如果不存在）
function createConfigFile() {
    if (!fs.existsSync(configPath)) {
        const config = `[DEFAULT]
ConnectionType=initiator
ReconnectInterval=5
FileLogPath=./logs
FileStorePath=./store

[SESSION]
BeginString=FIX.4.4
SenderCompID=NODE_CLIENT
TargetCompID=FIX_ENGINE
SocketConnectHost=localhost
SocketConnectPort=1234
HeartBtInt=30
StartTime=00:00:00
EndTime=23:59:59
ResetOnLogon=Y
ResetOnDisconnect=Y
ResetOnLogout=Y
`;
        fs.writeFileSync(configPath, config);
        console.log(`配置文件已创建: ${configPath}`);
    }
}

// 3. 创建FIX客户端
function createFixClient() {
    // 初始化日志和存储
    const logFactory = new fix.FileLogFactory(configPath);
    const storeFactory = new fix.FileStoreFactory(configPath);
    
    // 创建会话设置
    const settings = new fix.Settings(configPath);
    
    // 创建应用层
    class FixApplication extends fix.Application {
        constructor() {
            super();
            this.session = null;
        }
        
        //  onCreate - 会话创建时调用
        onCreate(sessionID) {
            console.log(`会话创建: ${sessionID.toString()}`);
            this.session = sessionID;
        }
        
        // onLogon - 登录成功时调用
        onLogon(sessionID) {
            console.log(`登录成功: ${sessionID.toString()}`);
            this.session = sessionID;
            
            // 登录成功后发送测试订单
            setTimeout(() => {
                this.sendNewOrder();
            }, 1000);
        }
        
        // onLogout - 登出时调用
        onLogout(sessionID) {
            console.log(`登出: ${sessionID.toString()}`);
        }
        
        // toAdmin - 发送管理消息前调用
        toAdmin(message, sessionID) {
            const msgType = message.getHeader().getField(35);
            console.log(`发送管理消息 [${msgType}]: ${sessionID.toString()}`);
        }
        
        // fromAdmin - 接收管理消息时调用
        fromAdmin(message, sessionID) {
            const msgType = message.getHeader().getField(35);
            console.log(`接收管理消息 [${msgType}]: ${sessionID.toString()}`);
        }
        
        // toApp - 发送应用消息前调用
        toApp(message, sessionID) {
            const msgType = message.getHeader().getField(35);
            console.log(`发送应用消息 [${msgType}]: ${sessionID.toString()}`);
        }
        
        // fromApp - 接收应用消息时调用
        fromApp(message, sessionID) {
            const msgType = message.getHeader().getField(35);
            console.log(`接收应用消息 [${msgType}]: ${sessionID.toString()}`);
            
            // 解析接收到的消息
            this.parseIncomingMessage(message);
        }
        
        // 解析收到的消息
        parseIncomingMessage(message) {
            const msgType = message.getHeader().getField(35);
            
            switch (msgType) {
                case '8': // 执行报告
                    const execType = message.getField(150);
                    const ordStatus = message.getField(39);
                    const clOrdId = message.getField(11);
                    console.log(`订单状态 [${clOrdId}]: 执行类型=${execType}, 订单状态=${ordStatus}`);
                    break;
                    
                case 'D': // 新订单确认
                    console.log('收到新订单确认');
                    break;
            }
        }
        
        // 发送新订单
        sendNewOrder() {
            if (!this.session) {
                console.error('未建立会话，无法发送订单');
                return;
            }
            
            // 创建新订单消息
            const order = new fix.Message();
            
            // 设置消息头
            order.getHeader().setField(35, 'D'); // MsgType = 新订单
            order.getHeader().setField(49, this.session.senderCompID); // SenderCompID
            order.getHeader().setField(56, this.session.targetCompID); // TargetCompID
            
            // 设置订单字段
            order.setField(11, `ORD-${Date.now()}`); // ClOrdID - 客户端订单ID
            order.setField(21, '1'); // HandlInst - 手工执行
            order.setField(55, 'AAPL'); // Symbol - 交易代码
            order.setField(54, '1'); // Side - 1=买入
            order.setField(38, '100'); // OrderQty
            order.setField(40, '2'); // OrdType - 2=限价单
            order.setField(44, '150.50'); // Price
            order.setField(59, '0'); // TimeInForce - 0=当日有效
            order.setField(60, new Date().toISOString().replace(/\.\d{3}/, '')); // TransactTime
            
            // 发送订单
            try {
                fix.Session.sendToTarget(order, this.session);
                console.log(`发送新订单: ${order.getField(11)}`);
            } catch (error) {
                console.error(`发送订单失败: ${error.message}`);
            }
        }
    }
    
    // 创建应用实例
    const application = new FixApplication();
    
    // 创建 initiator
    const initiator = new fix.SocketInitiator(
        application,
        storeFactory,
        settings,
        logFactory
    );
    
    return { initiator, application };
}

// 4. 启动客户端
function startClient() {
    createConfigFile();
    
    const { initiator } = createFixClient();
    
    // 启动 initiator
    initiator.start();
    console.log('FIX客户端已启动，正在连接到FIX引擎...');
    
    // 处理退出信号
    process.on('SIGINT', () => {
        console.log('正在关闭客户端...');
        initiator.stop();
        process.exit(0);
    });
}

// 启动客户端
startClient();
    