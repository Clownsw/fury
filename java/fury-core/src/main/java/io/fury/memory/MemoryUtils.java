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

package io.fury.memory;

import io.fury.util.Platform;
import io.fury.util.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Memory utils for fury.
 *
 * @author chaokunyang
 */
public class MemoryUtils {

  public static MemoryBuffer buffer(int size) {
    return wrap(new byte[size]);
  }

  public static MemoryBuffer buffer(long address, int size) {
    return MemoryBuffer.fromNativeAddress(address, size);
  }

  /**
   * Creates a new memory segment that targets to the given heap memory region.
   *
   * <p>This method should be used to turn short lived byte arrays into memory segments.
   *
   * @param buffer The heap memory region.
   * @return A new memory segment that targets the given heap memory region.
   */
  public static MemoryBuffer wrap(byte[] buffer, int offset, int length) {
    return MemoryBuffer.fromByteArray(buffer, offset, length);
  }

  public static MemoryBuffer wrap(byte[] buffer) {
    return MemoryBuffer.fromByteArray(buffer);
  }

  /**
   * Creates a new memory segment that represents the memory backing the given byte buffer section
   * of [buffer.position(), buffer,limit()).
   *
   * @param buffer a direct buffer or heap buffer
   */
  public static MemoryBuffer wrap(ByteBuffer buffer) {
    if (buffer.isDirect()) {
      return MemoryBuffer.fromByteBuffer(buffer);
    } else {
      int offset = buffer.arrayOffset() + buffer.position();
      return MemoryBuffer.fromByteArray(buffer.array(), offset, buffer.remaining());
    }
  }

  // Lazy load offset and also follow graalvm offset auto replace pattern.
  private static class Offset {
    private static final long BAS_BUF_BUF;
    private static final long BAS_BUF_COUNT;
    private static final long BIS_BUF_BUF;
    private static final long BIS_BUF_POS;
    private static final long BIS_BUF_COUNT;

    static {
      try {
        BAS_BUF_BUF =
            Platform.objectFieldOffset(ByteArrayOutputStream.class.getDeclaredField("buf"));
        BAS_BUF_COUNT =
            Platform.objectFieldOffset(ByteArrayOutputStream.class.getDeclaredField("count"));
        BIS_BUF_BUF =
            Platform.objectFieldOffset(ByteArrayInputStream.class.getDeclaredField("buf"));
        BIS_BUF_POS =
            Platform.objectFieldOffset(ByteArrayInputStream.class.getDeclaredField("pos"));
        BIS_BUF_COUNT =
            Platform.objectFieldOffset(ByteArrayInputStream.class.getDeclaredField("count"));
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Wrap a {@link ByteArrayOutputStream} into a {@link MemoryBuffer}. The writerIndex of buffer
   * will be the count of stream.
   */
  public static void wrap(ByteArrayOutputStream stream, MemoryBuffer buffer) {
    Preconditions.checkNotNull(stream);
    byte[] buf = (byte[]) Platform.getObject(stream, Offset.BAS_BUF_BUF);
    int count = Platform.getInt(stream, Offset.BAS_BUF_COUNT);
    buffer.pointTo(buf, 0, buf.length);
    buffer.writerIndex(count);
  }

  /**
   * Wrap a @link MemoryBuffer} into a {@link ByteArrayOutputStream}. The count of stream will be
   * the writerIndex of buffer.
   */
  public static void wrap(MemoryBuffer buffer, ByteArrayOutputStream stream) {
    Preconditions.checkNotNull(stream);
    byte[] bytes = buffer.getHeapMemory();
    Preconditions.checkNotNull(bytes);
    Platform.putObject(stream, Offset.BAS_BUF_BUF, bytes);
    Platform.putInt(stream, Offset.BAS_BUF_COUNT, buffer.writerIndex());
  }

  /**
   * Wrap a {@link ByteArrayInputStream} into a {@link MemoryBuffer}. The readerIndex of buffer will
   * be the pos of stream.
   */
  public static void wrap(ByteArrayInputStream stream, MemoryBuffer buffer) {
    Preconditions.checkNotNull(stream);
    byte[] buf = (byte[]) Platform.getObject(stream, Offset.BIS_BUF_BUF);
    int count = Platform.getInt(stream, Offset.BIS_BUF_COUNT);
    int pos = Platform.getInt(stream, Offset.BIS_BUF_POS);
    buffer.pointTo(buf, 0, count);
    buffer.readerIndex(pos);
  }

  public static int writePositiveVarInt(byte[] arr, int index, int v) {
    // The encoding algorithm are based on kryo UnsafeMemoryOutput.writeVarInt
    // varint are written using little endian byte order.
    if (v >>> 7 == 0) {
      arr[index] = (byte) v;
      return 1;
    }
    if (v >>> 14 == 0) {
      arr[index++] = (byte) ((v & 0x7F) | 0x80);
      arr[index] = (byte) (v >>> 7);
      return 2;
    }
    if (v >>> 21 == 0) {
      arr[index++] = (byte) ((v & 0x7F) | 0x80);
      arr[index++] = (byte) (v >>> 7 | 0x80);
      arr[index] = (byte) (v >>> 14);
      return 3;
    }
    if (v >>> 28 == 0) {
      arr[index++] = (byte) ((v & 0x7F) | 0x80);
      arr[index++] = (byte) (v >>> 7 | 0x80);
      arr[index++] = (byte) (v >>> 14 | 0x80);
      arr[index] = (byte) (v >>> 21);
      return 4;
    }
    arr[index++] = (byte) ((v & 0x7F) | 0x80);
    arr[index++] = (byte) (v >>> 7 | 0x80);
    arr[index++] = (byte) (v >>> 14 | 0x80);
    arr[index++] = (byte) (v >>> 21 | 0x80);
    arr[index] = (byte) (v >>> 28);
    return 5;
  }
}
