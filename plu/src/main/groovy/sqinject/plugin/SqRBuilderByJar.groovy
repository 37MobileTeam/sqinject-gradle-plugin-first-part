package sqinject.plugin

import java.lang.reflect.Field
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * 根据R.jar生成SqR.java
 */
class SqRBuilderByJar {

    private static Map<String, List<String>> classMap = new HashMap<>();

    private static String pkgName;

    public static String build(String RJarPath, String packageName){
        try {
            pkgName = packageName
            ModuleClassLoader classLoader = ModuleClassLoader.getInstance()
            URL url = new URL("file:"+RJarPath)
            classLoader.loadJar(url)
            String RInnerClassPrefix = packageName.replace(".", "/") + '''/R$''';
            List<String> RClazzName = filterClassName(RJarPath, RInnerClassPrefix);
            for (String rclassName : RClazzName) {
                rclassName = rclassName.replaceAll("/", ".").replace(".class", "");
                Class RClass = classLoader.loadClass(rclassName);
                if (RClass != null) {
                    readFields(RClass);
                }
            }
            return generateCodeByMap()
        } catch(Exception e) {
            e.printStackTrace()
        }
        return ""
    }

    private static String generateCodeByMap(){
        if (classMap != null && classMap.size() > 0) {
            String rFileContent = "package " + pkgName + ";\n" + "public final class SqR {" + "\n";
            for (Map.Entry<String, List<String>> entry : classMap) {
                String rInnerClass = "    public static class " + entry.getKey() + "{\n";
                for (String field : entry.getValue()) {
                    rInnerClass += "        public static final String " + field + " = " + "\"" + field + "\";\n";
                }
                rInnerClass += "    }\n"
                rFileContent += rInnerClass
            }
            rFileContent += "}";
            return rFileContent;
        }
        return ""
    }

    private  static List<String> filterClassName(String jarPath ,String name) throws IOException {
        List<String> list = new ArrayList<>()
        JarFile jf = new JarFile(jarPath)
        Enumeration<JarEntry> jfs = jf.entries()
        StringBuffer sb  = new StringBuffer()
        while(jfs.hasMoreElements())
        {
            JarEntry jfn = jfs.nextElement()
            if (jfn.getName().contains(name)) {
                list.add(jfn.getName())
            }
        }
        return list
    }

    private static void readFields(Class clazz) {
        System.out.println(clazz.getName())
        String name = clazz.getName().split("\\\$")[1];
        List<String> list = new ArrayList<>()
        Field[] fields = clazz.getDeclaredFields()
        for (Field field : fields) {
            list.add(field.getName())
        }
        classMap.put(name, list)
    }



}
