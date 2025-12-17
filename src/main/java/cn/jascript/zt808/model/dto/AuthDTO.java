package cn.jascript.zt808.model.dto;

import cn.jascript.zt808.constants.DataType;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class AuthDTO implements BaseDTO {
    private String terminalId;
    private Date receiveTime;
    private DataType dataType = DataType.AUTH;
    private boolean success;
    private String reason;
    private Map<String, String> ext = new HashMap<>();
}
