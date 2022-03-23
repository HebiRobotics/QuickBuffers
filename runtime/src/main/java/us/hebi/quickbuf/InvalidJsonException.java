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

/**
 * @author Florian Enner
 * @since 18 Mär 2022
 */
public class InvalidJsonException extends InvalidProtocolBufferException {

    private static final long serialVersionUID = 1L;

    public InvalidJsonException(String description) {
        super(description);
    }

    static InvalidJsonException truncatedMessage() {
        return new InvalidJsonException(
                "While parsing a protocol message, the input ended unexpectedly " +
                        "in the middle of a field. This could mean either than the " +
                        "input has been truncated or that the input is invalid.");
    }

    static InvalidJsonException illegalNumberFormat() {
        return new InvalidJsonException("Illegal number format.");
    }

}
