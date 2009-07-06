package org.world.hello.apps.classloading;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Enable HotSwap when it's possible.
 */
public class HotswapAgent {

    static Instrumentation instrumentation;
    public static boolean enabled = false;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        HotswapAgent.instrumentation = instrumentation;
        HotswapAgent.enabled = true;
        System.out.println("Object's size is " + instrumentation.getObjectSize(new Object()));
        System.out.println("Integer's size is " + instrumentation.getObjectSize(new Integer(0)));
        System.out.println("Float's size is " + instrumentation.getObjectSize(new Float(0)));
        System.out.println("Long's size is " + instrumentation.getObjectSize(new Long(0)));
        System.out.println("Double's size is " + instrumentation.getObjectSize(new Double(0)));
        System.out.println("String's size is " + instrumentation.getObjectSize(new String()));
        System.out.println("ArrayList's size is " + instrumentation.getObjectSize(new ArrayList()));
        System.out.println("HashMap's size is " + instrumentation.getObjectSize(new HashMap()));

    }

    public static void reload(ClassDefinition... definitions) throws UnmodifiableClassException, ClassNotFoundException {
        instrumentation.redefineClasses(definitions);
    }
}
