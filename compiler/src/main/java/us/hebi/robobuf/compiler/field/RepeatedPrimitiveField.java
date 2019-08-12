package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.GeneratorException;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import javax.lang.model.element.Modifier;

/**
 * @author Florian Enner
 * @since 10 Aug 2019
 */
public class RepeatedPrimitiveField extends FieldGenerator {

    protected RepeatedPrimitiveField(RequestInfo.FieldInfo info) {
        super(info);
        m.put("arrayType", getArrayType());
        m.put("maxExpansion", getMaximumExpansionSize());
        m.put("maxExpansionMinusOne", getMaximumExpansionSize() - 1);
    }

    private int getMaximumExpansionSize() {
        switch (info.getDescriptor().getType()) {

            case TYPE_BOOL:
                return 1;

            case TYPE_DOUBLE:
            case TYPE_SFIXED64:
            case TYPE_FIXED64:
                return 8;

            case TYPE_FLOAT:
            case TYPE_SFIXED32:
            case TYPE_FIXED32:
                return 4;

            // may expand to 5 bytes
            case TYPE_SINT32:
            case TYPE_UINT32:
            case TYPE_INT32:
                return 5;

            // may expand to 10 bytes
            case TYPE_SINT64:
            case TYPE_UINT64:
            case TYPE_INT64:
                return 10;

            default:
                throw new IllegalStateException("Unexpected value: " + info.getDescriptor().getType());
        }
    }

    protected TypeName getArrayType() {
        if (info.getTypeName() == TypeName.DOUBLE)
            return RuntimeClasses.REPEATED_DOUBLE_CLASS;
        if (info.getTypeName() == TypeName.FLOAT)
            return RuntimeClasses.REPEATED_FLOAT_CLASS;
        if (info.getTypeName() == TypeName.LONG)
            return RuntimeClasses.REPEATED_LONG_CLASS;
        if (info.getTypeName() == TypeName.INT)
            return RuntimeClasses.REPEATED_INT_CLASS;
        if (info.getTypeName() == TypeName.BOOLEAN)
            return RuntimeClasses.REPEATED_BOOLEAN_CLASS;
        throw new GeneratorException("Unknown array type for: " + typeName);
    }

    @Override
    public void generateMemberFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(getArrayType(), info.getFieldName(), Modifier.FINAL, Modifier.PRIVATE)
                .initializer("new $T()", getArrayType())
                .build());
    }

    @Override
    public void generateClearCode(MethodSpec.Builder method) {
        method.addNamedCode("$field:N.clear();\n", m);
    }

    @Override
    public void generateSerializationCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                "output.write$capitalizedType:L($number:L, $field:N.get(i));\n" +
                "$<}\n", m);
    }

    @Override
    public void generateComputeSerializedSizeCode(MethodSpec.Builder method) {
        method.addNamedCode("" +
                "for (int i = 0; i < $field:N.length(); i++) {$>\n" +
                "size += $computeClass:T.compute$capitalizedType:LSize($number:L, $field:N.get(i));\n" +
                "}$<\n", m);
    }

    @Override
    public void generateCopyFromCode(MethodSpec.Builder method) {
        method.addNamedCode("$field:N.copyFrom(other.$field:N);\n", m);
    }

    @Override
    public void generateMergingCode(MethodSpec.Builder method) {
        // non-packed -> items can be arbitrarily located on the wire. We don't need to
        // optimizes as much with array size as the array should already be large enough
        // in steady state, but we should check at least the next tag whether it's the same
        // type.
        method.addNamedCode("$field:N.add(input.read$capitalizedType:L());\n", m);
    }

   /* @Override
    public void generateMergingCodeFromPacked(MethodSpec.Builder method) {
        // We don't know exactly how many items there are for non-fixed width types,
        // but we at least know how many items there will be at least. The first iteration
        // will result in quite a few allocations, but steady state those should be gone.
        // TODO: allow a direct copy from input to RepeatedArray?
        // TODO: e.g. readPacked32(int numBytes, RepeatedFloat store) -> Unsafe.copyMemory?
        method.addNamedCode("" +
                "final int length = input.readRawVarint32();\n" +
                "$field:N.ensureSpace((length + $maxExpansionMinusOne:L) / $maxExpansion:L);\n" + // 32/64 bit values should never have a rest?
                "final int limit = input.pushLimit(length);\n" +
                "while (input.getBytesUntilLimit() > 0) {$>\n" +
                "$field:N.add(input.read$capitalizedType:L());\n" +
                "$<}\n" +
                "input.popLimit(limit);\n", m);
    }*/

    @Override
    protected void generateSetter(TypeSpec.Builder type) {

    }

    @Override
    protected void generateGetter(TypeSpec.Builder type) {

    }

}
