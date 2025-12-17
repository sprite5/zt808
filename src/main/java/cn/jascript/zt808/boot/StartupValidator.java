package cn.jascript.zt808.boot;

import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.message.parser.MsgExtParserProvider;
import cn.jascript.zt808.message.parser.MsgParserProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class StartupValidator {

    private StartupValidator() {
    }

    public static void validateOrThrow() {
        var config = AppConfig.get();

        var providerFlat = config.getFlatValuesByPrefix("message.parser.provider");
        var extProviderFlat = config.getFlatValuesByPrefix("message.parser.extProvider");
        String providerSection = "message.parser.provider";
        String extProviderSection = "message.parser.extProvider";

        var providerMap = normalizeProviderMap(providerFlat);
        var extProviderMap = normalizeProviderMap(extProviderFlat);

        var errors = new ArrayList<String>();

        // required msgId mappings
        requireProvider(providerMap, "0x0100", "register", errors);
        requireProvider(providerMap, "0x0102", "auth", errors);
        requireProvider(providerMap, "0x0002", "heartbeat", errors);

        validateProviderClasses(providerMap, MsgParserProvider.class, providerSection, errors);
        validateProviderClasses(extProviderMap, MsgExtParserProvider.class, extProviderSection, errors);

        if (!errors.isEmpty()) {
            for (var e : errors) {
                log.error("startup validate failed: {}", e);
            }
            throw new IllegalStateException("startup validate failed, errors=" + errors.size());
        }

        log.info("startup validate ok, providers={}, extProviders={}", providerMap.size(), extProviderMap.size());
    }

    private static void requireProvider(Map<String, String> map, String msgId, String name, ArrayList<String> errors) {
        if (!map.containsKey(msgId)) {
            errors.add("missing required provider mapping: " + msgId + " (" + name + ")");
        }
    }

    private static void validateProviderClasses(Map<String, String> map,
                                               Class<?> expectedType,
                                               String section,
                                               ArrayList<String> errors) {
        for (var entry : map.entrySet()) {
            var msgId = entry.getKey();
            var fqcn = entry.getValue();
            if (Objects.isNull(fqcn) || fqcn.isBlank()) {
                errors.add(section + "." + msgId + " class is blank");
                continue;
            }
            try {
                var clazz = Class.forName(fqcn);
                if (!expectedType.isAssignableFrom(clazz)) {
                    errors.add(section + "." + msgId + " class not instance of " + expectedType.getSimpleName() + ": " + fqcn);
                    continue;
                }
                clazz.getDeclaredConstructor();
            } catch (ClassNotFoundException e) {
                errors.add(section + "." + msgId + " class not found: " + fqcn);
            } catch (NoSuchMethodException e) {
                errors.add(section + "." + msgId + " missing no-arg constructor: " + fqcn);
            } catch (Exception e) {
                errors.add(section + "." + msgId + " validate error: " + fqcn + ", err=" + e.getClass().getSimpleName());
            }
        }
    }

    private static Map<String, String> normalizeProviderMap(Map<String, String> flat) {
        if (Objects.isNull(flat) || flat.isEmpty())
            return Map.of();

        // 1) legacy map format
        var result = new HashMap<String, String>();
        for (var e : flat.entrySet()) {
            var k = e.getKey();
            if (!k.contains("[")) {
                result.put(k, e.getValue());
            }
        }

        // 2) array format: <idx>.msgId + <idx>.provider
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

    private static String stripQuotes(String s) {
        if (Objects.isNull(s))
            return null;
        return s.replace("\"", "");
    }

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
