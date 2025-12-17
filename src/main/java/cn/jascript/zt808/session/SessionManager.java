package cn.jascript.zt808.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理终端会话：注册、鉴权标记、心跳刷新与统计。
 */
@Slf4j
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Map<String, Session> terminalSessions = new ConcurrentHashMap<>();
    private final Map<String, Session> channelSessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public Session register(Channel channel) {
        var channelId = channel.id().asLongText();
        var session = Session.builder()
                .channel(channel)
                .remoteAddress(channel.remoteAddress())
                .channelId(channelId)
                .authorized(false)
                .lastActiveTime(new Date())
                .build();
        channelSessions.put(channelId, session);
        log.info("channel registered, remote={}", session.getRemoteAddress());
        return session;
    }

    public void bindTerminal(String terminalId, Channel channel) {
        var session = findByChannel(channel).orElseGet(() -> register(channel));
        session.setTerminalId(terminalId);
        session.setLastActiveTime(new Date());
        terminalSessions.put(terminalId, session);
        log.info("terminal bound, terminalId={}, remote={}", terminalId, session.getRemoteAddress());
    }

    public void authorize(String terminalId, boolean authorized) {
        Optional.ofNullable(terminalSessions.get(terminalId)).ifPresent(session -> {
            session.setAuthorized(authorized);
            log.info("terminal auth update, terminalId={}, authorized={}", terminalId, authorized);
        });
    }

    public void refreshActiveTime(String terminalId) {
        if (Objects.isNull(terminalId) || terminalId.isEmpty())
            return;
        var sessionOpt =  Optional.ofNullable(terminalSessions.get(terminalId));
        sessionOpt.ifPresent(
                session -> session.setLastActiveTime(new Date())
        );
    }

    public void unregister(Channel channel) {
        var channelId = channel.id().asLongText();
        var session = channelSessions.remove(channelId);
        if (Objects.isNull(session)) {
            return;
        }
        var terminalId = session.getTerminalId();
        if (Objects.nonNull(terminalId)) {
            terminalSessions.remove(terminalId);
        }
        log.info("channel removed, terminalId={}, remote={}", terminalId, session.getRemoteAddress());
    }

    public Optional<Session> findByTerminal(String terminalId) {
        return Optional.ofNullable(terminalSessions.get(terminalId));
    }

    public Optional<Session> findByChannel(Channel channel) {
        return Optional.ofNullable(channelSessions.get(channel.id().asLongText()));
    }

    public int onlineCount() {
        return terminalSessions.size();
    }
}
