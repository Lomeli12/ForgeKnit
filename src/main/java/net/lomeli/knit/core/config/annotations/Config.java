package net.lomeli.knit.core.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Config {

    double minValue() default Double.MIN_VALUE;

    double maxValue() default Double.MAX_VALUE;

    String comment() default "";

    String translation() default "";

    String category() default "general";

    String categoryComment() default "";

    String categoryTranslation() default "";

    boolean requireRestart() default false;
}
