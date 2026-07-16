//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.app.common.util;

import java.math.BigInteger;

public class BIUtil {
    public BIUtil() {
    }

    public static boolean isLessThan(BigInteger valueA, BigInteger valueB) {
        return valueA.compareTo(valueB) < 0;
    }
}
