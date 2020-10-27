package sqinject.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher
import java.util.regex.Pattern

class SqRGenerator extends DefaultTask {

    @TaskAction
    public void generateSqR() {
        def android = project.extensions.android
        project.plugins.all {
            if (it instanceof LibraryPlugin) {
                this.configureR2Generation(project, android.libraryVariants)
            } else if (it instanceof AppPlugin) {
                this.configureR2Generation(project, android.applicationVariants)
            }
        }
    }

    public String getPackageName(BaseVariant variant) {
        def manifest = variant.sourceSets.get(0).manifestFile
        // According to the documentation, the earlier files in the list are meant to be overridden by the later ones.
        // So the first file in the sourceSets list should be main.
        def result = new XmlSlurper().parse(manifest)
        return result.getProperty("@package").toString()
    }

    public void configureR2Generation(Project project, DomainObjectSet<? extends BaseVariant> variants) {
        variants.all { variant ->

            /***
             * rPackage：获得包名，以便获取路径
             * 例子：com.sq.mobile.sqinject_gradle_plugin
             * */
            def rPackage = this.getPackageName(variant)
            println(rPackage)
            rPackage = rPackage.replace(".", "/")

            /***
             * 获得资源任务（the Android Resources processing task）
             * 以便用来读取R文件等
             * */
            //Returns the variant outputs《BaseVariantOutput》
            def variantOutput = variant.outputs.first()
            //Returns the Android Resources processing task.《TaskProvider》
            def processResources = variantOutput.processResourcesProvider.get()
            def rFiles = project.files(processResources.textSymbolOutputFile).builtBy(processResources)

            /**
             * R.txt路径：sqinjectgradleplugin/app/build/intermediates/symbols/debug/R.txt
             * */
            def RFilePath = rFiles.singleFile.absolutePath
            println("R.txt path: " + RFilePath)

            /**
             * sqinjectgradleplugin/app/build/generated/not_namespaced_r_class_sources/debug/r
             * */
            def outputDir = processResources.getSourceOutputDir()
            println("processResources output Dir: " + outputDir.absolutePath)

            /**
             * R.java路径
             * 例子：
             * RClassFile = sqinjectgradleplugin/app/build/generated/not_namespaced_r_class_sources/debug/r
             *  + com/sq/mobile/sqinject_gradle_plugin
             *  + R.java
             * */
            File RClassFile = new File(outputDir.absolutePath + File.separator + rPackage + File.separator + "R.java")
            if (!rFiles.singleFile.exists() && !RClassFile.exists() && !(outputDir.exists() && outputDir.name.contains("R.jar"))) {
                println(rFiles.singleFile.absolutePath + "不存在")
                println(RClassFile.absolutePath + "不存在")
                if (outputDir.name.contains("R.jar")) {
                    println(outputDir.absolutePath + "不存在")
                }
                return
            }
            if (RClassFile.exists()) {
                /***
                 * R.java 存在
                 * 那么通过 R.java 文件生成 我们自己的R《SqR》
                 * **/
                println("use outputDir generate SqR")
                //tempFile = sqinjectgradleplugin/app/build/generated/not_namespaced_r_class_sources/debug/r/com/sq/mobile/sqinject_gradle_plugin/R.java
                File tempFile = new File(outputDir.absolutePath + File.separator + rPackage + File.separator + "R.java");
                println("R file path: " + tempFile.absolutePath)
                rFileContent = tempFile.text
                Pattern pattern = Pattern.compile("public static(.*?) int (.*?)=(.*?);")
                Matcher matcher = pattern.matcher(rFileContent);
                // 将原先的R文件的int换成String，并将其值使用变量名赋值
                while (matcher.find()) {
                    String replace = "public static final String " + matcher.group(2) + " = \"" + matcher.group(2) + "\";";
                    rFileContent = rFileContent.replaceAll(matcher.group(), replace)
                }
                rFileContent = rFileContent.replaceAll("class R", "class SqR")
            } else {
                println("use R.txt generate SqR")
                rFileContent = "package " + this.getPackageName(variant) + ";\n" + "public final class SqR {" + "\n";
                try {
                    Map<String, List<String>> map = new HashMap<>();
                    FileInputStream fileInputStream = new FileInputStream(RFilePath);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream))
                    String line = null
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line != null) {
                            String[] words = line.split(" ");
                            if (words.length > 2) {
                                String type = words[1];
                                String field = words[2];
                                if (map.get(type) == null) {
                                    List<String> list = new ArrayList<>();
                                    list.add(field);
                                    map.put(type, list)
                                } else {
                                    map.get(type).add(field)
                                }
                            }
                        }
                    }
                    if (map != null) {
                        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                            String innerClass = "    public static class " + entry.getKey() + "{\n";
                            for (String field : entry.getValue()) {
                                innerClass += "        public static final String " + field + " = " + "\"" + field + "\";\n";
                            }
                            innerClass += "    }\n";
                            rFileContent += innerClass;
                        }
                    }
                    rFileContent += "}";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 修改R文件的类名为SqResource
            def newClassName = "SqR"
            def newClassPath = project.buildDir.absolutePath + File.separator + "generated/source/sqr/" + variant.dirName + "/" + rPackage + "/SqR.java"
            println "输出文件：" + newClassPath
            this.write(newClassPath, rFileContent)
            println "自动生成SqR文件成功！"

        }
    }

    //写文件
    void write(String filePath, String content) {
        File file = new File(filePath)
        if (!file.exists()) {
            file.getParentFile().mkdirs()
            file.createNewFile()
        }
        BufferedWriter bw = null;

        try {
            // 根据文件路径创建缓冲输出流
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
            // 将内容写入文件中
            bw.write(content);
            bw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    bw = null;
                }
            }
        }
    }


}