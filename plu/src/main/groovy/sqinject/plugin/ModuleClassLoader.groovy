package sqinject.plugin;

import java.lang.reflect.Method

public class ModuleClassLoader extends URLClassLoader {

    private static ModuleClassLoader instance;
    private static URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    private static final Method ADD_URL = initAddMethod();

    public ModuleClassLoader(URL[] urls) {
        super(urls)
    }

    public static ModuleClassLoader getInstance() {
        if (instance == null) {
            instance = new ModuleClassLoader([] as URL[]);
        }
        return instance;
    }

    private static Method initAddMethod() {
        try {
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true)
            return addUrl;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e)
        }
    }

    public void loadJar(URL url) {
        try {
            ADD_URL.invoke(classLoader, url)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

}
