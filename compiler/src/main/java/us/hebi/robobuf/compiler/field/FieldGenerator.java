package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.GeneratorException;
import us.hebi.robobuf.compiler.RequestInfo;
import us.hebi.robobuf.compiler.RuntimeClasses;

import java.util.HashMap;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public abstract class FieldGenerator {

    public abstract void generateMembers(TypeSpec.Builder type);

    public abstract void generateClearCode(MethodSpec.Builder method);

    public abstract void generateCopyFromCode(MethodSpec.Builder method);

    public abstract void generateMergingCode(MethodSpec.Builder method);

    public void generateMergingCodeFromPacked(MethodSpec.Builder method) {
        throw new GeneratorException("Merging from packed not implemented"); // only for repeated fields
    }

    public abstract void generateSerializationCode(MethodSpec.Builder method);

    public abstract void generateComputeSerializedSizeCode(MethodSpec.Builder method);

    public abstract void generateEqualsCode(MethodSpec.Builder method);

    public final void generateHashCodeCode(MethodSpec.Builder method){
        throw new GeneratorException("hashCode() not supported"); // not yet needed
    }

    public int getTag() {
        return info.getTag();
    }

    protected FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        typeName = info.getTypeName();

        // Common-variable map for named arguments
        m.put("name", info.getLowerName());
        m.put("getHas", info.getHasBit());
        m.put("setHas", info.getSetBit());
        m.put("clearHas", info.getClearBit());
        m.put("message", info.getParentType());
        m.put("type", typeName);
        m.put("number", info.getNumber());
        m.put("tag", info.getTag());
        m.put("capitalizedType", RuntimeClasses.getCapitalizedType(info.getDescriptor().getType()));
        m.put("computeClass", RuntimeClasses.PROTO_DEST);

    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;
    protected HashMap<String, Object> m = new HashMap<>();

}
