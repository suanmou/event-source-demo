package com.example.fixclient;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Logon;
import quickfix.fix44.Reject;

public class FIXClientApplication implements Application {
    private final FIXClient client;
    
    public FIXClientApplication(FIXClient client) {
        this.client = client;
    }
    
    @Override
    public void onCreate(SessionID sessionID) {
        System.out.println("Session created: " + sessionID);
    }
    
    @Override
    public void onLogon(SessionID sessionID) {
        System.out.println("Logon received: " + sessionID);
    }
    
    @Override
    public void onLogout(SessionID sessionID) {
        System.out.println("Logout received: " + sessionID);
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        // 处理发送到管理层的消息
        if (message instanceof Logon) {
            Logon logon = (Logon) message;
            // 可以在这里设置登录信息，如用户名和密码
            // logon.set(new Username("username"));
            // logon.set(new Password("password"));
        }
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // 处理来自管理层的消息
    }
    
    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        // 处理发送到应用层的消息
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // 处理来自应用层的消息
        try {
            if (message instanceof ExecutionReport) {
                handleExecutionReport((ExecutionReport) message);
            } else if (message instanceof Reject) {
                handleReject((Reject) message);
            } else {
                System.out.println("收到未处理的消息类型: " + message.getHeader().getField(new MsgType()).getValue());
            }
        } catch (Exception e) {
            System.out.println("处理消息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleExecutionReport(ExecutionReport executionReport) throws FieldNotFound {
        System.out.println("收到执行报告: " + executionReport.getClOrdID().getValue());
        System.out.println("订单状态: " + executionReport.getOrdStatus().getValue());
        System.out.println("执行类型: " + executionReport.getExecType().getValue());
        System.out.println("订单ID: " + executionReport.getOrderID().getValue());
        System.out.println("执行ID: " + executionReport.getExecID().getValue());
        System.out.println("成交量: " + executionReport.getCumQty().getValue());
        System.out.println("剩余量: " + executionReport.getLeavesQty().getValue());
        
        if (executionReport.isSetAvgPx()) {
            System.out.println("平均价格: " + executionReport.getAvgPx().getValue());
        }
    }
    
    private void handleReject(Reject reject) throws FieldNotFound {
        System.out.println("收到拒绝消息");
        if (reject.isSetRefSeqNum()) {
            System.out.println("参考序列号: " + reject.getRefSeqNum().getValue());
        }
        if (reject.isSetRefMsgType()) {
            System.out.println("参考消息类型: " + reject.getRefMsgType().getValue());
        }
        if (reject.isSetText()) {
            System.out.println("拒绝原因: " + reject.getText().getValue());
        }
    }
}
    