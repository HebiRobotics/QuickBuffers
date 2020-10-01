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

/**
 * @author Florian Enner
 * @since 17 Nov 2019
 */
public interface ProtoEnum<E extends Enum> {

    public int getNumber();

    public String getName();

    public interface EnumConverter<E extends ProtoEnum> {

        /**
         * @param value number defined in proto file
         * @return corresponding enum value or null if not known
         */
        public E forNumber(int value);

        public E forName(CharSequence value);

    }

}
