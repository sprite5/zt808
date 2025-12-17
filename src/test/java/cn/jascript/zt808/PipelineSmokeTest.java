package cn.jascript.zt808;

import cn.jascript.zt808.codec.DecoderFactory;
import cn.jascript.zt808.codec.EncoderFactory;
import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.handler.DataEventHandler;
import cn.jascript.zt808.testkit.JT808TestKit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class PipelineSmokeTest {

    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(channel)) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testPipeline_noErrorsForTestMdHex() {
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
        writeInboundAndAssertNoError(JT808TestKit.authFrame(terminalId, 0x00C9, authCode));
        writeInboundAndAssertNoError(JT808TestKit.heartbeatFrame(terminalId, 0x00CA));
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
        writeInboundAndAssertNoError(JT808TestKit.heartbeatFrame(terminalId, 0x00D7));
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
    }

    private void writeInboundAndAssertNoError(byte[] bytes) {
        assertDoesNotThrow(() -> {
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            channel.writeInbound(buf);
            channel.runPendingTasks();
            channel.checkException();
        });
    }
}
