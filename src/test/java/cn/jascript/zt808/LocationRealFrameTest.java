package cn.jascript.zt808;

import cn.jascript.zt808.codec.DecoderFactory;
import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.message.parser.extprovider.DefaultLocationExtParserProvider;
import cn.jascript.zt808.message.parser.provider.LocationMsgParserProvider;
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

public class LocationRealFrameTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(channel)) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testLocationReport_realFrame_shouldDecodeAndParseExt() {
        channel = new EmbeddedChannel(
                DecoderFactory.getDelimiterDecoder(),
                DecoderFactory.getEscapeDecoder(),
                DecoderFactory.getBccValidDecoder(),
                DecoderFactory.getMsgDecoder()
        );
        //真实数据测试
        String frameHex = "7E0200005C06180802758701F800000000000C004302630C1106BF05540417005001332512181148420104000081AB30011C31011D6102058CF7020000EB2A000C00B2898604D91623D122154900060089FFFFFFFE000400CE058C000600C5FFFFFFFF000400B71C1D647E";

        channel.writeInbound(Unpooled.wrappedBuffer(hexToBytes(frameHex)));
        channel.runPendingTasks();
        channel.checkException();

        TerminalMessage message = channel.readInbound();
        assertNotNull(message);
        try {
            assertEquals(MsgId.LOCATION, message.getMsgId());
            assertNull(channel.readInbound());

            var provider = new LocationMsgParserProvider();
            List<BaseDTO> dtos = provider.parse(channel, message);

            assertNotNull(dtos);
            assertEquals(1, dtos.size());
            assertTrue(dtos.get(0) instanceof LocationDTO);

            var dto = (LocationDTO) dtos.get(0);
            assertNotNull(dto.getTerminalId());
            assertNotNull(dto.getLatitude());
            assertNotNull(dto.getLongitude());
            assertNotNull(dto.getLocationTime());

            // apply ext parsing (0x01 mileage, 0x30 gpsSignal, 0x31 networkSignal)
            new DefaultLocationExtParserProvider().apply(message, dtos);

            try {
                System.out.println("LocationRealFrameTest parsed dto: " + OBJECT_MAPPER.writeValueAsString(dto));
            } catch (Exception e) {
                System.out.println("LocationRealFrameTest parsed dto: " + dto);
            }

            // located/accState are parsed from status bits
            assertNotNull(dto.getLocated());
            assertNotNull(dto.getAccState());

            // ext should include mileage when present in the real frame
            if (dto.getExt().containsKey("mileage")) {
                assertDoesNotThrow(() -> Long.parseLong(dto.getExt().get("mileage")));
                assertEquals("0.1km", dto.getExt().get("mileageUnit"));
            }

            // signals are optional (depending on frame extra items)
            assertNotNull(dto.getGpsSignal());
            assertNotNull(dto.getNetworkSignal());
            assertTrue(dto.getGpsSignal() >= -1);
            assertTrue(dto.getNetworkSignal() >= -1);
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
