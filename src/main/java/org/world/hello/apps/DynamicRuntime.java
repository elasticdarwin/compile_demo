package org.world.hello.apps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.world.hello.apps.classloading.ApplicationClasses;
import org.world.hello.apps.classloading.ApplicationClassloader;
import org.world.hello.apps.exceptions.DynamicRuntimeException;
import org.world.hello.apps.exceptions.UnexpectedException;
import org.world.hello.apps.libs.IO;
import org.world.hello.apps.vfs.VirtualFile;

/**
 * Main framework class
 */
public class DynamicRuntime {

    /**
     * 2 modes
     */
    public enum Mode {

        DEV, PROD
    }
    /**
     * Is the application started
     */
    public static boolean started = false;
    /**
     * The framework ID
     */
    public static String id;
    /**
     * The application mode
     */
    public static Mode mode;
    /**
     * The application root
     */
    public static File applicationPath = null;
    /**
     * tmp dir
     */
    public static File tmpDir = null;
    /**
     * The framework root
     */
//    public static File frameworkPath = null;
    /**
     * All loaded application classes
     */
    public static ApplicationClasses classes = new ApplicationClasses();
    /**
     * The application classLoader
     */
    public static ApplicationClassloader classloader;
    /**
     * All paths to search for files
     */
    public static List<VirtualFile> roots = new ArrayList<VirtualFile>();
    /**
     * All paths to search for Java files
     */
    public static List<VirtualFile> javaPath;
    /**
     * All paths to search for templates files
     */
    public static List<VirtualFile> templatesPath;
    /**
     * Main routes file
     */
    public static VirtualFile routes;
    /**
     * Plugin routes files
     */
    public static Map<String, VirtualFile> modulesRoutes;
    /**
     * The main application.conf
     */
    public static VirtualFile conf;
    /**
     * The app configuration (already resolved from the framework id)
     */
    public static Properties configuration;
    /**
     * The last time than the application has started
     */
    public static long startedAt;
    /**
     * The list of supported locales
     */
    public static List<String> langs = new ArrayList<String>();
    /**
     * The very secret key
     */
    public static String secretKey;
    /**
     * Modules
     */
    public static List<VirtualFile> modules = new ArrayList<VirtualFile>();
    /**
     * Framework version
     */
    public static String version = null;

    /**
     * Init the framework
     * @param root The application path
     * @param id The framework id to use
     */
    public static void init(File root, String id) {
        // Simple things
        DynamicRuntime.id = id;
        DynamicRuntime.started = false;
        DynamicRuntime.applicationPath = root;
        DynamicRuntime.version = "v0.9";

//        initStaticStuff();

//        frameworkPath = new File(".").getAbsoluteFile();

        Logger.info("");
        Logger.info("Starting %s", root.getAbsolutePath());

        // Read the configuration file
        readConfiguration();

        // Mode
        mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());

        // Configure logs
        String logLevel = configuration.getProperty("application.log", "INFO");
        Logger.setUp(logLevel);

        // Build basic java source path
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        roots.add(appRoot);
        javaPath = new ArrayList<VirtualFile>();
        javaPath.add(appRoot.child("app"));
        if (id.equals("test")) {
            javaPath.add(appRoot.child("test"));
        }


        // Enable a first classloader
        classloader = new ApplicationClassloader();


