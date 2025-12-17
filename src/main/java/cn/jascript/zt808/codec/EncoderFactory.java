package cn.jascript.zt808.codec;

import cn.jascript.zt808.constants.Protocol;
import cn.jascript.zt808.model.PlatformMessage;
import cn.jascript.zt808.util.BcdUtil;
import cn.jascript.zt808.util.CodecUtil;
import cn.jascript.zt808.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class EncoderFactory {

    private static final int PHONE_LENGTH_BYTES = 6;

    private EncoderFactory() {
    }

    public static MessageToByteEncoder<PlatformMessage> getMsgEncoder() {
        //使用单例可复用的encoder
        return MsgEncoderHolder.ENCODER;
    }

    @ChannelHandler.Sharable
    private static class MsgEncoder extends MessageToByteEncoder<PlatformMessage> {
        @Override
        protected void encode(ChannelHandlerContext ctx, PlatformMessage msg, ByteBuf out) {
            if (Objects.isNull(msg)) {
                return;
            }
            int startIndex = out.writerIndex();
            ByteBuf frame = ctx.alloc().buffer();
            try {
                //组装消息头和消息体
                writeHeaderAndBody(frame, msg);
                //计算BCC校验
                byte bcc = CodecUtil.calculateBcc(frame);
                //首尾固定0x7E,中间内容需要转义
                out.writeByte(Protocol.HEADER);
                writeEscaped(out, frame);
                writeEscaped(out, bcc);
                out.writeByte(Protocol.TAIL);

                if (log.isDebugEnabled()) {
                    int endIndex = out.writerIndex();
                    int len = endIndex - startIndex;
                    if (len > 0) {
                        byte[] bytes = new byte[len];
                        out.getBytes(startIndex, bytes);
                        log.debug("downlink encoded, terminalId={}, msgId=0x{}, flowId={}, hex={}",
                                msg.getTerminalId(), Integer.toHexString(msg.getMsgId()), msg.getFlowId(), HexUtil.toUpperHex(bytes));
                    }
                }
            } finally {
                frame.release();
            }
        }
    }

    private static class MsgEncoderHolder {
        private static final MsgEncoder ENCODER = new MsgEncoder();
    }

    private static void writeHeaderAndBody(ByteBuf buf, PlatformMessage msg) {
        buf.writeShort(msg.getMsgId());
        ByteBuf body = msg.getBody();
        int bodyLength = (Objects.isNull(body) ? 0 : body.readableBytes());
        //JT/T808约定消息体长度占10位,单帧最大1023字节
        if (bodyLength > Protocol.MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("body length too large:" + bodyLength);
        }
        buf.writeShort(bodyLength & Protocol.MAX_BODY_LENGTH);
        //终端号使用6字节BCD编码
        buf.writeBytes(BcdUtil.fromString(msg.getTerminalId(), PHONE_LENGTH_BYTES));
        buf.writeShort(CodecUtil.toUnsignedShort(msg.getFlowId()));
        if (bodyLength > 0) {
            buf.writeBytes(body.duplicate());
        }
    }

    private static void writeEscaped(ByteBuf out, ByteBuf data) {
        for (int i = data.readerIndex(); i < data.writerIndex(); i++) {
            writeEscaped(out, data.getByte(i));
        }
    }

    private static void writeEscaped(ByteBuf out, byte value) {
        //0x7E/0x7D需按协议进行转义
        if (value == Protocol.HEADER) {
            out.writeByte(Protocol.ESCAPE).writeByte(Protocol.ESCAPE_FOR_HEADER);
        } else if (value == Protocol.ESCAPE) {
            out.writeByte(Protocol.ESCAPE).writeByte(Protocol.ESCAPE_FOR_ESCAPE);
        } else {
            out.writeByte(value);
        }
    }
}
