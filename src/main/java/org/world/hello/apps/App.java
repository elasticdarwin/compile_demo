package org.world.hello.apps;

import java.io.File;
import org.world.hello.apps.Invoker.Invocation;
import org.world.hello.apps.exceptions.DynamicRuntimeException;
import org.world.hello.apps.exceptions.UnexpectedException;
import org.world.hello.apps.libs.Java;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws InterruptedException {
        DynamicRuntime.init(new File("src/main/resources"), "");

        while (true) {
            Thread.currentThread().sleep(2000);
            Invoker.invoke(new MyInvocation());
        }
    }
}

class MyInvocation extends Invocation {

    @Override
    public void onException(Throwable e) {
        Logger.info(e, "someError caused.");

        if (e instanceof DynamicRuntimeException) {
            throw (DynamicRuntimeException) e;
        }
        throw new UnexpectedException(e);
    }

    @Override
    public void execute() throws Exception {

        Class clazz = DynamicRuntime.classloader.getClass("Main");

        Logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        Java.invokeStatic(clazz, "start");
        Logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }
}
