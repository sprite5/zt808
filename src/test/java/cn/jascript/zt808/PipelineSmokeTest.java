package cn.jascript.zt808;

import cn.jascript.zt808.codec.DecoderFactory;
import cn.jascript.zt808.codec.EncoderFactory;
import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.constants.Protocol;
import cn.jascript.zt808.handler.DataEventHandler;
import cn.jascript.zt808.session.SessionManager;
import cn.jascript.zt808.testkit.JT808TestKit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pipeline baseline test (fail => check codec/handlers/config)")
public class PipelineSmokeTest {

    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(channel)) {
            SessionManager.getInstance().unregister(channel);
            channel.finishAndReleaseAll();
        }
    }

    @Test
    @DisplayName("Baseline: register/auth/heartbeat/location should reply correctly")
    void baseline_shouldReplyCorrectly_forTypicalUplinkFrames() {
        channel = new EmbeddedChannel(
                DecoderFactory.getDelimiterDecoder(),
                DecoderFactory.getEscapeDecoder(),
                DecoderFactory.getBccValidDecoder(),
                DecoderFactory.getMsgDecoder(),
                new DataEventHandler(),
                EncoderFactory.getMsgEncoder()
        );

        String terminalId = "14540756282";
        String authCode = AppConfig.get().getAuth().getCode();

        writeInboundAndAssertNoError(JT808TestKit.registerFrame(
                terminalId,
                0x00C8,
                0x002C,
                0x012F,
                "70111",
                "KM-02R",
                "0000000"
        ));
        assertOutboundMsgId(MsgId.REGISTER_REPLY);

        writeInboundAndAssertNoError(JT808TestKit.authFrame(terminalId, 0x00C9, authCode));
        assertOutboundGeneralReply(0x00C9, MsgId.AUTH, 0);

        writeInboundAndAssertNoError(JT808TestKit.heartbeatFrame(terminalId, 0x00CA));
        assertOutboundGeneralReply(0x00CA, MsgId.HEARTBEAT, 0);

        writeInboundAndAssertNoError(JT808TestKit.locationFrame(
                terminalId,
                0x00CD,
                0,
                0,
                31.123456,
                121.654321,
                0,
                80,
                0,
                LocalDateTime.of(2025, 12, 15, 10, 0, 13)
        ));
        assertOutboundGeneralReply(0x00CD, MsgId.LOCATION, 0);

        writeInboundAndAssertNoError(JT808TestKit.heartbeatFrame(terminalId, 0x00D7));
        assertOutboundGeneralReply(0x00D7, MsgId.HEARTBEAT, 0);

        writeInboundAndAssertNoError(JT808TestKit.locationFrame(
                terminalId,
                0x00D8,
                0,
                0,
                31.123400,
                121.654300,
                0,
                80,
                0,
                LocalDateTime.of(2025, 12, 15, 10, 23, 22)
        ));
        assertOutboundGeneralReply(0x00D8, MsgId.LOCATION, 0);
    }

    private void writeInboundAndAssertNoError(byte[] bytes) {
        assertDoesNotThrow(() -> {
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            channel.writeInbound(buf);
            channel.runPendingTasks();
            channel.checkException();
        });
    }

    private void assertOutboundMsgId(int expectedMsgId) {
        ByteBuf outbound = channel.readOutbound();
        assertNotNull(outbound);
        try {
            var decoded = decodeOutboundFrame(outbound);
            assertEquals(expectedMsgId, decoded.msgId);
        } finally {
            outbound.release();
        }
    }

    private void assertOutboundGeneralReply(int expectedFlowId, int expectedReplyForMsgId, int expectedResult) {
        ByteBuf outbound = channel.readOutbound();
        assertNotNull(outbound);
        try {
            var decoded = decodeOutboundFrame(outbound);
            assertEquals(MsgId.PLATFORM_GENERAL_REPLY, decoded.msgId);
            assertNotNull(decoded.body);
            assertEquals(5, decoded.body.length);
            int flowId = ((decoded.body[0] & 0xFF) << 8) | (decoded.body[1] & 0xFF);
            int replyForMsgId = ((decoded.body[2] & 0xFF) << 8) | (decoded.body[3] & 0xFF);
            int result = decoded.body[4] & 0xFF;
            assertEquals(expectedFlowId & 0xFFFF, flowId);
            assertEquals(expectedReplyForMsgId & 0xFFFF, replyForMsgId);
            assertEquals(expectedResult & 0xFF, result);
        } finally {
            outbound.release();
        }
    }

    private static DecodedDownlink decodeOutboundFrame(ByteBuf out) {
        int len = out.readableBytes();
        if (len < 2) {
            throw new IllegalArgumentException("outbound frame too short");
        }
        int start = out.readerIndex();
        int end = out.readerIndex() + len - 1;
        if ((out.getByte(start) & 0xFF) != Protocol.HEADER || (out.getByte(end) & 0xFF) != Protocol.TAIL) {
            throw new IllegalArgumentException("invalid frame delimiter");
        }

        byte[] unescaped = new byte[len];
        int w = 0;
        for (int i = start + 1; i < end; i++) {
            int b = out.getByte(i) & 0xFF;
            if (b == Protocol.ESCAPE && i + 1 < end) {
                int next = out.getByte(i + 1) & 0xFF;
                if (next == Protocol.ESCAPE_FOR_ESCAPE) {
                    unescaped[w++] = (byte) Protocol.ESCAPE;
                    i++;
                    continue;
                }
                if (next == Protocol.ESCAPE_FOR_HEADER) {
                    unescaped[w++] = (byte) Protocol.HEADER;
                    i++;
                    continue;
                }
            }
            unescaped[w++] = (byte) b;
        }
        if (w < 1) {
            throw new IllegalArgumentException("invalid escaped content");
        }
        int payloadLen = w - 1;
        byte receivedBcc = unescaped[w - 1];
        byte calculatedBcc = 0;
        for (int i = 0; i < payloadLen; i++) {
            calculatedBcc ^= unescaped[i];
        }
        if (calculatedBcc != receivedBcc) {
            throw new IllegalArgumentException("bcc mismatch");
        }

        if (payloadLen < 12) {
            throw new IllegalArgumentException("payload too short");
        }

        int msgId = ((unescaped[0] & 0xFF) << 8) | (unescaped[1] & 0xFF);
        int bodyLen = ((unescaped[2] & 0x03) << 8) | (unescaped[3] & 0xFF);
        int headerLen = 2 + 2 + 6 + 2;
        int bodyStart = headerLen;
        int bodyEnd = Math.min(payloadLen, bodyStart + Math.max(0, bodyLen));
        byte[] body = null;
        if (bodyEnd > bodyStart) {
            body = new byte[bodyEnd - bodyStart];
            System.arraycopy(unescaped, bodyStart, body, 0, body.length);
        }
        return new DecodedDownlink(msgId, body);
    }

    private static final class DecodedDownlink {
        private final int msgId;
        private final byte[] body;

        private DecodedDownlink(int msgId, byte[] body) {
            this.msgId = msgId;
            this.body = body;
        }
    }
}
