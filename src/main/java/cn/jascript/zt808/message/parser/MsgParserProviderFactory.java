package cn.jascript.zt808.message.parser;

import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.constants.MsgReplyMode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * msgId -> MsgProvider 工厂（按配置反射加载并缓存）。
 */
@Slf4j
public class MsgParserProviderFactory {

    private static final MsgParserProviderFactory INSTANCE = new MsgParserProviderFactory();

    private final Map<Integer, MsgParserProvider> cache = new ConcurrentHashMap<>();
    private final Map<String, String> configMap;
    private final Map<String, String> providerByMsgId;
    private final Map<String, MsgReplyMode> replyModeByMsgId;

    private MsgParserProviderFactory() {
        // key: "0x0200" or "512" (string)
        this.configMap = AppConfig.get().getFlatValuesByPrefix("message.parser.provider");
        this.providerByMsgId = buildProviderByMsgId(configMap);
        this.replyModeByMsgId = buildReplyModeByMsgId(configMap);
        if (!configMap.isEmpty()) {
            log.info("msgParserProvider mappings loaded, size={}", configMap.size());
        }
    }

    public static MsgParserProviderFactory getInstance() {
        return INSTANCE;
    }

    public MsgReplyMode getReplyMode(int msgId) {
        var v = replyModeByMsgId.get(toHexKey(msgId));
        if (Objects.nonNull(v)) 
            return v;
        v = replyModeByMsgId.get(String.valueOf(msgId));
        if (Objects.nonNull(v)) 
            return v;
        return MsgReplyMode.GENERAL;
    }

    public Optional<MsgParserProvider> getProvider(int msgId) {
        var key = msgId;
        var cached = cache.get(key);
        if (Objects.nonNull(cached)) {
            return Optional.of(cached);
        }
        //从配置里获取全类名
        var fqcn = providerByMsgId.get(toHexKey(msgId));
        if (Objects.isNull(fqcn))
            fqcn = providerByMsgId.get(String.valueOf(msgId));
        if (Objects.isNull(fqcn))
            return Optional.empty();
        Objects.nonNull(fqcn);
        try {
            var clazz = Class.forName(fqcn);
            var instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof MsgParserProvider) {
                MsgParserProvider provider = (MsgParserProvider) instance;
                cache.put(key, provider);
                log.info("msgParserProvider loaded, msgId={}, class={}", toHexKey(msgId), fqcn);
                return Optional.of(provider);
            }
            log.error("msgParserProvider class {} does not implement MsgParserProvider, msgId={}", fqcn, toHexKey(msgId));
        } catch (Exception e) {
            log.error("load msgParserProvider failed, msgId={}, class={}", toHexKey(msgId), fqcn, e);
        }
        return Optional.empty();
    }

    private String toHexKey(int msgId) {
        return String.format("0x%04X", msgId);
    }

    private static Map<String, String> buildProviderByMsgId(Map<String, String> flat) {
        if (Objects.isNull(flat) || flat.isEmpty()) 
            return Map.of();

        // 1) 兼容旧格式：message.provider.<msgId> = fqcn
        var result = new HashMap<String, String>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            if (!k.contains("[")) {
                result.put(k, e.getValue());
            }
        }

        // 2) 新格式：message.provider[i].msgId / message.provider[i].provider
        var idxToMsgId = new HashMap<String, String>();
        var idxToProvider = new HashMap<String, String>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            var idx = extractIndexKey(k);
            if (Objects.isNull(idx)) 
                continue;
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

    private static Map<String, MsgReplyMode> buildReplyModeByMsgId(Map<String, String> flat) {
        if (Objects.isNull(flat) || flat.isEmpty()) 
            return Map.of();

        // 1) 兼容旧格式：message.parser.provider.<msgId>.replyMode = "general|none|provider"
        var result = new HashMap<String, MsgReplyMode>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            if (k.contains("["))
                continue;
            if (!k.endsWith(".replyMode"))
                continue;
            var msgIdKey = k.substring(0, k.length() - ".replyMode".length());
            var mode = parseReplyMode(stripQuotes(e.getValue()));
            if (Objects.nonNull(mode)) {
                result.put(msgIdKey, mode);
            }
        }

        // 2) 新格式：message.parser.provider[i].msgId / message.parser.provider[i].replyMode
        var idxToMsgId = new HashMap<String, String>();
        var idxToReplyMode = new HashMap<String, String>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            var idx = extractIndexKey(k);
            if (Objects.isNull(idx)) 
                continue;
            if (k.endsWith(".msgId")) {
                idxToMsgId.put(idx, stripQuotes(v));
            } else if (k.endsWith(".replyMode")) {
                idxToReplyMode.put(idx, stripQuotes(v));
            }
        }
        for (var idx : idxToMsgId.keySet()) {
            var msgId = idxToMsgId.get(idx);
            var replyMode = idxToReplyMode.get(idx);
            if (Objects.isNull(msgId) || Objects.isNull(replyMode))
                continue;
            var mode = parseReplyMode(replyMode);
            if (Objects.nonNull(mode)) {
                result.put(msgId, mode);
            }
        }
        return result;
    }

    private static MsgReplyMode parseReplyMode(String v) {
        if (Objects.isNull(v)) 
            return null;
        var normalized = v.trim();
        if (normalized.isEmpty()) 
            return null;
        normalized = normalized.toUpperCase();
        try {
            return MsgReplyMode.valueOf(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static String stripQuotes(String s) {
        if (Objects.isNull(s)) 
            return null;
        return s.replace("\"", "");
    }

    // 输入：provider[0].msgId / provider[0].provider / provider[0].name
    // 输出：provider[0]
    private static String extractIndexKey(String key) {
        if (Objects.isNull(key)) 
            return null;
        var lb = key.indexOf('[');
        var rb = key.indexOf(']');
        if (lb < 0 || rb < 0 || rb <= lb)
            return null;
        var dot = key.indexOf('.', rb);
        if (dot < 0)
            return null;
        return key.substring(0, dot);
    }
}
