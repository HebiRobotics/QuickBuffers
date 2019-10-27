/*-
 * #%L
 * robobuf-runtime
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

package us.hebi.robobuf;

/**
 * Provides a text output for messages
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public interface ProtoPrinter {

    // ======== Root Message ========

    public ProtoPrinter print(ProtoMessage value);

    // ======== Optional / Required ========

    public void print(String field, boolean value);

    public void print(String field, int value);

    public void print(String field, long value);

    public void print(String field, float value);

    public void print(String field, double value);

    public void print(String field, ProtoMessage value);

    public void print(String field, StringBuilder value);

    public void print(String field, RepeatedByte value);

    // ======== Repeated ========

    public void print(String field, RepeatedBoolean value);

    public void print(String field, RepeatedInt value);

    public void print(String field, RepeatedLong value);

    public void print(String field, RepeatedFloat value);

    public void print(String field, RepeatedDouble value);

    public void print(String field, RepeatedMessage value);

    public void print(String field, RepeatedString value);

    public void print(String field, RepeatedBytes value);

}
