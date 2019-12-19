package com.jokerwan.apt_demo.test;

import com.jokerwan.apt_demo.MainActivity;

/**
 * Created by JokerWan on 2019-12-19.
 * Function:
 */
public class XXXActivity$$ARouter {

    public static Class<?> findTargetClass(String path) {
        if (path.equals("/app/MainActivity")) {
            return MainActivity.class;
        }
        return null;
    }
}
