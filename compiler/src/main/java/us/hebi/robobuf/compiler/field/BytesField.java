package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 12 Aug 2019
 */
public class BytesField extends FieldGenerator {

    protected BytesField(RequestInfo.FieldInfo info) {
        super(info);
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(RuntimeClasses.BYTES_STORAGE_CLASS, info.getFieldName())
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", RuntimeClasses.BYTES_STORAGE_CLASS)
                .build());
    }

    @Override
    protected void generateSetter(TypeSpec.Builder type) {

    }

    @Override
    protected void generateGetter(TypeSpec.Builder type) {

    }

}
