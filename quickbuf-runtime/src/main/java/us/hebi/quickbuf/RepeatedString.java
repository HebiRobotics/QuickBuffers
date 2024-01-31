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
public final class RepeatedString extends RepeatedObject<RepeatedString, Utf8String, CharSequence, String> {

    public static RepeatedString newEmptyInstance() {
        return new RepeatedString();
    }

    RepeatedString() {
    }

    public String get(final int index, final Utf8Decoder decoder) {
        checkIndex(index);
        return array[index].getString(decoder);
    }

    @Override
    protected void setIndex0(int index, CharSequence value) {
        array[index].copyFrom(value);
    }

    @Override
    protected String getIndex0(int index) {
        return array[index].getString();
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected void copyFrom0(Utf8String store, Utf8String other) {
        store.copyFrom(other);
    }

    @Override
    protected Utf8String[] allocateArray0(int desiredSize) {
        return new Utf8String[desiredSize];
    }

    @Override
    protected Utf8String createEmpty() {
        return Utf8String.newEmptyInstance();
    }

}
