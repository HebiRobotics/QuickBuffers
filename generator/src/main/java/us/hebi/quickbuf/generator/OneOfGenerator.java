/*-
 * #%L
 * quickbuf-generator
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
        int[] bitset = BitField.generateBitset(fields);

        // Checks if any has state is true
        MethodSpec.Builder has = MethodSpec.methodBuilder(info.getHazzerName())
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return $L", BitField.hasAnyBit(bitset));

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
                int[] otherBits = BitField.generateBitset(otherFields);

                MethodSpec.Builder clearOthers = MethodSpec.methodBuilder(field.getClearOtherOneOfName())
                        .addModifiers(Modifier.PRIVATE)
                        .beginControlFlow("if ($L)", BitField.hasAnyBit(otherBits));

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
