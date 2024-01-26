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

/**
 * This class calls JDK methods that were added in future versions.
 * The real class file gets compiled separately with a newer JDK and
 * a Java 6 source/target level, so it can be loaded with older JDKs.
 * <p>
 * When called from older JDKs, the methods will throw a NoSuchMethod
 * Exception. This allows using intrinsics without the overhead of
 * reflections. The effect is similar to creating a multi-release (MR)
 * jar, but is better supported by tooling and bytecode analyzers for
 * obfuscation or native images.
 *
 * @author Florian Enner
 * @since 14 Jan 2023
 */
class JdkMethods {

    public static long multiplyHigh(long x, long y) {
        throw new NoSuchMethodError();
    }

}
