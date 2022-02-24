/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

// automatically generated by the FlatBuffers compiler, do not modify

package protos.benchmarks.flatbuffers.fb;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class Bar extends Struct {
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public Bar __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public Foo parent() { return parent(new Foo()); }
  public Foo parent(Foo obj) { return obj.__assign(bb_pos + 0, bb); }
  public int time() { return bb.getInt(bb_pos + 16); }
  public float ratio() { return bb.getFloat(bb_pos + 20); }
  public int size() { return bb.getShort(bb_pos + 24) & 0xFFFF; }

  public static int createBar(FlatBufferBuilder builder, long parent_id, short parent_count, byte parent_prefix, long parent_length, int time, float ratio, int size) {
    builder.prep(8, 32);
    builder.pad(6);
    builder.putShort((short)size);
    builder.putFloat(ratio);
    builder.putInt(time);
    builder.prep(8, 16);
    builder.putInt((int)parent_length);
    builder.pad(1);
    builder.putByte(parent_prefix);
    builder.putShort(parent_count);
    builder.putLong(parent_id);
    return builder.offset();
  }
}

