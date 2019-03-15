package it.marbola.jdbc.annotation;

import com.google.common.base.CaseFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD,ElementType.TYPE})
public @interface Field {

	String name() default "";
	CaseFormat caseFormat() default CaseFormat.LOWER_CAMEL;

}
