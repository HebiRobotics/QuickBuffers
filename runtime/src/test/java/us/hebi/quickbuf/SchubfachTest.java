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
package us.hebi.quickbuf;

import org.junit.Test;
import us.hebi.quickbuf.Schubfach.DoubleToDecimal;
import us.hebi.quickbuf.Schubfach.FloatToDecimal;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 14 Jän 2023
 */
public class SchubfachTest {

    @Test
    public void testDoubleToString() {
        for (int i = 0; i < 100; i++) {
            testDouble(rnd.nextDouble());
        }
        for (int exp = -200; exp < 200; exp++) {
            testDouble(Math.pow(10, exp) + rnd.nextDouble());
        }
    }

    @Test
    public void testFloatToString() {
        for (int i = 0; i < 100; i++) {
            testFloat(rnd.nextFloat());
        }
        for (int exp = -50; exp < 50; exp++) {
            testFloat((float) Math.pow(10, exp) + rnd.nextFloat());
        }
    }

    private static void testDouble(double expected) {
        String str = DoubleToDecimal.toString(expected);
        double actual = Double.parseDouble(str);
        assertTrue(ProtoUtil.isEqual(expected, actual));
    }

    private static void testFloat(float expected) {
        String str = FloatToDecimal.toString(expected);
        float actual = Float.parseFloat(str);
        assertTrue(ProtoUtil.isEqual(expected, actual));
    }

    Random rnd = new Random();

}
