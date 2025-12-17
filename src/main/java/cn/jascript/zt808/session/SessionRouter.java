package cn.jascript.zt808.session;

import io.netty.channel.Channel;

import java.util.Objects;
import java.util.Optional;

public class SessionRouter {

    private final SessionManager sessionManager;

    public SessionRouter() {
        this(SessionManager.getInstance());
    }

    public SessionRouter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public Optional<Channel> route(String terminalId) {
        if (Objects.isNull(terminalId) || terminalId.isBlank())
            return Optional.empty();
        return sessionManager.findByTerminal(terminalId)
                .map(Session::getChannel)
                .filter(ch -> Objects.nonNull(ch) && ch.isActive());
    }

    public Optional<Session> routeSession(String terminalId) {
        if (Objects.isNull(terminalId) || terminalId.isBlank())
            return Optional.empty();
        return sessionManager.findByTerminal(terminalId);
    }
}
