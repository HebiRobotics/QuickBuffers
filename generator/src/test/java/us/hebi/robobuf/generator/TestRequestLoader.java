/*-
 * #%L
 * robobuf-generator
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

package us.hebi.robobuf.generator;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static us.hebi.robobuf.generator.Preconditions.*;

/**
 * @author Florian Enner
 * @since 06 Aug 2019
 */
public class TestRequestLoader {

    @Test
    public void testAllAvailable() {
        getRequiredRequest();
        getImportRequest();
        getAllTypesRequest();
        getRepeatedPackablesRequest();
    }

    public static CodeGeneratorRequest getRequiredRequest() {
        return getRequest("required");
    }

    public static CodeGeneratorRequest getImportRequest() {
        return getRequest("import");
    }

    public static CodeGeneratorRequest getAllTypesRequest() {
        return getRequest("allTypes");
    }

    public static CodeGeneratorRequest getRepeatedPackablesRequest() {
        return getRequest("repeatedPackables");
    }

    private static CodeGeneratorRequest getRequest(String name) {
        try {
            InputStream is = TestRequestLoader.class.getResourceAsStream(name + ".request");
            return CodeGeneratorRequest.parseFrom(checkNotNull(is));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
