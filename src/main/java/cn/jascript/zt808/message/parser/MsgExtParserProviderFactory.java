package cn.jascript.zt808.message.parser;

import cn.jascript.zt808.config.AppConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * msgId -> MsgExtProvider 工厂（单个覆盖）。
 */
@Slf4j
public class MsgExtParserProviderFactory {

    private static final MsgExtParserProviderFactory INSTANCE = new MsgExtParserProviderFactory();

    private final Map<Integer, MsgExtParserProvider> cache = new ConcurrentHashMap<>();
    private final Map<String, String> configMap;
    private final Map<String, String> providerByMsgId;

    private MsgExtParserProviderFactory() {
        this.configMap = AppConfig.get().getFlatValuesByPrefix("message.parser.extProvider");
        this.providerByMsgId = buildProviderByMsgId(configMap);
        if (!configMap.isEmpty()) {
            log.info("msgExtParserProvider mappings loaded, size={}", configMap.size());
        }
    }

    public static MsgExtParserProviderFactory get() {
        return INSTANCE;
    }

    public Optional<MsgExtParserProvider> find(int msgId) {
        var key = msgId;
        var cached = cache.get(key);
        if (Objects.nonNull(cached)) {
            return Optional.of(cached);
        }

        var fqcn = providerByMsgId.get(toHexKey(msgId));
        if (Objects.isNull(fqcn)) {
            fqcn = providerByMsgId.get(String.valueOf(msgId));
        }
        if (Objects.isNull(fqcn)) {
            return Optional.empty();
        }

        try {
            var clazz = Class.forName(fqcn);
            var instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof MsgExtParserProvider) {
                MsgExtParserProvider provider = (MsgExtParserProvider) instance;
                cache.put(key, provider);
                log.info("msgExtParserProvider loaded, msgId={}, class={}", toHexKey(msgId), fqcn);
                return Optional.of(provider);
            }
            log.error("msgExtParserProvider class {} does not implement MsgExtParserProvider, msgId={}", fqcn, toHexKey(msgId));
        } catch (Exception e) {
            log.error("load msgExtParserProvider failed, msgId={}, class={}", toHexKey(msgId), fqcn, e);
        }
        return Optional.empty();
    }

    private String toHexKey(int msgId) {
        return String.format("0x%04X", msgId);
    }

    private static Map<String, String> buildProviderByMsgId(Map<String, String> flat) {
        if (Objects.isNull(flat) || flat.isEmpty()) {
            return Map.of();
        }

        // 1) 兼容旧格式：message.extProvider.<msgId> = fqcn
        var result = new HashMap<String, String>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            if (!k.contains("[")) {
                result.put(k, e.getValue());
            }
        }

        // 2) 新格式：message.extProvider[i].msgId / message.extProvider[i].provider
        var idxToMsgId = new HashMap<String, String>();
        var idxToProvider = new HashMap<String, String>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            var idx = extractIndexKey(k);
            if (Objects.isNull(idx)) {
                continue;
            }
            if (k.endsWith(".msgId")) {
                idxToMsgId.put(idx, stripQuotes(v));
            } else if (k.endsWith(".provider")) {
                idxToProvider.put(idx, stripQuotes(v));
            }
        }
        for (var idx : idxToMsgId.keySet()) {
            var msgId = idxToMsgId.get(idx);
            var provider = idxToProvider.get(idx);
            if (Objects.nonNull(msgId) && Objects.nonNull(provider)) {
                result.put(msgId, provider);
            }
        }
        return result;
    }

    private static String stripQuotes(String s) {
        if (Objects.isNull(s)) {
            return null;
        }
        return s.replace("\"", "");
    }

    private static String extractIndexKey(String key) {
        if (Objects.isNull(key)) {
            return null;
        }
        var lb = key.indexOf('[');
        var rb = key.indexOf(']');
        if (lb < 0 || rb < 0 || rb <= lb) {
            return null;
        }
        var dot = key.indexOf('.', rb);
        if (dot < 0) {
            return null;
        }
        return key.substring(0, dot);
    }
}
