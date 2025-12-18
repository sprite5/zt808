package cn.jascript.zt808.model.dto;

import cn.jascript.zt808.constants.AlarmType;
import cn.jascript.zt808.constants.DataType;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class LocationDTO implements BaseDTO {
    private String terminalId;
    private Date receiveTime;
    private DataType dataType = DataType.LOCATION;

    //定位时间
    private Date locationTime;

    //经度
    private Double longitude;

    //纬度
    private Double latitude;

    private Map<String, String> ext = new HashMap<>();

    //点火信息
    private Boolean accState;

    //是否定位
    private Boolean located;

    //速度,单位: 1/10 km/h，-1表示未知
    private Integer speed = -1;

    //gps星数,-1表示未知
    private Integer gpsSignal = -1;

    //网络信号,-1表示未知
    private Integer networkSignal = -1;

    //高度,-1表示未知
    private Integer height = -1;

    //方向,度 0 表示正北 ，取值,-1表示未知
    private Integer direction = -1;

    //携带报警数据
    private Set<AlarmType> alarmSet = Set.of();
}