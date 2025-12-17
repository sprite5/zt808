package cn.jascript.zt808.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 轻量配置装载，仅解析本地 application.yaml 中与 auth/forward 相关的字段。
 */
@Slf4j
public class AppConfig {

    private static final AppConfig INSTANCE = load();

    private final Map<String, String> flatValues;
    private final Map<String, Set<String>> flatLists;

    @Getter
    private final ServerConfig server;
    @Getter
    private final AuthConfig auth;
    @Getter
    private final ForwardConfig forward;
    @Getter
    private final ParserConfig parser;
    @Getter
    private final DuplicateConfig duplicate;
    @Getter
    private final BusinessExecutorConfig businessExecutor;

    private AppConfig(Map<String, String> flatValues,
                      Map<String, Set<String>> flatLists,
                      ServerConfig server,
                      AuthConfig auth,
                      ForwardConfig forward,
                      ParserConfig parser,
                      DuplicateConfig duplicate,
                      BusinessExecutorConfig businessExecutor) {
        this.flatValues = flatValues;
        this.flatLists = flatLists;
        this.server = server;
        this.auth = auth;
        this.forward = forward;
        this.parser = parser;
        this.duplicate = duplicate;
        this.businessExecutor = businessExecutor;
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    private static AppConfig load() {
        var defaultServer = new ServerConfig(6808, 360, 50000);
        var defaultAuth = new AuthConfig("123456", Collections.emptySet());
        var defaultForward = new ForwardConfig("");
        var defaultParser = new ParserConfig("", "");
        var defaultDuplicate = new DuplicateConfig(true, 60, 200000);
        var defaultBusinessExecutor = new BusinessExecutorConfig(0);

        var flatValues = new ConcurrentHashMap<String, String>();
        var flatLists = new ConcurrentHashMap<String, Set<String>>();

        var resource = AppConfig.class.getResourceAsStream("/application.yaml");
        if (Objects.isNull(resource)) {
            log.warn("application.yaml not found, use defaults");
            return new AppConfig(flatValues, flatLists, defaultServer, defaultAuth, defaultForward, defaultParser, defaultDuplicate, defaultBusinessExecutor);
        }

        var server = new ServerConfig(defaultServer.getPort(), defaultServer.getIdleSeconds(), defaultServer.getMaxConnections());
        var auth = new AuthConfig(defaultAuth.getCode(), defaultAuth.getBlackList());
        var forward = new ForwardConfig(defaultForward.getProvider());
        var parser = new ParserConfig(defaultParser.getRegister(), defaultParser.getLocationStatusAndExtension());
        var duplicate = new DuplicateConfig(defaultDuplicate.isEnable(), defaultDuplicate.getTtlSeconds(), defaultDuplicate.getMaximumSize());
        var businessExecutor = new BusinessExecutorConfig(defaultBusinessExecutor.getThreads());

        try {
            var mapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = mapper.readTree(resource);

            flattenNode(flatValues, flatLists, "", root);

            var serverNode = root.path("server");
            server.setPort(serverNode.path("port").asInt(server.getPort()));
            server.setIdleSeconds(serverNode.path("idleSeconds").asInt(server.getIdleSeconds()));
            server.setMaxConnections(serverNode.path("maxConnections").asInt(server.getMaxConnections()));

            var authNode = root.path("auth");
            if (!authNode.path("code").isMissingNode() && !authNode.path("code").isNull()) {
                auth.setCode(authNode.path("code").asText(auth.getCode()));
            }
            var blackListNode = authNode.path("blackList");
            if (blackListNode.isArray()) {
                auth.setBlackList(normalizeTerminalIdSet(toStringSet(blackListNode)));
            }

            var forwardNode = root.path("forward");
            if (!forwardNode.path("provider").isMissingNode() && !forwardNode.path("provider").isNull()) {
                forward.setProvider(forwardNode.path("provider").asText(forward.getProvider()));
            }

            var parserNode = root.path("parser");
            if (!parserNode.path("register").isMissingNode() && !parserNode.path("register").isNull()) {
                parser.setRegister(parserNode.path("register").asText(parser.getRegister()));
            }
            if (!parserNode.path("locationStatusAndExtension").isMissingNode() && !parserNode.path("locationStatusAndExtension").isNull()) {
                parser.setLocationStatusAndExtension(parserNode.path("locationStatusAndExtension").asText(parser.getLocationStatusAndExtension()));
            }

            var duplicateNode = root.path("duplicate");
            if (!duplicateNode.path("enable").isMissingNode() && !duplicateNode.path("enable").isNull()) {
                duplicate.setEnable(duplicateNode.path("enable").asBoolean(duplicate.isEnable()));
            }
            if (!duplicateNode.path("ttlSeconds").isMissingNode() && !duplicateNode.path("ttlSeconds").isNull()) {
                duplicate.setTtlSeconds(duplicateNode.path("ttlSeconds").asInt(duplicate.getTtlSeconds()));
            }
            if (!duplicateNode.path("maximumSize").isMissingNode() && !duplicateNode.path("maximumSize").isNull()) {
                duplicate.setMaximumSize(duplicateNode.path("maximumSize").asLong(duplicate.getMaximumSize()));
            }

            var bizNode = root.path("businessExecutor");
            if (!bizNode.path("threads").isMissingNode() && !bizNode.path("threads").isNull()) {
                businessExecutor.setThreads(bizNode.path("threads").asInt(businessExecutor.getThreads()));
            }
        } catch (Exception e) {
            log.error("load application.yaml failed, use defaults", e);
        }
        return new AppConfig(flatValues, flatLists, server, auth, forward, parser, duplicate, businessExecutor);
    }

    private static Set<String> normalizeTerminalIdSet(Set<String> ids) {
        if (Objects.isNull(ids) || ids.isEmpty())
            return Collections.emptySet();
        var result = new HashSet<String>();
        for (var id : ids) {
            if (StringUtils.isBlank(id))
                continue;
            var normalized = id.replaceAll("\\D", "");
            if (StringUtils.isBlank(normalized))
                continue;
            // align with terminalId normalization: strip all leading zeros, but keep all-zero value unchanged
            boolean allZero = true;
            for (int i = 0; i < normalized.length(); i++) {
                if (normalized.charAt(i) != '0') {
                    allZero = false;
                    break;
                }
            }
            if (!allZero) {
                int idx = 0;
                while (idx < normalized.length() && normalized.charAt(idx) == '0') {
                    idx++;
                }
                normalized = normalized.substring(idx);
            }
            result.add(normalized);
        }
        return result;
    }

    private static void flattenNode(Map<String, String> flatValues,
                                    Map<String, Set<String>> flatLists,
                                    String path,
                                    JsonNode node) {
        if (Objects.isNull(node) || node.isMissingNode() || node.isNull())
            return;

        if (node.isObject()) {
            var it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                var childPath = StringUtils.isBlank(path) ? e.getKey() : path + "." + e.getKey();
                flattenNode(flatValues, flatLists, childPath, e.getValue());
            }
            return;
        }

        if (node.isArray()) {
            boolean allScalar = true;
            for (var item : node) {
                if (Objects.isNull(item) || item.isNull())
                    continue;
                if (!item.isValueNode()) {
                    allScalar = false;
                    break;
                }
            }
            if (allScalar) {
                flatLists.put(path, toStringSet(node));
                return;
            }
            for (int i = 0; i < node.size(); i++) {
                var childPath = path + "[" + i + "]";
                flattenNode(flatValues, flatLists, childPath, node.get(i));
            }
            return;
        }

        if (node.isValueNode()) {
            flatValues.put(path, node.asText());
        }
    }

