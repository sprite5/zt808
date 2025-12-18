package cn.jascript.zt808.message.parser.provider;

import cn.jascript.zt808.constants.AlarmType;
import cn.jascript.zt808.constants.StandardAlarmType;
import cn.jascript.zt808.message.parser.MsgParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.LocationDTO;
import cn.jascript.zt808.util.BcdUtil;
import cn.jascript.zt808.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class LocationMsgParserProvider implements MsgParserProvider {

    private static final int LOCATION_MIN_LEN = 28;
    
    private static final int TIME_LEN = 6;
    private static final double LAT_LON_FACTOR = 1_000_000.0;
    
    // Status Bit Positions
    private static final int BIT_ACC = 0;
    private static final int BIT_LOCATED = 1;

    // Alarm Bit Positions (JT/T 808-2019)
    private static final int BIT_SOS = 0;
    private static final int BIT_OVERSPEED = 1;
    private static final int BIT_FATIGUE = 2;
    private static final int BIT_DANGER_WARNING = 3;
    private static final int BIT_GNSS_FAULT = 4;
    private static final int BIT_GNSS_DISCONNECT = 5;
    private static final int BIT_GNSS_SHORT = 6;
    private static final int BIT_MAIN_POWER_UNDER = 7;
    private static final int BIT_MAIN_POWER_CUT = 8;
    private static final int BIT_LCD_FAULT = 9;
    private static final int BIT_TTS_FAULT = 10;
    private static final int BIT_CAMERA_FAULT = 11;
    private static final int BIT_IC_CARD_FAULT = 12;
    private static final int BIT_SPEED_WARNING = 13;
    private static final int BIT_FATIGUE_WARNING = 14;

    @Override
    public List<BaseDTO> parse(Channel channel, TerminalMessage message) {
        if (Objects.isNull(message))
            return List.of();
        var body = message.getBody();
        if (Objects.isNull(body))
            return List.of();

        var terminalId = message.getTerminalId();

        var dto = parseSingleLocation(body.duplicate(), terminalId);
        dto.setReceiveTime(new Date());
        return List.of(dto);
    }

    protected LocationDTO parseSingleLocation(ByteBuf buf, String terminalId) {
        var dto = new LocationDTO();
        dto.setTerminalId(terminalId);
        if (buf.readableBytes() < LOCATION_MIN_LEN)
            return dto;

        int alarm = buf.readInt();
        dto.getExt().put("alarmHex", String.format("%08X", alarm));
        dto.setAlarmSet(parseAlarm(alarm));

        var statusBytes = new byte[4];
        buf.readBytes(statusBytes);
        int status = CodecUtil.toInt(statusBytes);
        dto.getExt().put("statusHex", CodecUtil.bytesToHex(statusBytes));

        int lat = buf.readInt();
        int lon = buf.readInt();
        dto.setLatitude(lat / LAT_LON_FACTOR);
        dto.setLongitude(lon / LAT_LON_FACTOR);

        dto.setHeight((int) buf.readUnsignedShort());
        dto.setSpeed((int) buf.readUnsignedShort());
        dto.setDirection((int) buf.readUnsignedShort());

        var timeBytes = new byte[TIME_LEN];
        buf.readBytes(timeBytes);
        dto.setLocationTime(parseBcdTime(timeBytes));

        dto.setAccState(CodecUtil.checkBit(status, BIT_ACC));
        dto.setLocated(CodecUtil.checkBit(status, BIT_LOCATED));

        if (buf.isReadable()) {
            var extra = new byte[buf.readableBytes()];
            buf.readBytes(extra);
            dto.getExt().put("extraHex", CodecUtil.bytesToHex(extra));
        }

        return dto;
    }
    //默认对标准协议的报警处理,特殊需求可以走ext处理
    private Set<AlarmType> parseAlarm(int alarm) {
        var set = EnumSet.noneOf(StandardAlarmType.class);
        if (CodecUtil.checkBit(alarm, BIT_SOS)) 
            set.add(StandardAlarmType.SOS);
        if (CodecUtil.checkBit(alarm, BIT_OVERSPEED)) 
            set.add(StandardAlarmType.OVERSPEED);
        if (CodecUtil.checkBit(alarm, BIT_FATIGUE)) 
            set.add(StandardAlarmType.FATIGUE);
        if (CodecUtil.checkBit(alarm, BIT_GNSS_FAULT)) 
            set.add(StandardAlarmType.GNSS_FAULT);
        if (CodecUtil.checkBit(alarm, BIT_GNSS_SHORT)) 
            set.add(StandardAlarmType.GNSS_SHORT);
        if (CodecUtil.checkBit(alarm, BIT_GNSS_DISCONNECT)) 
            set.add(StandardAlarmType.GNSS_DISCONNECT);
        if (CodecUtil.checkBit(alarm, BIT_MAIN_POWER_CUT)) 
            set.add(StandardAlarmType.MAIN_POWER_CUT);
        if (CodecUtil.checkBit(alarm, BIT_MAIN_POWER_UNDER)) 
            set.add(StandardAlarmType.MAIN_POWER_UNDER);
        if (CodecUtil.checkBit(alarm, BIT_LCD_FAULT)) 
            set.add(StandardAlarmType.LCD_FAULT);
        if (CodecUtil.checkBit(alarm, BIT_TTS_FAULT)) 
            set.add(StandardAlarmType.TTS_FAULT);
        if (CodecUtil.checkBit(alarm, BIT_CAMERA_FAULT)) 
            set.add(StandardAlarmType.CAMERA_FAULT);
        
        // Extended bits
        // if (CodecUtil.checkBit(alarm, BIT_DANGER_WARNING)) ...
        if (CodecUtil.checkBit(alarm, BIT_SPEED_WARNING)) 
            set.add(StandardAlarmType.SPEED_WARNING);
        if (CodecUtil.checkBit(alarm, BIT_FATIGUE_WARNING)) 
            set.add(StandardAlarmType.FATIGUE_WARNING);
        return new HashSet<>(set);
    }

    private Date parseBcdTime(byte[] bytes) {
        if (Objects.isNull(bytes) || bytes.length < 6)
            return null;
        int year = BcdUtil.bcdToInt(bytes[0]) + 2000;
        int month = BcdUtil.bcdToInt(bytes[1]);
        int day = BcdUtil.bcdToInt(bytes[2]);
        int hour = BcdUtil.bcdToInt(bytes[3]);
        int minute = BcdUtil.bcdToInt(bytes[4]);
        int second = BcdUtil.bcdToInt(bytes[5]);
        try {
            var ldt = LocalDateTime.of(year, month, day, hour, minute, second);
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }
}
