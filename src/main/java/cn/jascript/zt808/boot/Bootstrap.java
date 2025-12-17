package cn.jascript.zt808.boot;


import cn.jascript.zt808.codec.DecoderFactory;
import cn.jascript.zt808.codec.EncoderFactory;
import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.handler.ConnectionEventHandler;
import cn.jascript.zt808.handler.DataEventHandler;
import cn.jascript.zt808.handler.IdleHandlerFactory;
import cn.jascript.zt808.handler.MdcLogHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bootstrap {

    private  NioEventLoopGroup  bossGroup;
    private   NioEventLoopGroup  workerGroup;
    private ServerBootstrap bootstrap;
    private DefaultEventExecutorGroup businessGroup;


    public static Bootstrap getServer(){
        return ServerHolder.BOOTSTRAP;
    }

    private static class ServerHolder {
        private static final Bootstrap BOOTSTRAP = new Bootstrap();

        private ServerHolder(){}
    }

    private void  init() {
        log.info("server init");
        bossGroup = new NioEventLoopGroup();
        workerGroup  = new NioEventLoopGroup();
        int bizThreads = AppConfig.get().getBusinessExecutor().effectiveThreads();
        businessGroup = new DefaultEventExecutorGroup(bizThreads);
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG,128)
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //解码器
                        ch.pipeline()
                                .addLast(DecoderFactory.getDelimiterDecoder())
                                .addLast(DecoderFactory.getEscapeDecoder())
                                .addLast(DecoderFactory.getBccValidDecoder())
                                .addLast(DecoderFactory.getMsgDecoder());
                        //处理器
                        ch.pipeline()
                                //超时
                                .addLast(IdleHandlerFactory.getIdleHandler(AppConfig.get().getServer().getIdleSeconds()))
                                .addLast(new MdcLogHandler())
                                .addLast(new ConnectionEventHandler())
                                // 业务分发放入业务线程池，避免阻塞 IO 线程
                                .addLast(businessGroup, new DataEventHandler());
                        //编码器
                        ch.pipeline()
                                .addLast(EncoderFactory.getMsgEncoder());

                    }
                });

    }

    public void start(int port,boolean closeSync){
        init();
        try {
            var channelFuture = bootstrap.bind(port).sync();
            log.info("server start on port:{}", port);
            if(closeSync)
                channelFuture.channel().closeFuture().sync();
            //注册关闭hook
            registerShutdownHook();

        }catch (InterruptedException e) {
            log.info("server exception", e);
        }
    }

    public    void  close(){
        log.info("server closing");
        try {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }catch (Exception e){
            log.info("server close exception",e);
        }
        log.info("server closed");
    }

    //注册一个jvm关闭hook
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }
}
