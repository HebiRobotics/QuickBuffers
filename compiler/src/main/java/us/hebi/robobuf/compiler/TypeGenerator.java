package us.hebi.robobuf.compiler;

import com.squareup.javapoet.TypeSpec;

/**
 * @author Florian Enner
 * @since 07 Aug 2019
 */
public interface TypeGenerator {

    TypeSpec generate();

}
