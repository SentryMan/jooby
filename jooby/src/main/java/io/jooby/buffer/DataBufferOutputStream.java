/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} that writes to a {@link DataBuffer}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see DataBuffer#asOutputStream()
 */
final class DataBufferOutputStream extends OutputStream {

  private final DataBuffer dataBuffer;

  private boolean closed;

  public DataBufferOutputStream(DataBuffer dataBuffer) {
    Assert.notNull(dataBuffer, "DataBuffer must not be null");
    this.dataBuffer = dataBuffer;
  }

  @Override
  public void write(int b) throws IOException {
    checkClosed();
    this.dataBuffer.ensureWritable(1);
    this.dataBuffer.write((byte) b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    checkClosed();
    if (len > 0) {
      this.dataBuffer.ensureWritable(len);
      this.dataBuffer.write(b, off, len);
    }
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("DataBufferOutputStream is closed");
    }
  }
}