        if (mode == Mode.PROD) {
            if (preCompile()) {
                start();
            } else {
                return;
            }
        } else {
            Logger.warn("You're running Play! in DEV mode");
        }
        // Yop
        Logger.info("Application '%s' is ready !", configuration.getProperty("application.name", ""));
    }

    static void readConfiguration() {
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        conf = appRoot.child("conf/application.conf");
        try {
            configuration = IO.readUtf8Properties(conf.inputstream());
        } catch (IOException ex) {
            Logger.fatal("Cannot read application.conf");
            System.exit(0);
        }
        // Ok, check for instance specifics configuration
        Properties newConfiguration = new Properties();
        Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
        for (Object key : configuration.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (!matcher.matches()) {
                newConfiguration.put(key, configuration.get(key).toString().trim());
            }
        }
        for (Object key : configuration.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(id)) {
                    newConfiguration.put(matcher.group(2), configuration.get(key).toString().trim());
                }
            }
        }
        configuration = newConfiguration;
        // Resolve ${..}
        pattern = Pattern.compile("\\$\\{([^}]+)}");
        for (Object key : configuration.keySet()) {
            String value = configuration.getProperty(key.toString());
            Matcher matcher = pattern.matcher(value);
            StringBuffer newValue = new StringBuffer();
            while (matcher.find()) {
                String jp = matcher.group(1);
                String r = System.getProperty(jp);
                if (r == null) {
                    Logger.warn("Cannot replace %s in configuration (%s=%s)", jp, key, value);
                    continue;
                }
                matcher.appendReplacement(newValue, System.getProperty(jp).replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            configuration.setProperty(key.toString(), newValue.toString());
        }

    }

    /**
     * Start the application.
     * Recall to restart !
     */
    public static synchronized void start() {
        try {
            if (started) {
                Logger.info("Reloading ...");
                stop();
            }

            if (mode == Mode.DEV) {
                // Need a new classloader
                classloader = new ApplicationClassloader();

            }

            // Reload configuration
            readConfiguration();
            if (configuration.getProperty("play.tmp", "tmp").equals("none")) {
                tmpDir = null;
                Logger.debug("No tmp folder will be used (play.tmp is set to none)");
            } else {
                tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
                if (!tmpDir.isAbsolute()) {
                    tmpDir = new File(applicationPath, tmpDir.getPath());
                }
                try {
                    tmpDir.mkdirs();
                } catch (Throwable e) {
                    tmpDir = null;
                    Logger.warn("No tmp folder will be used (cannot create the tmp dir)");
                }
            }

            // Configure logs
            String logLevel = configuration.getProperty("application.log", "INFO");
            Logger.setUp(logLevel);

            // Locales
            langs = Arrays.asList(configuration.getProperty("application.langs", "").split(","));
            if (langs.size() == 1 && langs.get(0).trim().equals("")) {
                langs = new ArrayList<String>();
            }


            // SecretKey
            secretKey = configuration.getProperty("application.secret", "").trim();
            if (secretKey.equals("")) {
                Logger.warn("No secret key defined. Sessions will not be encrypted");
            }

            // Try to load all classes
            DynamicRuntime.classloader.getAllClasses();



            // We made it
            started = true;
            startedAt = System.currentTimeMillis();


        } catch (DynamicRuntimeException e) {
            started = false;
            throw e;
        } catch (Exception e) {
            started = false;
            throw new UnexpectedException(e);
        }
    }

    /**
     * Stop the application
     */
    public static synchronized void stop() {
        started = false;
    }

    static boolean preCompile() {
        try {
            Logger.info("Precompiling ...");
            long start = System.currentTimeMillis();
            classloader.getAllClasses();
            Logger.trace("%sms to precompile the Java stuff", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            Logger.trace("%sms to precompile the templates", System.currentTimeMillis() - start);
            return true;
        } catch (Throwable e) {
            Logger.error(e, "Cannot start in PROD mode with errors");
            try {
                System.exit(-1);
            } catch (Exception ex) {
                // Will not work in some application server
            }
            return false;
        }
    }

    /**
     * Detect sources modifications
     */
    public static synchronized void detectChanges() {
        if (mode == Mode.PROD) {
            return;
        }
        try {
            classloader.detectChanges();
            if (conf.lastModified() > startedAt) {
                start();
                return;
            }
            if (!DynamicRuntime.started) {
                throw new RuntimeException("Not started");
            }
        } catch (DynamicRuntimeException e) {
            throw e;
        } catch (Exception e) {
            // We have to do a clean refresh
            start();
        }
    }

    /**
     * Search a VirtualFile in all loaded applications and plugins
     * @param path Relative path from the applications root
     * @return The virtualFile or null
     */
    public static VirtualFile getVirtualFile(String path) {
        return VirtualFile.search(roots, path);
    }

    /**
     * Search a File in the current application
     * @param path Relative path from the application root
     * @return The file even if it doesn't exist
     */
    public static File getFile(String path) {
        return new File(applicationPath, path);
    }
    /**
     * Allow some code to run very eraly in Play! - Use with caution !
     */
//    public static void initStaticStuff() {
//        // Play! plugings
//        Enumeration<URL> urls = null;
//        try {
//            urls = Play.class.getClassLoader().getResources("play.static");
//        } catch (Exception e) {
//        }
//        while (urls != null && urls.hasMoreElements()) {
//            URL url = urls.nextElement();
//            try {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    try {
//                        Class.forName(line);
//                    } catch (Exception e) {
//                        System.out.println("! Cannot init static : " + line);
//                    }
//                }
//            } catch (Exception ex) {
//                Logger.error(ex, "Cannot load %s", url);
//            }
//        }
//    }
}
