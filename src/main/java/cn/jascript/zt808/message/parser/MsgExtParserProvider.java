package cn.jascript.zt808.message.parser;

import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;

import java.util.List;

/**
 * msgId 扩展解析器：对主解析结果做二次加工（补字段、过滤、拆分/合并等）。
 */
public interface MsgExtParserProvider {

    void apply(TerminalMessage message, List<BaseDTO> dtos);
}
