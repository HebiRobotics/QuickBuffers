/*-
 * #%L
 * quickbuf-generator
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
package us.hebi.quickbuf.generator;

import com.google.protobuf.compiler.PluginProtos;
import lombok.Getter;
import us.hebi.quickbuf.parser.ParserUtil;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.Boolean.*;

/**
 * Utility class for keeping track of user options passed to the plugin by protoc
 *
 * @author Florian Enner
 * @since 18 Jan 2023
 */
@Getter
class PluginOptions {

    PluginOptions(PluginProtos.CodeGeneratorRequest request) {
        final Map<String, String> map = ParserUtil.getGeneratorParameters(request);
        indentString = parseIndentString(map.getOrDefault("indent", "2"));
        replacePackageFunction = parseReplacePackage(map.get("replace_package"));
        expectedIncomingOrder = ExpectedIncomingOrder.parseFromString(map.getOrDefault("input_order", "quickbuf"));
        storeUnknownFieldsEnabled = parseBoolean(map.getOrDefault("store_unknown_fields", "false"));
        allocationStrategy = AllocationStrategy.parseFromString(map.getOrDefault("allocation", "eager"));
        extensionSupport = ExtensionSupport.parseFromString(map.getOrDefault("extensions", "disabled"));
        enforceHasChecksEnabled = parseBoolean(map.getOrDefault("enforce_has_checks", "false"));
        tryGetAccessorsEnabled = parseBoolean(map.getOrDefault("java8_optional", "false"));
    }

    enum ExpectedIncomingOrder {
        Quickbuf, // parsing messages from Quickbuf
        AscendingNumber, // parsing messages from official protobuf bindings
        None; // parsing messages from unknown sources

        static ExpectedIncomingOrder parseFromString(String string) {
            switch (string.toLowerCase()) {
                case "quickbuf":
                    return Quickbuf;
                case "number":
                    return AscendingNumber;
                case "random":
                case "none":
                    return None;
            }
            throw new GeneratorException("'input_order' parameter accepts ['quickbuf', 'number', 'random']. Found: " + string);
        }

        @Override
        public String toString() {
            switch (this) {
                case Quickbuf:
                    return "QuickBuffers";
                case AscendingNumber:
                    return "Sorted by Field Numbers";
                default:
                    return name();
            }
        }
    }

    enum AllocationStrategy {
        Lazy,
        Eager;

        static AllocationStrategy parseFromString(String string) {
            switch (string.toLowerCase()) {
                case "eager":
                    return Eager;
                case "lazy":
                    return Lazy;
            }
            throw new GeneratorException("'allocation' parameter accepts ['eager', 'lazy']. Found: " + string);
        }

    }

    enum ExtensionSupport {
        Disabled, Embedded;

        static ExtensionSupport parseFromString(String string) {
            switch (string.toLowerCase()) {
                case "embedded":
                    return Embedded;
                case "disabled":
                    return Disabled;
            }
            throw new GeneratorException("'extensions' parameter accepts ['disabled', 'embedded']. Found: " + string);
        }

    }

    private static String parseIndentString(String indent) {
        switch (indent) {
            case "8":
                return "        ";
            case "4":
                return "    ";
            case "2":
                return "  ";
            case "tab":
                return "\t";
        }
        throw new GeneratorException("Expected 2,4,8,tab. Found: " + indent);
    }

    Function<String, String> parseReplacePackage(String replaceOption) {
        // leave as is
        if (replaceOption == null) {
            return str -> str;
        }

        // parse "pattern=replacement"
        String[] parts = replaceOption.split("=");
        if (parts.length != 2)
            throw new GeneratorException("'replace_package' expects 'pattern=replacement'. Found: '" + replaceOption + "'");

        // regex replace
        Pattern pattern = Pattern.compile(parts[0]);
        String replacement = parts[1];
        return input -> pattern
                .matcher(input)
                .replaceAll(replacement);
    }

    final ExpectedIncomingOrder expectedIncomingOrder;
    final AllocationStrategy allocationStrategy;
    final ExtensionSupport extensionSupport;
    final String indentString;
    final boolean storeUnknownFieldsEnabled;
    final boolean enforceHasChecksEnabled;
    final boolean tryGetAccessorsEnabled;
    final Function<String, String> replacePackageFunction;

}
