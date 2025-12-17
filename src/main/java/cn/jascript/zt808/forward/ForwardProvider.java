package cn.jascript.zt808.forward;

import cn.jascript.zt808.model.dto.BaseDTO;

import java.util.List;

/**
 * 转发接口，所有业务事件统一通过此接口输出，默认实现记录日志。
 */
public interface ForwardProvider {

    /**
     * 结果转发，按 BaseDTO 携带的 dataType 由实现方自行分流。
     */
    void forward(List<BaseDTO> dtos);
}
