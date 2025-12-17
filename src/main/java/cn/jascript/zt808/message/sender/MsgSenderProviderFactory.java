package cn.jascript.zt808.message.sender;

import cn.jascript.zt808.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * MsgSenderProvider 工厂：从配置加载自定义实现，缺省使用 DefaultPlatformMsgSenderProvider。
 */
@Slf4j
public class MsgSenderProviderFactory {

    private static final MsgSenderProvider INSTANCE = init();

    private MsgSenderProviderFactory() {
    }

    public static MsgSenderProvider get() {
        return INSTANCE;
    }

    private static MsgSenderProvider init() {
        var providerFqcn = AppConfig.get().getString("message.sender.provider", "");
        if (StringUtils.isBlank(providerFqcn)) {
            log.info("message sender provider not configured, use DefaultPlatformMsgSenderProvider");
            return new DefaultPlatformMsgSenderProvider();
        }
        try {
            var clazz = Class.forName(providerFqcn);
            var instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof MsgSenderProvider) {
                MsgSenderProvider senderProvider = (MsgSenderProvider) instance;
                log.info("message sender provider loaded: {}", providerFqcn);
                return senderProvider;
            }
            log.error("message sender provider {} does not implement MsgSenderProvider, fallback to default", providerFqcn);
        } catch (Exception e) {
            log.error("load message sender provider {} failed, fallback to default", providerFqcn, e);
        }
        return new DefaultPlatformMsgSenderProvider();
    }
}
