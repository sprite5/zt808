package cn.jascript.zt808.forward;

import cn.jascript.zt808.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * ForwardProvider 工厂：从配置加载自定义实现，缺省使用默认日志实现。
 */
@Slf4j
public class ForwardProviderFactory {

    private static final ForwardProvider INSTANCE = init();

    private ForwardProviderFactory() {
    }

    public static ForwardProvider get() {
        return INSTANCE;
    }

    private static ForwardProvider init() {
        var providerFqcn = AppConfig.get().getForward().getProvider();
        if (StringUtils.isBlank(providerFqcn)) {
            log.info("forward provider not configured, use DefaultForwardProvider");
            return new DefaultForwardProvider();
        }
        try {
            var clazz = Class.forName(providerFqcn);
            var instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof ForwardProvider) {
                ForwardProvider forwardProvider = (ForwardProvider) instance;
                log.info("forward provider loaded: {}", providerFqcn);
                return forwardProvider;
            }
            log.error("forward provider {} does not implement ForwardProvider, fallback to default", providerFqcn);
        } catch (Exception e) {
            log.error("load forward provider {} failed, fallback to default", providerFqcn, e);
        }
        return new DefaultForwardProvider();
    }
}
