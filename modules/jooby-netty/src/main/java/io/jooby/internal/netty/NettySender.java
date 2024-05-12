/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Sender;
import io.jooby.buffer.DataBuffer;
import io.jooby.netty.buffer.NettyDataBuffer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

public class NettySender implements Sender {

  private final NettyContext ctx;
  private final ChannelHandlerContext context;

  public NettySender(NettyContext ctx, ChannelHandlerContext context) {
    this.ctx = ctx;
    this.context = context;
  }

  @Override
  public Sender write(@NonNull byte[] data, @NonNull Callback callback) {
    context
        .writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data)))
        .addListener(newChannelFutureListener(ctx, callback));
    return this;
  }

  @NonNull @Override
  public Sender write(@NonNull DataBuffer data, @NonNull Callback callback) {
    context
        .writeAndFlush(new DefaultHttpContent(((NettyDataBuffer) data).getNativeBuffer()))
        .addListener(newChannelFutureListener(ctx, callback));
    return this;
  }

  @Override
  public void close() {
    context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ctx);
    ctx.requestComplete();
  }

  private static ChannelFutureListener newChannelFutureListener(
      NettyContext ctx, Callback callback) {
    return future -> {
      if (future.isSuccess()) {
        callback.onComplete(ctx, null);
      } else {
        Throwable cause = future.cause();
        try {
          callback.onComplete(ctx, cause);
        } finally {
          ctx.destroy(cause);
        }
      }
    };
  }
}
