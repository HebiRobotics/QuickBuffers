/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2022 HEBI Robotics
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when attempting to build a protocol message that is missing required fields. This is a
 * {@code RuntimeException} because it normally represents a programming error: it happens when some
 * code which constructs a message fails to set all the fields. {@code parseFrom()} methods <b>do
 * not</b> throw this; they throw an {@link InvalidProtocolBufferException} if required fields are
 * missing, because it is not a programming error to receive an incomplete message. In other words,
 * {@code UninitializedMessageException} should never be thrown by correct code, but {@code
 * InvalidProtocolBufferException} might be.
 *
 * @author kenton@google.com Kenton Varda
 */
public class UninitializedMessageException extends RuntimeException {

    private static final long serialVersionUID = 0L;

    public UninitializedMessageException(final ProtoMessage<?> message) {
        this(message.getMissingFields());
    }

    public UninitializedMessageException(final List<String> missingFields) {
        super(buildDescription(missingFields));
        this.missingFields = missingFields;
    }

    private final List<String> missingFields;

    /**
     * Get a list of human-readable names of required fields missing from this message. Each name is a
     * full path to a field, e.g. "foo.bar[5].baz". Returns null if the lite runtime was used, since
     * it lacks the ability to find missing fields.
     */
    public List<String> getMissingFields() {
        return Collections.unmodifiableList(missingFields);
    }

    /**
     * Converts this exception to an {@link InvalidProtocolBufferException}. When a parsed message is
     * missing required fields, this should be thrown instead of {@code UninitializedMessageException}.
     */
    public InvalidProtocolBufferException asInvalidProtocolBufferException() {
        return new InvalidProtocolBufferException(this);
    }

    /** Construct the description string for this exception. */
    private static String buildDescription(final List<String> missingFields) {
        final StringBuilder description = new StringBuilder("Message missing required fields: ");
        boolean first = true;
        for (final String field : missingFields) {
            if (first) {
                first = false;
            } else {
                description.append(", ");
            }
            description.append(field);
        }
        return description.toString();
    }

}
