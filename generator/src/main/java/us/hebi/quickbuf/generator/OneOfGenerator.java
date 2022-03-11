/*-
 * #%L
 * quickbuf-generator
 * %%
 * Copyright (C) 2019 HEBI Robotics
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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.quickbuf.generator.RequestInfo.FieldInfo;
import us.hebi.quickbuf.generator.RequestInfo.OneOfInfo;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Florian Enner
 * @since 19 Nov 2019
 */
public class OneOfGenerator {

    public OneOfGenerator(OneOfInfo info) {
        this.info = info;
        this.fields = info.getFields();
    }

    protected void generateMemberMethods(TypeSpec.Builder type) {
        // Checks if any has state is true
        MethodSpec.Builder has = MethodSpec.methodBuilder(info.getHazzerName())
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return $L", BitField.hasAnyBit(fields));

        // Method that clears all fields
        MethodSpec.Builder clear = MethodSpec.methodBuilder(info.getClearName())
                .addModifiers(Modifier.PUBLIC)
                .returns(info.getParentType())
                .beginControlFlow("if ($L())", info.getHazzerName());

        for (FieldInfo field : fields) {
            clear.addStatement("$N()", field.getClearName());
        }
        clear.endControlFlow();
        clear.addStatement("return this");

        type.addMethod(has.build());
        type.addMethod(clear.build());

        // Add a utility method that clears all but one fields
        if (fields.size() > 1) {
            for (FieldInfo field : fields) {

                List<FieldInfo> otherFields = fields.stream()
                        .filter(info -> info != field)
                        .collect(Collectors.toList());

                MethodSpec.Builder clearOthers = MethodSpec.methodBuilder(field.getClearOtherOneOfName())
                        .addModifiers(Modifier.PRIVATE)
                        .beginControlFlow("if ($L)", BitField.hasAnyBit(otherFields));

                for (FieldInfo otherField : otherFields) {
                    clearOthers.addStatement("$N()", otherField.getClearName());
                }
                clearOthers.endControlFlow();
                type.addMethod(clearOthers.build());

            }
        }

    }

    final OneOfInfo info;
    final List<FieldInfo> fields;

}
