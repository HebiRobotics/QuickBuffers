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

import us.hebi.quickbuf.ProtoUtil.Charsets;

/**
 * Field name for serializing text based output like JSON. It allows
 * for caching e.g. the encoded field name while allowing external
 * libraries to access what they can consume.
 *
 * @author Florian Enner
 * @since 28 Nov 2019
 */
public final class FieldName {

    public static FieldName forField(String jsonName) {
        return new FieldName(jsonName, jsonName);
    }

    public static FieldName forField(String jsonName, String protoName) {
        return new FieldName(jsonName, protoName);
    }

    private FieldName(String jsonName, String protoName) {
        this.jsonName = jsonName;
        this.protoName = protoName;
    }

    public String getJsonName() {
        return jsonName;
    }

    public String getProtoName() {
        return protoName;
    }

    /**
     * @return utf8 bytes with name quotes and colon
     */
    public byte[] getJsonKeyBytes() {
        if (jsonKey == null) {
            jsonKey = ('"' + jsonName + '"' + ':').getBytes(Charsets.UTF_8);
        }
        return jsonKey;
    }

    /**
     * @return utf8 bytes with name quotes and colon
     */
    public byte[] getProtoKeyBytes() {
        if (protoKey == null) {
            protoKey = ('"' + protoName + '"' + ':').getBytes(Charsets.UTF_8);
        }
        return protoKey;
    }

    private final String jsonName;
    private final String protoName;

    private byte[] jsonKey;
    private byte[] protoKey;

}
