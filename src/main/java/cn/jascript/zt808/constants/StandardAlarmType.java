package cn.jascript.zt808.constants;

/**
 * 标准报警类型（基于 0x0200 报警标志常见位）。
 */
public enum StandardAlarmType implements AlarmType {
    SOS,                // 紧急报警
    OVERSPEED,          // 超速报警
    FATIGUE,            // 疲劳驾驶
    GNSS_FAULT,         // GNSS 模块故障
    GNSS_SHORT,         // GNSS 天线短路
    GNSS_DISCONNECT,    // GNSS 天线未接
    MAIN_POWER_CUT,     // 主电源掉电
    MAIN_POWER_UNDER,   // 主电源欠压
    LCD_FAULT,          // 液晶故障
    TTS_FAULT,          // TTS 故障
    CAMERA_FAULT,       // 摄像头故障
    SPEED_WARNING,      // 超速预警
    FATIGUE_WARNING,    // 疲劳驾驶预警
    OTHER               // 其他
}
