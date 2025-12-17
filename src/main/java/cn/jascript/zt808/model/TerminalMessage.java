package cn.jascript.zt808.model;


import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TerminalMessage {
    //设备号
    private String terminalId;
    //消息ID(协议头msgId)
    private int msgId;
    //流水号
    private int flowId;
    //消息体长度
    private int bodyLen;
    //具体消息体
    private ByteBuf body;
}
