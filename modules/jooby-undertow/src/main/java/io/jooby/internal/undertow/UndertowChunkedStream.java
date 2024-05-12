/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

import org.xnio.IoUtils;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;

public class UndertowChunkedStream implements IoCallback, Runnable {

  private ReadableByteChannel source;

  private HttpServerExchange exchange;

  private Sender sender;

  private PooledByteBuffer pooled;

  private IoCallback callback;

  private final long len;

  private long total;

  public UndertowChunkedStream(final long len) {
    this.len = len;
  }

  public void send(
      final ReadableByteChannel source,
      final HttpServerExchange exchange,
      final IoCallback callback) {
    this.source = source;
    this.exchange = exchange;
    this.callback = callback;
    this.sender = exchange.getResponseSender();
    ServerConnection connection = exchange.getConnection();
    this.pooled = connection.getByteBufferPool().allocate();

    onComplete(exchange, sender);
  }

  @Override
  public void run() {
    if (pooled != null && pooled.isOpen()) {
      var buffer = pooled.getBuffer();
      try {
        buffer.clear();
        int count = source.read(buffer);
        if (count == -1 || (len != -1 && total >= len)) {
          done();
          callback.onComplete(exchange, sender);
        } else {
          total += count;
          buffer.flip();
          if (len > 0) {
            if (total > len) {
              long limit = count - (total - len);
              buffer.limit((int) limit);
            }
          }
          sender.send(buffer, this);
        }
      } catch (IOException ex) {
        onException(exchange, sender, ex);
      }
    } else {
      onException(exchange, sender, new ClosedChannelException());
    }
  }

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
    } else {
      run();
    }
  }

  @Override
  public void onException(
      final HttpServerExchange exchange, final Sender sender, final IOException ex) {
    try {
      callback.onException(exchange, sender, ex);
    } finally {
      done();
    }
  }

  private void done() {
    if (pooled != null) {
      try {
        pooled.close();
        IoUtils.safeClose(source);
      } finally {
        pooled = null;
      }
    }
  }
}
