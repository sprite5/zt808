package cn.jascript.zt808.session;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowIdGenerator {

    private static final int MAX = 0xFFFF;

    private final SessionRouter sessionRouter;
    private final AtomicInteger fallbackCounter = new AtomicInteger(0);

    public FlowIdGenerator() {
        this(new SessionRouter());
    }

    public FlowIdGenerator(SessionRouter sessionRouter) {
        this.sessionRouter = sessionRouter;
    }

    public int nextFlowId(String terminalId) {
        if (Objects.nonNull(terminalId) && !terminalId.isBlank()) {
            var sessionOpt = sessionRouter.routeSession(terminalId);
            if (sessionOpt.isPresent()) {
                var counter = sessionOpt.get().getFlowIdCounter();
                if (Objects.nonNull(counter)) {
                    return normalize(counter.incrementAndGet());
                }
            }
        }
        return normalize(fallbackCounter.incrementAndGet());
    }

    private static int normalize(int value) {
        return value & MAX;
    }
}
