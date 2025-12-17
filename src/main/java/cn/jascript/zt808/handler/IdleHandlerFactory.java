package cn.jascript.zt808.handler;

import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class IdleHandlerFactory {
    public static IdleStateHandler getDefaultIdleHandler(){
        return new IdleStateHandler(300,300,300, TimeUnit.SECONDS);
    }

    public static IdleStateHandler getIdleHandler(int idleSeconds){
        return new IdleStateHandler(idleSeconds,idleSeconds,idleSeconds, TimeUnit.SECONDS);
    }

}
