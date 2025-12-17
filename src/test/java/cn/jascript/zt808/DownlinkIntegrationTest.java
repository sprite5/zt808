package cn.jascript.zt808;

import cn.jascript.zt808.codec.DecoderFactory;
import cn.jascript.zt808.codec.EncoderFactory;
import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.constants.Protocol;
import cn.jascript.zt808.handler.DataEventHandler;
import cn.jascript.zt808.message.sender.DefaultPlatformMsgSenderProvider;
import cn.jascript.zt808.session.FlowIdGenerator;
import cn.jascript.zt808.session.SessionManager;
import cn.jascript.zt808.session.SessionRouter;
import cn.jascript.zt808.testkit.JT808TestKit;
import cn.jascript.zt808.util.BcdUtil;
import cn.jascript.zt808.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class DownlinkIntegrationTest {

    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(channel)) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testRegisterAuthThenDownlink8300() {
        channel = new EmbeddedChannel(
                DecoderFactory.getDelimiterDecoder(),
                DecoderFactory.getEscapeDecoder(),
                DecoderFactory.getBccValidDecoder(),
                DecoderFactory.getMsgDecoder(),
                new DataEventHandler(),
                EncoderFactory.getMsgEncoder()
        );

        // register + auth
        var terminalId = "14540756282";
        var registerFrame = JT808TestKit.registerFrame(
                terminalId,
                0x00C8,
                0x002C,
                0x012F,
                "70111",
                "KM-02R",
                "0000000"
        );
        writeInboundAndAssertNoError(registerFrame);
        var authCode = AppConfig.get().getAuth().getCode();
        var authFrame = JT808TestKit.authFrame(terminalId, 0x00C9, authCode);
        writeInboundAndAssertNoError(authFrame);

        assertTrue(SessionManager.getInstance().findByTerminal(terminalId).isPresent());

        // route channel and generate flowId
        var router = new SessionRouter();
        Channel ch = router.route(terminalId).orElseThrow();
        var flowIdGenerator = new FlowIdGenerator(router);
        int flowId = flowIdGenerator.nextFlowId(terminalId);

        // build 0x8300 body: flag(1) + text(GBK)
        byte flag = 0x00;
        byte[] textBytes = "CQ".getBytes(Charset.forName("GBK"));
        byte[] body = new byte[1 + textBytes.length];
        body[0] = flag;
        System.arraycopy(textBytes, 0, body, 1, textBytes.length);

        // send via provider
        var provider = new DefaultPlatformMsgSenderProvider();
        boolean ok = provider.sendMessage(ch, terminalId, flowId, body, null);
        assertTrue(ok);

        // drain outbound until we find msgId=0x8300
        ByteBuf outbound;
        byte[] encoded8300 = null;
        while (Objects.nonNull(outbound = channel.readOutbound())) {
            try {
                byte[] bytes = new byte[outbound.readableBytes()];
                outbound.getBytes(outbound.readerIndex(), bytes);
                if (tryReadMsgId(bytes) == 0x8300) {
                    encoded8300 = bytes;
                    break;
                }
            } finally {
                outbound.release();
            }
        }

        assertNotNull(encoded8300);
        assertEquals((byte) 0x7E, encoded8300[0]);
        assertEquals((byte) 0x7E, encoded8300[encoded8300.length - 1]);

        byte[] raw = unescape(Arrays.copyOfRange(encoded8300, 1, encoded8300.length - 1));
        assertTrue(raw.length >= 1);

        byte bcc = raw[raw.length - 1];
        ByteBuf bccBuf = Unpooled.wrappedBuffer(raw, 0, raw.length - 1);
        try {
            byte calculated = CodecUtil.calculateBcc(bccBuf);
            assertEquals(calculated, bcc);
        } finally {
            bccBuf.release();
        }

        ByteBuf msgBuf = Unpooled.wrappedBuffer(raw, 0, raw.length - 1);
        try {
            int msgId = msgBuf.readUnsignedShort();
            assertEquals(0x8300, msgId);

            int attrs = msgBuf.readUnsignedShort();
            int bodyLen = attrs & 0x03FF;
            assertEquals(body.length, bodyLen);

            byte[] bcd = new byte[6];
            msgBuf.readBytes(bcd);
            String bcdStr = BcdUtil.toString(bcd);
            String expectedBcd = BcdUtil.leftPad(terminalId.replaceAll("\\D", ""), 12, '0');
            assertEquals(expectedBcd, bcdStr);

            int decodedFlowId = msgBuf.readUnsignedShort();
            assertEquals(flowId, decodedFlowId);

            byte[] decodedBody = new byte[bodyLen];
            msgBuf.readBytes(decodedBody);
            assertEquals(flag, decodedBody[0]);
            assertArrayEquals(textBytes, Arrays.copyOfRange(decodedBody, 1, decodedBody.length));
        } finally {
            msgBuf.release();
        }
    }

    private void writeInboundAndAssertNoError(byte[] bytes) {
        assertDoesNotThrow(() -> {
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            channel.writeInbound(buf);
            channel.runPendingTasks();
            channel.checkException();
        });
    }

    private static int tryReadMsgId(byte[] encoded) {
        if (Objects.isNull(encoded) || encoded.length < 2) {
            return -1;
        }
        if (encoded[0] != 0x7E || encoded[encoded.length - 1] != 0x7E) {
            return -1;
        }
        byte[] raw = unescape(Arrays.copyOfRange(encoded, 1, encoded.length - 1));
        if (raw.length < 2) {
            return -1;
        }
        return ((raw[0] & 0xFF) << 8) | (raw[1] & 0xFF);
    }

    private static byte[] unescape(byte[] escaped) {
        if (Objects.isNull(escaped) || escaped.length == 0) {
            return new byte[0];
        }
        byte[] tmp = new byte[escaped.length];
        int j = 0;
        for (int i = 0; i < escaped.length; i++) {
            byte b = escaped[i];
            if (b == Protocol.ESCAPE && i + 1 < escaped.length) {
                byte next = escaped[++i];
                if (next == Protocol.ESCAPE_FOR_HEADER) {
                    tmp[j++] = Protocol.HEADER;
                } else if (next == Protocol.ESCAPE_FOR_ESCAPE) {
                    tmp[j++] = Protocol.ESCAPE;
                } else {
                    tmp[j++] = b;
                    tmp[j++] = next;
                }
            } else {
                tmp[j++] = b;
            }
        }
        return Arrays.copyOf(tmp, j);
    }
}
