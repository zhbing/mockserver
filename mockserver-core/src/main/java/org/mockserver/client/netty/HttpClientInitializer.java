package org.mockserver.client.netty;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mockserver.client.netty.codec.MockServerClientCodec;
import org.mockserver.logging.LoggingHandler;
import org.mockserver.model.HttpResponse;
import org.mockserver.socket.SSLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;

public class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean secure;
    private final InetSocketAddress remoteAddress;
    private HttpClientHandler httpClientHandler = new HttpClientHandler();

    public HttpClientInitializer(boolean secure, InetSocketAddress remoteAddress) {
        this.secure = secure;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void initChannel(SocketChannel channel) throws SSLException {
        ChannelPipeline pipeline = channel.pipeline();

        if (secure) {
            SslContext sslCtx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            pipeline.addLast(sslCtx.newHandler(channel.alloc(), remoteAddress.getHostName(), remoteAddress.getPort()));
        }

        // add logging
        if (logger.isDebugEnabled()) {
            pipeline.addLast(new LoggingHandler(this.getClass().getSimpleName() + " -->"));
        }

        pipeline.addLast(new HttpClientCodec());

        pipeline.addLast(new HttpContentDecompressor());

        pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

        pipeline.addLast(new MockServerClientCodec());

        pipeline.addLast(httpClientHandler);
    }

    public SettableFuture<HttpResponse> getResponseFuture() {
        return httpClientHandler.getResponseFuture();
    }
}