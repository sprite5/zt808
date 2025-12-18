package cn.jascript.zt808.codec;

import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.util.BcdUtil;
import cn.jascript.zt808.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.util.List;

import static cn.jascript.zt808.constants.Protocol.MAX_FRAME_LENGTH;

public class DecoderFactory {
    public static ByteToMessageDecoder getDelimiterDecoder(){
        //去掉头尾7E,且超出长度直接失败
        return new DelimiterBasedFrameDecoder(MAX_FRAME_LENGTH,true,true,Unpooled.wrappedBuffer(new byte[] {0x7E}));
    }

    public static ByteToMessageDecoder getEscapeDecoder(){
        return new EscapeDecoder();
    }

    public static ByteToMessageDecoder getBccValidDecoder(){
        return new BccValidDecoder();
    }

    public static ByteToMessageDecoder getMsgDecoder(){
        return new MessageDecoder();
    }

    //转义解码器
    static class EscapeDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            //单包最少长度12
            if(in.readableBytes() < 12)
                throw new CorruptedFrameException("frame too short");
            ByteBuf decoded = Unpooled.buffer();
            while (in.readableBytes() > 0) {
                byte b = in.readByte();
                if (b == 0x7D) {
                    byte next = in.readByte();
                    if (next == 0x01)
                        decoded.writeByte(0x7D);
                    else if (next == 0x02)
                        decoded.writeByte(0x7E);
                    else {
                        // JT/T 808 标准转义只定义：0x7D 0x01 -> 0x7D，0x7D 0x02 -> 0x7E。
                        // 但实际设备可能会上报非标准序列（例如 0x7D 0x40）。
                        // 为避免“吞字节”导致后续 BCC 校验失败/报文错位，这里采用容错策略：按字面写回 0x7D 与其后继字节。
                        decoded.writeByte(0x7D);
                        decoded.writeByte(next);
                    }
                } else {
                    decoded.writeByte(b);
                }
            }
            out.add(decoded);
        }
    }

    //校验器
    static class BccValidDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            int len = in.readableBytes();
            byte receivedBcc = in.getByte(len - 1); // 最后一个字节为校验位
            byte calculatedBcc = 0;
            for (int i = 0; i < len - 1; i++) {
                calculatedBcc ^= in.getByte(i);
            }
            if (calculatedBcc != receivedBcc)
                throw new CorruptedFrameException("BCC check failed");
            //输出时不包含校验位
            ByteBuf withoutBccBuf = in.slice(0, len - 1).retain();
            out.add(withoutBccBuf);
            //消费所有字节,避免重新进入解码器
            in.skipBytes(len);
        }
    }

    //转换为实体
    static class MessageDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            // 读取消息头
            in.markReaderIndex();
            short msgId = in.readShort();
            short attr = in.readShort();
            //取低10位为长度
            var bodyLen = CodecUtil.getLowTenBitValue(attr);
            byte[] terminalBytes = new byte[6];
            in.readBytes(terminalBytes);
            String terminalId = BcdUtil.replaceLeftZeroString(terminalBytes);
            int flowId = in.readShort();
            // 判断是否分包：bit 13 是否为1
            boolean isSplit = ((attr >> 13) & 0x01) == 1;
            //分包先跳过4字节,避免消息体异常
            var totalPackets = 0;
            var packetSeq = 0;
            if(isSplit) {
                totalPackets = in.readShort(); // 倒数第4~3字节
                packetSeq = in.readShort();     // 倒数第2~1字节
                //包含分包的数据，暂时计入无法处理的数据里
            }
            //
            var bodyByteBuf = in.readBytes(bodyLen);
            var message = new TerminalMessage(terminalId, msgId, flowId, bodyLen, bodyByteBuf);
            out.add(message);
        }
    }


}
