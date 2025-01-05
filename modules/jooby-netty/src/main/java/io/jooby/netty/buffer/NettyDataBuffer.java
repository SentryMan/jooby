/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntPredicate;

import io.jooby.Context;
import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.PooledDataBuffer;
import io.jooby.internal.netty.NettyContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty 4 {@link ByteBuf}.
 * Typically constructed with {@link NettyDataBufferFactory}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public class NettyDataBuffer implements PooledDataBuffer {

  private ByteBuf byteBuf;

  private final NettyDataBufferFactory dataBufferFactory;

  /**
   * Create a new {@code NettyDataBuffer} based on the given {@code ByteBuff}.
   *
   * @param byteBuf the buffer to base this buffer on
   */
  NettyDataBuffer(ByteBuf byteBuf, NettyDataBufferFactory dataBufferFactory) {
    Objects.requireNonNull(byteBuf, "ByteBuf must not be null");
    Objects.requireNonNull(dataBufferFactory, "NettyDataBufferFactory must not be null");
    this.byteBuf = byteBuf;
    this.dataBufferFactory = dataBufferFactory;
  }

  /**
   * Directly exposes the native {@code ByteBuf} that this buffer is based on.
   *
   * @return the wrapped byte buffer
   */
  public ByteBuf getNativeBuffer() {
    return this.byteBuf;
  }

  @Override
  public NettyDataBufferFactory factory() {
    return this.dataBufferFactory;
  }

  @Override
  public int indexOf(IntPredicate predicate, int fromIndex) {
    Objects.requireNonNull(predicate, "IntPredicate must not be null");
    if (fromIndex < 0) {
      fromIndex = 0;
    } else if (fromIndex >= this.byteBuf.writerIndex()) {
      return -1;
    }
    int length = this.byteBuf.writerIndex() - fromIndex;
    return this.byteBuf.forEachByte(fromIndex, length, predicate.negate()::test);
  }

  @Override
  public int lastIndexOf(IntPredicate predicate, int fromIndex) {
    Objects.requireNonNull(predicate, "IntPredicate must not be null");
    if (fromIndex < 0) {
      return -1;
    }
    fromIndex = Math.min(fromIndex, this.byteBuf.writerIndex() - 1);
    return this.byteBuf.forEachByteDesc(0, fromIndex + 1, predicate.negate()::test);
  }

  @Override
  public int readableByteCount() {
    return this.byteBuf.readableBytes();
  }

  @Override
  public int writableByteCount() {
    return this.byteBuf.writableBytes();
  }

  @Override
  public int readPosition() {
    return this.byteBuf.readerIndex();
  }

  @Override
  public NettyDataBuffer readPosition(int readPosition) {
    this.byteBuf.readerIndex(readPosition);
    return this;
  }

  @Override
  public int writePosition() {
    return this.byteBuf.writerIndex();
  }

  @Override
  public NettyDataBuffer writePosition(int writePosition) {
    this.byteBuf.writerIndex(writePosition);
    return this;
  }

  @Override
  public byte getByte(int index) {
    return this.byteBuf.getByte(index);
  }

  @Override
  public int capacity() {
    return this.byteBuf.capacity();
  }

  @Override
  public NettyDataBuffer duplicate() {
    return new NettyDataBuffer(byteBuf.duplicate(), dataBufferFactory);
  }

  @Override
  public NettyDataBuffer ensureWritable(int capacity) {
    this.byteBuf.ensureWritable(capacity);
    return this;
  }

  @Override
  public byte read() {
    return this.byteBuf.readByte();
  }

  @Override
  public NettyDataBuffer read(byte[] destination) {
    this.byteBuf.readBytes(destination);
    return this;
  }

  @Override
  public NettyDataBuffer read(byte[] destination, int offset, int length) {
    this.byteBuf.readBytes(destination, offset, length);
    return this;
  }

  @Override
  public NettyDataBuffer write(byte b) {
    this.byteBuf.writeByte(b);
    return this;
  }

  @Override
  public NettyDataBuffer write(byte[] source) {
    this.byteBuf.writeBytes(source);
    return this;
  }

  @Override
  public NettyDataBuffer write(byte[] source, int offset, int length) {
    this.byteBuf.writeBytes(source, offset, length);
    return this;
  }

  @Override
  public NettyDataBuffer write(DataBuffer... dataBuffers) {
    if (hasNettyDataBuffers(dataBuffers)) {
      ByteBuf[] nativeBuffers = new ByteBuf[dataBuffers.length];
      for (int i = 0; i < dataBuffers.length; i++) {
        nativeBuffers[i] = ((NettyDataBuffer) dataBuffers[i]).getNativeBuffer();
      }
      write(nativeBuffers);
    } else {
      ByteBuffer[] byteBuffers = new ByteBuffer[dataBuffers.length];
      for (int i = 0; i < dataBuffers.length; i++) {
        byteBuffers[i] = ByteBuffer.allocate(dataBuffers[i].readableByteCount());
        dataBuffers[i].toByteBuffer(byteBuffers[i]);
      }
      write(byteBuffers);
    }
    return this;
  }

  private static boolean hasNettyDataBuffers(DataBuffer[] buffers) {
    for (DataBuffer buffer : buffers) {
      if (!(buffer instanceof NettyDataBuffer)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public NettyDataBuffer write(ByteBuffer... buffers) {
    for (ByteBuffer buffer : buffers) {
      this.byteBuf.writeBytes(buffer);
    }
    return this;
  }

  /**
   * Writes one or more Netty {@link ByteBuf ByteBufs} to this buffer, starting at the current
   * writing position.
   *
   * @param byteBufs the buffers to write into this buffer
   * @return this buffer
   */
  public NettyDataBuffer write(ByteBuf... byteBufs) {
    for (ByteBuf byteBuf : byteBufs) {
      this.byteBuf.writeBytes(byteBuf);
    }
    return this;
  }

  @Override
  public DataBuffer write(CharSequence charSequence, Charset charset) {
    Objects.requireNonNull(charSequence, "CharSequence must not be null");
    Objects.requireNonNull(charset, "Charset must not be null");
    if (StandardCharsets.UTF_8.equals(charset)) {
      ByteBufUtil.writeUtf8(this.byteBuf, charSequence);
    } else if (StandardCharsets.US_ASCII.equals(charset)) {
      ByteBufUtil.writeAscii(this.byteBuf, charSequence);
    } else {
      return PooledDataBuffer.super.write(charSequence, charset);
    }
    return this;
  }

  @Override
  public NettyDataBuffer split(int index) {
    ByteBuf split = this.byteBuf.retainedSlice(0, index);
    int writerIndex = this.byteBuf.writerIndex();
    int readerIndex = this.byteBuf.readerIndex();

    split.writerIndex(Math.min(writerIndex, index));
    split.readerIndex(Math.min(readerIndex, index));

    this.byteBuf = this.byteBuf.slice(index, this.byteBuf.capacity() - index);
    this.byteBuf.writerIndex(Math.max(writerIndex, index) - index);
    this.byteBuf.readerIndex(Math.max(readerIndex, index) - index);

    return new NettyDataBuffer(split, this.dataBufferFactory);
  }

  @Override
  public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
    Objects.requireNonNull(dest, "Dest must not be null");

    dest = dest.duplicate().clear();
    dest.put(destPos, this.byteBuf.nioBuffer(srcPos, length), 0, length);
  }

  @Override
  public DataBuffer.ByteBufferIterator readableByteBuffers() {
    ByteBuffer[] readable =
        this.byteBuf.nioBuffers(this.byteBuf.readerIndex(), this.byteBuf.readableBytes());
    return new ByteBufferIterator(readable, true);
  }

  @Override
  public DataBuffer.ByteBufferIterator writableByteBuffers() {
    ByteBuffer[] writable =
        this.byteBuf.nioBuffers(this.byteBuf.writerIndex(), this.byteBuf.writableBytes());
    return new ByteBufferIterator(writable, false);
  }

  @Override
  public String toString(Charset charset) {
    Objects.requireNonNull(charset, "Charset must not be null");
    return this.byteBuf.toString(charset);
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    Objects.requireNonNull(charset, "Charset must not be null");
    return this.byteBuf.toString(index, length, charset);
  }

  @Override
  public DataBuffer clear() {
    this.byteBuf.clear();
    return this;
  }

  @Override
  public Context send(Context ctx) {
    ((NettyContext) ctx).send(this.byteBuf);
    return ctx;
  }

  @Override
  public boolean isAllocated() {
    return this.byteBuf.refCnt() > 0;
  }

  @Override
  public PooledDataBuffer retain() {
    return new NettyDataBuffer(this.byteBuf.retain(), this.dataBufferFactory);
  }

  @Override
  public PooledDataBuffer touch(Object hint) {
    this.byteBuf.touch(hint);
    return this;
  }

  @Override
  public boolean release() {
    return this.byteBuf.release();
  }

  @Override
  public boolean equals(Object other) {
    return (this == other
        || (other instanceof NettyDataBuffer that && this.byteBuf.equals(that.byteBuf)));
  }

  @Override
  public int hashCode() {
    return this.byteBuf.hashCode();
  }

  @Override
  public String toString() {
    return this.byteBuf.toString();
  }

  private static final class ByteBufferIterator implements DataBuffer.ByteBufferIterator {

    private final ByteBuffer[] byteBuffers;

    private final boolean readOnly;

    private int cursor = 0;

    public ByteBufferIterator(ByteBuffer[] byteBuffers, boolean readOnly) {
      this.byteBuffers = byteBuffers;
      this.readOnly = readOnly;
    }

    @Override
    public boolean hasNext() {
      return this.cursor < this.byteBuffers.length;
    }

    @Override
    public ByteBuffer next() {
      int index = this.cursor;
      if (index < this.byteBuffers.length) {
        this.cursor = index + 1;
        ByteBuffer next = this.byteBuffers[index];
        return this.readOnly ? next.asReadOnlyBuffer() : next;
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void close() {}
  }
}
