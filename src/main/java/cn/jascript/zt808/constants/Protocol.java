package cn.jascript.zt808.constants;

public class Protocol {
    public static final int HEADER = 0x7E;
    public static final int TAIL = HEADER;
    public static final int ESCAPE = 0x7D;
    public static final int ESCAPE_FOR_HEADER = 0x02;
    public static final int ESCAPE_FOR_ESCAPE = 0x01;
    public static final int MAX_FRAME_LENGTH = 1024;
    public static final int MAX_BODY_LENGTH = 0x3FF;


}
