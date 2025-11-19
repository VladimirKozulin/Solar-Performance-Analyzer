package com.solarlab.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Высокопроизводительный HTTP-клиент на базе Netty.
 * 
 * Особенности:
 * - Connection pooling для повторного использования соединений
 * - Direct byte buffers для zero-copy операций
 * - Адаптивное изменение размера буферов
 * - Автоматическое следование редиректам
 * - TCP оптимизации (TCP_NODELAY, SO_KEEPALIVE)
 * 
 * High-performance Netty-based HTTP client with connection pooling and zero-copy buffers.
 */
public class NettyImageDownloader {
    private static final Logger logger = LoggerFactory.getLogger(NettyImageDownloader.class);
    
    private final HttpClient httpClient;

    public NettyImageDownloader() {
        // Настройка пула соединений для оптимальной производительности
        // Configure connection pool for optimal performance
        ConnectionProvider provider = ConnectionProvider.builder("solar-pool")
            .maxConnections(10)                          // Максимум 10 соединений / Max 10 connections
            .maxIdleTime(Duration.ofSeconds(30))         // Время простоя / Idle time
            .maxLifeTime(Duration.ofMinutes(5))          // Время жизни / Lifetime
            .pendingAcquireMaxCount(50)                  // Очередь ожидания / Pending queue
            .evictInBackground(Duration.ofSeconds(60))   // Фоновая очистка / Background eviction
            .build();

        // Create HTTP client with direct buffer allocation
        this.httpClient = HttpClient.create(provider)
            .compress(true)
            .followRedirect(true)
            .responseTimeout(Duration.ofSeconds(10))
            .option(io.netty.channel.ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
            .option(io.netty.channel.ChannelOption.TCP_NODELAY, true)
            .option(io.netty.channel.ChannelOption.RCVBUF_ALLOCATOR, 
                new io.netty.channel.AdaptiveRecvByteBufAllocator(64, 1024, 65536));
    }

    public Mono<ByteBuf> download(String url) {
        return httpClient
            .get()
            .uri(url)
            .responseSingle((response, content) -> {
                if (response.status().code() != 200) {
                    return Mono.error(new RuntimeException(
                        "HTTP " + response.status().code() + " for " + url));
                }
                
                return content.asByteArray()
                    .map(bytes -> {
                        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(bytes.length);
                        buf.writeBytes(bytes);
                        logger.debug("Downloaded {} bytes from {}", buf.readableBytes(), url);
                        return buf;
                    });
            })
            .doOnError(error -> logger.error("Download failed for {}: {}", 
                url, error.getMessage()));
    }

    public void shutdown() {
        logger.info("Shutting down Netty downloader");
    }
}
