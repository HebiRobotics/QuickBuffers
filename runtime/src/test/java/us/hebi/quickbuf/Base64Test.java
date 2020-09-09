/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 09 Sep 2020
 */
public class Base64Test {

    @Test
    public void testDecodeBase64() throws Exception {
        for (byte[] sample : randomSamples) {
            String input = Base64.getEncoder().encodeToString(sample);
            assertArrayEquals(sample, decodeBase64(input));
        }
    }

    @Test
    public void testDecodeBase64_noPadding() throws Exception {
        for (byte[] sample : randomSamples) {
            String input = Base64.getEncoder().encodeToString(sample).replaceAll("=", "");
            assertArrayEquals(sample, decodeBase64(input));
        }
    }

    @Test
    public void testDecodeBase64_url() throws Exception {
        for (byte[] sample : randomSamples) {
            String input = Base64.getUrlEncoder().encodeToString(sample);
            assertArrayEquals(sample, decodeBase64(input));
        }
    }

    @Test
    public void testDecodeBase64_url_noPadding() throws Exception {
        for (byte[] sample : randomSamples) {
            String input = Base64.getUrlEncoder().encodeToString(sample).replaceAll("=", "");
            assertArrayEquals(sample, decodeBase64(input));
        }
    }

    private byte[] decodeBase64(CharSequence input) {
        return us.hebi.quickbuf.Base64.decodeFast(input.toString());
    }


    private static List<byte[]> generateRandomSamples() {
        int n = 50;
        List<byte[]> samples = new ArrayList<byte[]>(n);
        Random rnd = new Random(0);
        for (int i = 0; i < n; i++) {
            byte[] bytes = new byte[i];
            rnd.nextBytes(bytes);
            samples.add(bytes);
        }
        return samples;
    }

    private final RepeatedByte tmp = RepeatedByte.newEmptyInstance();
    private static final List<byte[]> randomSamples = generateRandomSamples();

}