    private static Set<String> toStringSet(JsonNode arrayNode) {
        if (Objects.isNull(arrayNode) || !arrayNode.isArray())
            return Collections.emptySet();
        var set = new HashSet<String>();
        for (var item : arrayNode) {
            if (Objects.isNull(item) || item.isNull())
                continue;
            var v = item.asText();
            if (StringUtils.isNotBlank(v)) {
                set.add(v);
            }
        }
        return set;
    }

    public String getString(String path) {
        return flatValues.get(path);
    }

    public String getString(String path, String defaultValue) {
        var val = getString(path);
        return StringUtils.isBlank(val) ? defaultValue : val;
    }

    public Integer getInt(String path) {
        var val = getString(path);
        if (StringUtils.isBlank(val))
            return null;
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
    }

    public int getInt(String path, int defaultValue) {
        var v = getInt(path);
        return Objects.isNull(v) ? defaultValue : v;
    }

    public Boolean getBoolean(String path) {
        var val = getString(path);
        if (StringUtils.isBlank(val))
            return null;
        return Boolean.parseBoolean(val);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        var v = getBoolean(path);
        return Objects.isNull(v) ? defaultValue : v;
    }

    public Set<String> getList(String path) {
        return flatLists.getOrDefault(path, Collections.emptySet());
    }

    public Map<String, String> getFlatValuesByPrefix(String prefix) {
        if (StringUtils.isBlank(prefix))
            return Collections.emptyMap();
        return flatValues.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> {
                            var rest = e.getKey().substring(prefix.length());
                            if (rest.startsWith(".")) rest = rest.substring(1);
                            return rest;
                        },
                        Map.Entry::getValue
                ));
    }

    @Data
    public static class AuthConfig {
        private String code;
        private Set<String> blackList;

        public AuthConfig(String code, Set<String> blackList) {
            this.code = code;
            this.blackList = blackList;
        }
    }

    @Data
    public static class ForwardConfig {
        private String provider;

        public ForwardConfig(String provider) {
            this.provider = provider;
        }
    }

    @Data
    public static class ParserConfig {
        private String register;
        private String locationStatusAndExtension;

        public ParserConfig(String register, String locationStatusAndExtension) {
            this.register = register;
            this.locationStatusAndExtension = locationStatusAndExtension;
        }
    }

    @Data
    public static class DuplicateConfig {
        private boolean enable;
        private int ttlSeconds;
        private long maximumSize;

        public DuplicateConfig(boolean enable, int ttlSeconds, long maximumSize) {
            this.enable = enable;
            this.ttlSeconds = ttlSeconds;
            this.maximumSize = maximumSize;
        }
    }

    @Data
    public static class BusinessExecutorConfig {
        /**
         * 业务线程数，<=0 时按 2 * CPU 核心。
         */
        private int threads;

        public BusinessExecutorConfig(int threads) {
            this.threads = threads;
        }

        public int effectiveThreads() {
            return threads <= 0 ? Math.max(2, Runtime.getRuntime().availableProcessors() * 2) : threads;
        }
    }

    @Data
    public static class ServerConfig {
        private int port;
        private int idleSeconds;
        private int maxConnections;

        public ServerConfig(int port, int idleSeconds, int maxConnections) {
            this.port = port;
            this.idleSeconds = idleSeconds;
            this.maxConnections = maxConnections;
        }
    }
}
