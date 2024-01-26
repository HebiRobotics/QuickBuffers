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
 * @since 17 Nov 2019
 */
public interface ProtoEnum<E extends Enum> {

    public int getNumber();

    public String getName();

    public interface EnumConverter<E extends ProtoEnum> {

        /**
         * @param value The numeric wire value of the enum entry.
         * @return The enum value associated with the given numeric wire value, or null if unknown.
         */
        public E forNumber(int value);

        /**
         *
         * @param value The text representation of the enum entry
         * @return The enum value associated with the given text representation, or null if unknown.
         */
        public E forName(CharSequence value);

    }

}
