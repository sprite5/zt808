package cn.jascript.zt808.boot;

import cn.jascript.zt808.config.AppConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {
    public static void main(String[] args) {
        var serverConfig = AppConfig.get().getServer();
        StartupValidator.validateOrThrow();
        log.info("start jt808 server, port={}", serverConfig.getPort());
        Bootstrap.getServer().start(serverConfig.getPort(), true);
    }
}