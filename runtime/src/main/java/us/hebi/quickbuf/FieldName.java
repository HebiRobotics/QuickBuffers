/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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

    public static FieldName forField(String fieldName) {
        return new FieldName(fieldName);
    }

    private FieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return fieldName;
    }

    /**
     * @return utf8 bytes with name quotes and colon
     */
    public byte[] asJsonKey() {
        if (jsonKey == null) {
            jsonKey = ('"' + fieldName + '"' + ':').getBytes(Charsets.UTF_8);
        }
        return jsonKey;
    }

    private final String fieldName;
    private byte[] jsonKey;

}
