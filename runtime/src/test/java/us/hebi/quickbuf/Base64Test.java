/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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
