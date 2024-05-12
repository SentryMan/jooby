/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty.buffer;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.List;

import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.DataBufferFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Implementation of the {@code DataBufferFactory} interface based on a Netty 4 {@link
 * ByteBufAllocator}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see io.netty.buffer.PooledByteBufAllocator
 * @see io.netty.buffer.UnpooledByteBufAllocator
 */
public class NettyDataBufferFactory implements DataBufferFactory {

  private final ByteBufAllocator byteBufAllocator;

  /**
   * Create a new {@code NettyDataBufferFactory} based on the given factory.
   *
   * @param byteBufAllocator the factory to use
   * @see io.netty.buffer.PooledByteBufAllocator
   * @see io.netty.buffer.UnpooledByteBufAllocator
   */
  public NettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
    requireNonNull(byteBufAllocator, "ByteBufAllocator must not be null");
    this.byteBufAllocator = byteBufAllocator;
  }

  /** Return the {@code ByteBufAllocator} used by this factory. */
  public ByteBufAllocator getByteBufAllocator() {
    return this.byteBufAllocator;
  }

  @Override
  public NettyDataBuffer allocateBuffer() {
    ByteBuf byteBuf = this.byteBufAllocator.buffer();
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public NettyDataBuffer allocateBuffer(int initialCapacity) {
    ByteBuf byteBuf = this.byteBufAllocator.buffer(initialCapacity);
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public NettyDataBuffer wrap(ByteBuffer byteBuffer) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(byteBuffer);
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public DataBuffer wrap(byte[] bytes) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    return new NettyDataBuffer(byteBuf, this);
  }

  /**
   * Wrap the given Netty {@link ByteBuf} in a {@code NettyDataBuffer}.
   *
   * @param byteBuf the Netty byte buffer to wrap
   * @return the wrapped buffer
   */
  public NettyDataBuffer wrap(ByteBuf byteBuf) {
    byteBuf.touch();
    return new NettyDataBuffer(byteBuf, this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation uses Netty's {@link CompositeByteBuf}.
   */
  @Override
  public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
    requireNonNull(dataBuffers, "DataBuffer List must not be empty");
    int bufferCount = dataBuffers.size();
    if (bufferCount == 1) {
      return dataBuffers.get(0);
    }
    CompositeByteBuf composite = this.byteBufAllocator.compositeBuffer(bufferCount);
    for (DataBuffer dataBuffer : dataBuffers) {
      if (!(dataBuffer instanceof NettyDataBuffer)) {
        throw new IllegalArgumentException("");
      }
      composite.addComponent(true, ((NettyDataBuffer) dataBuffer).getNativeBuffer());
    }
    return new NettyDataBuffer(composite, this);
  }

  @Override
  public boolean isDirect() {
    return this.byteBufAllocator.isDirectBufferPooled();
  }

  /**
   * Return the given Netty {@link DataBuffer} as a {@link ByteBuf}.
   *
   * <p>Returns the {@linkplain NettyDataBuffer#getNativeBuffer() native buffer} if {@code
   * dataBuffer} is a {@link NettyDataBuffer}; returns {@link Unpooled#wrappedBuffer(ByteBuffer)}
   * otherwise.
   *
   * @param dataBuffer the {@code DataBuffer} to return a {@code ByteBuf} for
   * @return the netty {@code ByteBuf}
   */
  public static ByteBuf toByteBuf(DataBuffer dataBuffer) {
    if (dataBuffer instanceof NettyDataBuffer nettyDataBuffer) {
      return nettyDataBuffer.getNativeBuffer();
    } else {
      ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
      dataBuffer.toByteBuffer(byteBuffer);
      return Unpooled.wrappedBuffer(byteBuffer);
    }
  }

  @Override
  public String toString() {
    return "NettyDataBufferFactory (" + this.byteBufAllocator + ")";
  }
}
