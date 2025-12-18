package cn.jascript.zt808;

import cn.jascript.zt808.codec.DecoderFactory;
import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.message.parser.provider.BatchLocationMsgMsgParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.LocationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class BatchLocationReportTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(channel)) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testBatchLocationReport_realFrame() {
        channel = new EmbeddedChannel(
                DecoderFactory.getDelimiterDecoder(),
                DecoderFactory.getEscapeDecoder(),
                DecoderFactory.getBccValidDecoder(),
                DecoderFactory.getMsgDecoder()
        );
        //真实数据测试
        String terminalId = "14540357526";
        String frameHex = "7E0704012F014540357526002C000501003A0000000000000002020A73D9067D406101720000000025121703584601040001CA37EB16000C00B28986049401208031423900060089FFFFFFFE003A0000000000000002020A73D9067D406101720000000025121704034801040001CA37EB16000C00B28986049401208031423900060089FFFFFFFE003A0000000000000002020A73D9067D406101720000000025121704085001040001CA37EB16000C00B28986049401208031423900060089FFFFFFFE003A0000000000000002020A73D9067D406101720000000025121704135101040001CA37EB16000C00B28986049401208031423900060089FFFFFFFE003A0000000000000002020A73D9067D406101720000000025121704185301040001CA37EB16000C00B28986049401208031423900060089FFFFFFFEB37E";

        byte[] bytes = hexToBytes(frameHex);
        channel.writeInbound(Unpooled.wrappedBuffer(bytes));
        channel.runPendingTasks();
        channel.checkException();

        TerminalMessage message = channel.readInbound();
        assertNotNull(message);
        try {
            assertEquals(MsgId.BATCH_LOCATION, message.getMsgId());
            assertEquals(terminalId, message.getTerminalId());
            assertNull(channel.readInbound());

            var provider = new BatchLocationMsgMsgParserProvider();
            List<BaseDTO> dtos = provider.parse(channel, message);

            try {
                System.out.println("BatchLocationReportTest parsed dtos: " + OBJECT_MAPPER.writeValueAsString(dtos));
            } catch (Exception e) {
                System.out.println("BatchLocationReportTest parsed dtos: " + dtos);
            }

            assertNotNull(dtos);
            assertEquals(5, dtos.size());

            for (BaseDTO base : dtos) {
                assertTrue(base instanceof LocationDTO);
                LocationDTO dto = (LocationDTO) base;
                assertEquals(terminalId, dto.getTerminalId());
                assertEquals("1", dto.getExt().get("batchType"));

                assertNotNull(dto.getLocationTime());
                assertNotNull(dto.getLatitude());
                assertNotNull(dto.getLongitude());
                assertTrue(dto.getLatitude() >= -90 && dto.getLatitude() <= 90);
                assertTrue(dto.getLongitude() >= -180 && dto.getLongitude() <= 180);
            }
        } finally {
            ReferenceCountUtil.release(message.getBody());
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        String s = hex.replaceAll("\\s+", "");
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException("hex length must be even");
        }
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex character");
            }
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
