package cn.jascript.zt808.forward;

import cn.jascript.zt808.model.dto.BaseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 默认转发实现：简单输出日志，具体分流交由上游实现。
 */
@Slf4j
public class DefaultForwardProvider implements ForwardProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void forward(List<BaseDTO> dtos) {
        if (Objects.isNull(dtos) || dtos.isEmpty())
            return;

        String dtosJson;
        try {
            dtosJson = OBJECT_MAPPER.writeValueAsString(dtos);
        } catch (Exception e) {
            dtosJson = String.valueOf(dtos);
        }

        log.info("forward dtos, dtoSize={}, dtos={}", dtos.size(), dtosJson);
    }
}
