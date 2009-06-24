package org.world.hello.apps.classloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.world.hello.apps.Logger;
import org.world.hello.apps.DynamicRuntime;
import org.world.hello.apps.exceptions.UnexpectedException;
import org.world.hello.apps.vfs.VirtualFile;

/**
 * Application classes container.
 */
public class ApplicationClasses {

    ApplicationCompiler compiler = new ApplicationCompiler(this);
    Map<String, ApplicationClass> classes = new HashMap<String, ApplicationClass>();

    /**
     * Cleat the classes cache
     */
    public void clear() {
        classes = new HashMap<String, ApplicationClass>();
    }

    /**
     * Get a class by name
     * @param name The fully qualified class name
     * @return The ApplicationClass or null
     */
    public ApplicationClass getApplicationClass(String name) {
        if (!classes.containsKey(name) && getJava(name) != null) {
            classes.put(name, new ApplicationClass(name));
        }
        return classes.get(name);
    }

    /**
     * Retrieve all application classes assignable to this class.
     * @param clazz The superclass, or the interface.
     * @return A list of application classes.
     */
    public List<ApplicationClass> getAssignableClasses(Class clazz) {
        List<ApplicationClass> results = new ArrayList<ApplicationClass>();
        for (ApplicationClass applicationClass : classes.values()) {
            try {
                DynamicRuntime.classloader.loadClass(applicationClass.name);
            } catch (ClassNotFoundException ex) {
                throw new UnexpectedException(ex);
            }
            if (clazz.isAssignableFrom(applicationClass.javaClass) && !applicationClass.javaClass.getName().equals(clazz.getName())) {
                results.add(applicationClass);
            }
        }
        return results;
    }

    /**
     * Retrieve all application classes with a specific annotation.
     * @param clazz The annotation class.
     * @return A list of application classes.
     */
    public List<ApplicationClass> getAnnotatedClasses(Class clazz) {
        List<ApplicationClass> results = new ArrayList<ApplicationClass>();
        for (ApplicationClass applicationClass : classes.values()) {
            try {
                DynamicRuntime.classloader.loadClass(applicationClass.name);
            } catch (ClassNotFoundException ex) {
                throw new UnexpectedException(ex);
            }
            if (applicationClass.javaClass.isAnnotationPresent(clazz)) {
                results.add(applicationClass);
            }
        }
        return results;
    }

    /**
     * All loaded classes.
     * @return All loaded classes
     */
    public List<ApplicationClass> all() {
        return new ArrayList<ApplicationClass>(classes.values());
    }

    /**
     * Does this class is already loaded ?
     * @param name The fully qualified class name
     * @return
     */
    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }

    /**
     * Represent a application class
     */
    public class ApplicationClass {

        /**
         * The fully qualified class name
         */
        public String name;
        /**
         * A reference to the java source file
         */
        public VirtualFile javaFile;
        /**
         * The Java source
         */
        public String javaSource;
        /**
         * The compiled byteCode
         */
        public byte[] javaByteCode;
        /**
         * The enhanced byteCode
         */
        public byte[] enhancedByteCode;
        /**
         * The in JVM loaded class
         */
        public Class javaClass;
        /**
         * Last time than this class was compiled
         */
        public Long timestamp = 0L;
        /**
         * Is this class compiled
         */
        boolean compiled;
        /**
         * Signatures checksum
         */
        public int sigChecksum;

        public ApplicationClass(String name) {
            this.name = name;
            this.javaFile = getJava(name);
            this.refresh();
        }

        /**
         * Need to refresh this class !
         */
        public void refresh() {
            this.javaSource = this.javaFile.contentAsString();
            this.javaByteCode = null;
            this.enhancedByteCode = null;
            this.compiled = false;
            this.timestamp = 0L;
        }

        /**
         * Enhance this class
         * @return the enhanced byteCode
         */
        public byte[] enhance() {


            return this.enhancedByteCode;

        }

        /**
         * Is this class already compiled but not defined ?
         * @return
         */
        public boolean isDefinable() {
            return compiled && javaClass != null;
        }

        /**
         * Compile the class from Java source
         * @return
         */
        public byte[] compile() {
            long start = System.currentTimeMillis();
            compiler.compile(new String[]{this.name});
            Logger.trace("%sms to compile class %s", System.currentTimeMillis() - start, name);
            return this.javaByteCode;
        }

        /**
         * Unload the class
         */
        public void uncompile() {
            this.javaClass = null;
        }

        /**
         * Call back when a class is compiled.
         * @param code The bytecode.
         */
        public void compiled(byte[] code) {
            javaByteCode = code;
            enhancedByteCode = code;
            compiled = true;
            this.timestamp = this.javaFile.lastModified();
        }
    }

    // ~~ Utils
    /**
     * Retrieve the corresponding source file for a given class name.
     * It handle innerClass too !
     * @param name The fully qualified class name 
     * @return The virtualFile if found
     */
    public VirtualFile getJava(String name) {
        String fileName = name;
        if (fileName.contains("$")) {
            fileName = fileName.substring(0, fileName.indexOf("$"));
        }
        fileName = fileName.replace(".", "/") + ".java";
        for (VirtualFile path : DynamicRuntime.javaPath) {
            VirtualFile javaFile = path.child(fileName);
            if (javaFile.exists()) {
                return javaFile;
            }
        }
        return null;
    }
}
