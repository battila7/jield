package jield.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that a method should be transformed into a generator method.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Generator {
}
