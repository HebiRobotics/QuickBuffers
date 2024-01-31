/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedBytes extends RepeatedObject<RepeatedBytes, RepeatedByte, byte[], RepeatedByte> {

    public static RepeatedBytes newEmptyInstance() {
        return new RepeatedBytes();
    }

    private RepeatedBytes() {
    }

    @Override
    protected void setIndex0(int index, byte[] value) {
        array[index].copyFrom(value);
    }

    @Override
    protected RepeatedByte getIndex0(int index) {
        return array[index];
    }

    @Override
    protected void copyFrom0(RepeatedByte store, RepeatedByte other) {
        store.copyFrom(other);
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected RepeatedByte createEmpty() {
        return new RepeatedByte();
    }

    @Override
    protected RepeatedByte[] allocateArray0(int desiredSize) {
        return new RepeatedByte[desiredSize];
    }

}
