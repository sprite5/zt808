package cn.jascript.zt808.model.dto;


import cn.jascript.zt808.constants.DataType;

import java.util.Date;
import java.util.Map;

public interface BaseDTO {
    //设备号
    String getTerminalId();


    //接收时间
    Date getReceiveTime();


    //消息类型
    DataType getDataType();

    //扩展信息
    Map<String, String> getExt();

}
