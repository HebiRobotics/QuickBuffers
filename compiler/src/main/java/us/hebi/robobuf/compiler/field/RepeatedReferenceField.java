package us.hebi.robobuf.compiler.field;

import com.squareup.javapoet.MethodSpec;
import us.hebi.robobuf.compiler.RequestInfo;

/**
 * @author Florian Enner
 * @since 13 Aug 2019
 */
public abstract class RepeatedReferenceField extends RepeatedField {

    protected RepeatedReferenceField(RequestInfo.FieldInfo info) {
        super(info);
    }

}
