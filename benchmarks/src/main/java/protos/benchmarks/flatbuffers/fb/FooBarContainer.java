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
public final class FooBarContainer extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_2_0_0(); }
  public static FooBarContainer getRootAsFooBarContainer(ByteBuffer _bb) { return getRootAsFooBarContainer(_bb, new FooBarContainer()); }
  public static FooBarContainer getRootAsFooBarContainer(ByteBuffer _bb, FooBarContainer obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public FooBarContainer __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public protos.benchmarks.flatbuffers.fb.FooBar list(int j) { return list(new protos.benchmarks.flatbuffers.fb.FooBar(), j); }
  public protos.benchmarks.flatbuffers.fb.FooBar list(protos.benchmarks.flatbuffers.fb.FooBar obj, int j) { int o = __offset(4); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int listLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public protos.benchmarks.flatbuffers.fb.FooBar.Vector listVector() { return listVector(new protos.benchmarks.flatbuffers.fb.FooBar.Vector()); }
  public protos.benchmarks.flatbuffers.fb.FooBar.Vector listVector(protos.benchmarks.flatbuffers.fb.FooBar.Vector obj) { int o = __offset(4); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }
  public boolean initialized() { int o = __offset(6); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public short fruit() { int o = __offset(8); return o != 0 ? bb.getShort(o + bb_pos) : 0; }
  public String location() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer locationAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer locationInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }

  public static int createFooBarContainer(FlatBufferBuilder builder,
      int listOffset,
      boolean initialized,
      short fruit,
      int locationOffset) {
    builder.startTable(4);
    FooBarContainer.addLocation(builder, locationOffset);
    FooBarContainer.addList(builder, listOffset);
    FooBarContainer.addFruit(builder, fruit);
    FooBarContainer.addInitialized(builder, initialized);
    return FooBarContainer.endFooBarContainer(builder);
  }

  public static void startFooBarContainer(FlatBufferBuilder builder) { builder.startTable(4); }
  public static void addList(FlatBufferBuilder builder, int listOffset) { builder.addOffset(0, listOffset, 0); }
  public static int createListVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startListVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addInitialized(FlatBufferBuilder builder, boolean initialized) { builder.addBoolean(1, initialized, false); }
  public static void addFruit(FlatBufferBuilder builder, short fruit) { builder.addShort(2, fruit, 0); }
  public static void addLocation(FlatBufferBuilder builder, int locationOffset) { builder.addOffset(3, locationOffset, 0); }
  public static int endFooBarContainer(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }
  public static void finishFooBarContainerBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  public static void finishSizePrefixedFooBarContainerBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public FooBarContainer get(int j) { return get(new FooBarContainer(), j); }
    public FooBarContainer get(FooBarContainer obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

