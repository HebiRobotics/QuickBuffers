package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import us.hebi.robobuf.compiler.RequestInfo;

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

    public abstract void generateMergingCodeFromPacked(MethodSpec.Builder method);

    public abstract void generateSerializationCode(MethodSpec.Builder method);

    public abstract void generateSerializedSizeCode(MethodSpec.Builder method);

    public abstract void generateEqualsCode(MethodSpec.Builder method);

    public abstract void generateHashCodeCode(MethodSpec.Builder method);


    protected FieldGenerator(RequestInfo.FieldInfo info) {
        this.info = info;
        typeName = info.getTypeName();

        // Common-variable map for named arguments
        m.put("name", info.getLowerName());
//        m.put("Name", info.getUpperName());
        m.put("getHas", info.getHasBit());
        m.put("setHas", info.getSetBit());
        m.put("clearHas", info.getClearBit());
        m.put("message", info.getParentType());
        m.put("type", typeName);

    }

    protected final RequestInfo.FieldInfo info;
    protected final TypeName typeName;
    protected HashMap<String, Object> m = new HashMap<>();

}
