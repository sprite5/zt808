package cn.jascript.zt808.handler;

import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.constants.MsgReplyMode;
import cn.jascript.zt808.forward.ForwardProviderFactory;
import cn.jascript.zt808.message.helper.ReplyHelper;
import cn.jascript.zt808.message.parser.MsgExtParserProviderFactory;
import cn.jascript.zt808.message.parser.MsgParserProviderFactory;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务分发：根据 MessageType 调用对应 Service。
 */
@Slf4j
public class DataEventHandler extends ChannelInboundHandlerAdapter {

    private final AppConfig.DuplicateConfig duplicateConfig = AppConfig.get().getDuplicate();
    private final DuplicateCache duplicateCache = new DuplicateCache(duplicateConfig.getMaximumSize(), duplicateConfig.getTtlSeconds());

    private final SessionManager sessionManager = SessionManager.getInstance();

    private final MsgParserProviderFactory msgParserProviderFactory = MsgParserProviderFactory.getInstance();
    private final MsgExtParserProviderFactory msgExtParserProviderFactory = MsgExtParserProviderFactory.get();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (Objects.isNull(msg) || !(msg instanceof TerminalMessage)) {
            ctx.fireChannelRead(msg);
            return;
        }
        TerminalMessage message = (TerminalMessage) msg;
        //配置MDC上下文
        setupMdc(ctx, message);
        try{
           doChannel(ctx,message);
        }finally {
            sessionManager.refreshActiveTime(message.getTerminalId());
            MDC.clear();
        }
    }

    //具体业务处理
    private void doChannel(ChannelHandlerContext ctx, TerminalMessage message){
        int msgId = message.getMsgId();
        var msgParserProviderOpt = msgParserProviderFactory.getProvider(message.getMsgId());
        //判断是否已鉴权
        boolean authorized = sessionManager.findByTerminal(message.getTerminalId())
                .map(s -> Boolean.TRUE.equals(s.isAuthorized()))
                .orElse(false);
        //默认使用能用回复
        var replyMode = MsgReplyMode.GENERAL;
        //如果没有配置解析代理,已鉴权回复通用成功回复,未鉴权回复能用失败回复
        if(msgParserProviderOpt.isEmpty()){
            log.debug("no msgProvider configured, replay GENERAL. terminalId={}, msgId={}, flowId={}",
                    message.getTerminalId(), message.getMsgId(), message.getFlowId());
            if(authorized) {
                ReplyHelper.sendGeneralReply(ctx.channel(), message, true);
            }else {
                ReplyHelper.sendGeneralReply(ctx.channel(), message, false);
            }
        }else {
            var msgParserProvider = msgParserProviderOpt.get();
            // 决定是否回复通用应答：由配置 message.parser.provider[*].replyMode 决定，未配置默认 GENERAL
            replyMode = msgParserProviderFactory.getReplyMode(msgId);

            // 未鉴权拦截：
            // - 默认仅允许注册(0x0100)/鉴权(0x0102)
            // - 若存在 Provider，可由 Provider 进一步放行（allowWhenUnauthorized=true）
            // - 被拦截时默认回平台通用应答 0x8001(result=1)
            boolean allowWhenUnauthorized = MsgId.ALLOW_UNAUTHED_MSGIDS.contains(msgId);
            if (!authorized && !allowWhenUnauthorized) {
                log.info("unauthorized message dropped, terminalId={}, msgId={}, flowId={}, reply=0x8001(result=1)",
                        message.getTerminalId(), message.getMsgId(), message.getFlowId());
                if (replyMode != MsgReplyMode.NONE) {
                    ReplyHelper.sendGeneralReply(ctx.channel(), message, false);
                }
                return;
            }
            if (Objects.equals(replyMode, MsgReplyMode.GENERAL)) {
                ReplyHelper.sendGeneralReply(ctx.channel(), message);
            }

            // 基于 terminalId + msgId + flowId 的短窗口排重：命中则只回包不处理
            if (duplicateConfig.isEnable()) {
                var key = buildDuplicateKey(message);
                if (duplicateCache.seen(key)) {
                    log.debug("dup message ignored, terminalId={}, msgId={}, flowId={}",
                            message.getTerminalId(), message.getMsgId(), message.getFlowId());
                    return;
                }
            }
            List<BaseDTO> dtos;

            try {
                dtos = msgParserProvider.parse(ctx.channel(), message);
            } catch (Exception e) {
                log.error("msgProvider parse failed, terminalId={}, msgId={}, flowId={}",
                        message.getTerminalId(), message.getMsgId(), message.getFlowId(), e);
                return;
            }
            if (Objects.isNull(dtos) || dtos.isEmpty())
                return;
            var extOpt = msgExtParserProviderFactory.find(message.getMsgId());
            if (extOpt.isPresent()) {
                try {
                    extOpt.get().apply(message, dtos);
                } catch (Exception e) {
                    log.error("msgExtProvider apply failed, terminalId={}, msgId={}, flowId={}",
                            message.getTerminalId(), message.getMsgId(), message.getFlowId(), e);
                    //主解析成功,扩展解处理时,数据仍然可用
                    //return;
                }
            }
            if (Objects.isNull(dtos) || dtos.isEmpty()) {
                return;
            }
            try {
                ForwardProviderFactory.get().forward(dtos);
            } catch (Exception e) {
                log.error("forward failed, terminalId={}, msgId={}, flowId={}, dtoSize={}",
                        message.getTerminalId(), message.getMsgId(), message.getFlowId(), dtos.size(), e);
            }
        }
    }

    private void setupMdc(ChannelHandlerContext ctx, TerminalMessage message) {
        if (Objects.nonNull(message.getTerminalId())) {
            MDC.put("terminalId", message.getTerminalId());
        }
        var addr = ctx.channel().remoteAddress();
        if (Objects.nonNull(addr) && addr instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) addr;
            MDC.put("Ip", inet.getHostString());
            MDC.put("Port", String.valueOf(inet.getPort()));
        }
    }

    private String buildDuplicateKey(TerminalMessage message) {
        return message.getTerminalId() + '-' + message.getMsgId() + '-' + message.getFlowId();
    }

    private static final class DuplicateCache {
        private final long maxSize;
        private final long ttlMillis;
        private final ConcurrentHashMap<String, Long> expireAtMillisByKey = new ConcurrentHashMap<>();

        private DuplicateCache(long maxSize, long ttlSeconds) {
            this.maxSize = Math.max(1, maxSize);
            this.ttlMillis = Math.max(1, ttlSeconds) * 1000L;
        }

        private boolean seen(String key) {
            if (Objects.isNull(key) || key.isEmpty()) {
                return false;
            }
            long now = System.currentTimeMillis();
            Long expireAt = expireAtMillisByKey.get(key);
            if (Objects.nonNull(expireAt)) {
                if (expireAt > now) {
                    return true;
                }
                expireAtMillisByKey.remove(key, expireAt);
            }
            expireAtMillisByKey.put(key, now + ttlMillis);
            cleanupIfNeeded(now);
            return false;
        }

        private void cleanupIfNeeded(long now) {
            if (expireAtMillisByKey.size() <= maxSize) {
                return;
            }
            for (var it = expireAtMillisByKey.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (e.getValue() <= now) {
                    it.remove();
                }
            }
            if (expireAtMillisByKey.size() <= maxSize) {
                return;
            }
            long overflow = expireAtMillisByKey.size() - maxSize;
            for (var it = expireAtMillisByKey.keySet().iterator(); it.hasNext() && overflow > 0; ) {
                it.next();
                it.remove();
                overflow--;
            }
        }
    }
}
