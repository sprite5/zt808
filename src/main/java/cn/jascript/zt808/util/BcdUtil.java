package cn.jascript.zt808.util;

import java.util.Arrays;
import java.util.Objects;

public class BcdUtil {
    // BCD 字节数组转为十六进制字符串（大写）
    public static String toString(byte[] bcd) {
        if (Objects.isNull(bcd) || bcd.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bcd) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // 将 BCD 字符串左侧补 0 的终端号去掉前导 0
    public static String replaceLeftZeroString(byte[] bcd) {
        var value = toString(bcd);
        if (Objects.isNull(value) || value.isEmpty()) {
            return "";
        }
        // JT/T 808 terminalId is left-padded with '0' to 12 digits when shorter.
        // Normalize by stripping ALL leading '0'. But if the value is all zeros, keep it.
        boolean allZero = true;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            return value;
        }
        int idx = 0;
        while (idx < value.length() && value.charAt(idx) == '0') {
            idx++;
        }
        return value.substring(idx);
    }

    // 十进制字符串转为指定长度的 BCD 字节数组（不足左补 0）
    public static byte[] fromString(String digits, int byteLength) {
        var normalized = Objects.isNull(digits) ? "" : digits.replaceAll("\\D", "");
        normalized = leftPad(normalized, byteLength * 2, '0');
        byte[] result = new byte[byteLength];
        for (int i = 0; i < byteLength; i++) {
            result[i] = (byte) Integer.parseInt(normalized.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    // 字符串左侧补齐到指定长度（超长则截取右侧）
    public static String leftPad(String value, int targetLength, char padChar) {
        if (value.length() >= targetLength) {
            return value.substring(value.length() - targetLength);
        }
        char[] chars = new char[targetLength];
        Arrays.fill(chars, padChar);
        int start = targetLength - value.length();
        System.arraycopy(value.toCharArray(), 0, chars, start, value.length());
        return new String(chars);
    }

    // 单个 BCD 字节转为十进制整数（00~99）
    public static int bcdToInt(byte value) {
        int high = (value >> 4) & 0x0F;
        int low = value & 0x0F;
        return high * 10 + low;
    }
}
