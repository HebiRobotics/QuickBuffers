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
 * Provides a text output for messages. The key is expected to be a
 * ascii representation of the field name including json escaping, e.g.,
 *
 * "ascii-text":
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public interface TextPrinter {

    // ======== Root Message ========

    public TextPrinter print(ProtoMessage value);

    // ======== Optional / Required ========

    public void print(byte[] key, boolean value);

    public void print(byte[] key, int value);

    public void print(byte[] key, long value);

    public void print(byte[] key, float value);

    public void print(byte[] key, double value);

    public void print(byte[] key, ProtoMessage value);

    public void print(byte[] key, StringBuilder value);

    public void print(byte[] key, RepeatedByte value);

    // ======== Repeated ========

    public void print(byte[] key, RepeatedBoolean value);

    public void print(byte[] key, RepeatedInt value);

    public void print(byte[] key, RepeatedLong value);

    public void print(byte[] key, RepeatedFloat value);

    public void print(byte[] key, RepeatedDouble value);

    public void print(byte[] key, RepeatedMessage value);

    public void print(byte[] key, RepeatedString value);

    public void print(byte[] key, RepeatedBytes value);

}
