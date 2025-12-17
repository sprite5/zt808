package cn.jascript.zt808.constants;

import java.util.List;

public final class MsgId {
    private MsgId() {
    }

    // terminal uplink
    public static final int HEARTBEAT = 0x0002;
    public static final int REGISTER = 0x0100;
    public static final int AUTH = 0x0102;
    public static final int LOCATION = 0x0200;
    public static final int BATCH_LOCATION = 0x0704;

    //未鉴权可发送的msgId,仅注册鉴权支持未鉴权发送
    public static final  List<Integer> ALLOW_UNAUTHED_MSGIDS = List.of(AUTH,REGISTER);

    // platform downlink
    public static final int PLATFORM_GENERAL_REPLY = 0x8001;
    public static final int REGISTER_REPLY = 0x8100;
    public static final int PUSH_TEXT = 0x8300;
}
