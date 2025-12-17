package cn.jascript.zt808.model.dto;

import cn.jascript.zt808.constants.DataType;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class RegisterDTO implements BaseDTO {
    private String terminalId;
    //厂商id
    private String manufacturerId;
    //设备型号
    private String deviceModel;
    private Date receiveTime;
    private DataType dataType = DataType.REGIST;
    private Map<String, String> ext = new HashMap<>();
}
