package com.jokerwan.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by JokerWan on 2019-12-19.
 * Function: 标记类的路由注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface JRouter {

    /**
     * 详细路由路径（必填），如："/app/MainActivity"
     */
    String path();

    /**
     * 路由组名（选填，如果开发者不填写，可以从path中截取出来）
     */
    String group() default "";
}
