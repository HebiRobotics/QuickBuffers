/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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
package us.hebi.quickbuf.schubfach;

import org.junit.Test;
import us.hebi.quickbuf.ProtoUtil;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 14 Jän 2023
 */
public class SchubfachTest {

    @Test
    public void testReplacedClassFile() {
        // The dummy class file throws an error, so it gets
        // replaced, but we should also check that it's the
        // correct result.
        if (Math9.isAvailable()) {
            // System.out.println("found jdk >=9");
        } else {
            // System.out.println("found jdk <9");
        }
    }

    @Test
    public void testDoubleToString() {
        for (int i = 0; i < 100; i++) {
            double expected = rnd.nextDouble();
            String str = DoubleToDecimal.toString(expected);
            double actual = Double.parseDouble(str);
            assertTrue(ProtoUtil.isEqual(expected, actual));
        }
    }

    @Test
    public void testFloatToString() {
        for (int i = 0; i < 100; i++) {
            float expected = rnd.nextFloat();
            String str = FloatToDecimal.toString(expected);
            float actual = Float.parseFloat(str);
            assertTrue(ProtoUtil.isEqual(expected, actual));
        }
    }

    Random rnd = new Random();

}
