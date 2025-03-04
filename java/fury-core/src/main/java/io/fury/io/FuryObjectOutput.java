/*
 * Copyright 2023 The Fury Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fury.io;

import io.fury.Fury;
import io.fury.memory.MemoryBuffer;
import io.fury.serializer.StringSerializer;
import io.fury.util.Preconditions;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

/**
 * ObjectOutput based on {@link Fury} and {@link MemoryBuffer}.
 *
 * @author chaokunyang
 */
public class FuryObjectOutput extends OutputStream implements ObjectOutput {
  private final Fury fury;
  private final DataOutputStream utf8out = new DataOutputStream(this);
  private final StringSerializer stringSerializer;
  private MemoryBuffer buffer;

  public FuryObjectOutput(Fury fury, MemoryBuffer buffer) {
    this.fury = fury;
    this.buffer = buffer;
    this.stringSerializer = new StringSerializer(fury);
  }

  public MemoryBuffer getBuffer() {
    return buffer;
  }

  public void setBuffer(MemoryBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void writeObject(Object obj) throws IOException {
    fury.writeRef(buffer, obj);
  }

  @Override
  public void write(int b) throws IOException {
    buffer.writeByte((byte) b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    buffer.writeBytes(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    buffer.writeBytes(b, off, len);
  }

  @Override
  public void writeBoolean(boolean v) throws IOException {
    buffer.writeBoolean(v);
  }

  @Override
  public void writeByte(int v) throws IOException {
    buffer.writeByte((byte) v);
  }

  @Override
  public void writeShort(int v) throws IOException {
    buffer.writeShort((short) v);
  }

  @Override
  public void writeChar(int v) throws IOException {
    buffer.writeChar((char) v);
  }

  @Override
  public void writeInt(int v) throws IOException {
    buffer.writeInt(v);
  }

  @Override
  public void writeLong(long v) throws IOException {
    buffer.writeLong(v);
  }

  @Override
  public void writeFloat(float v) throws IOException {
    buffer.writeFloat(v);
  }

  @Override
  public void writeDouble(double v) throws IOException {
    buffer.writeDouble(v);
  }

  @Override
  public void writeBytes(String s) throws IOException {
    Preconditions.checkNotNull(s);
    int len = s.length();
    for (int i = 0; i < len; i++) {
      buffer.writeByte((byte) s.charAt(i));
    }
  }

  @Override
  public void writeChars(String s) throws IOException {
    Preconditions.checkNotNull(s);
    stringSerializer.writeJavaString(buffer, s);
  }

  @Override
  public void writeUTF(String s) throws IOException {
    Preconditions.checkNotNull(s);
    stringSerializer.writeJavaString(buffer, s);
  }

  @Override
  public void flush() throws IOException {}

  @Override
  public void close() throws IOException {}
}
