package icu.etl.maven;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import icu.etl.util.FileUtils;
import icu.etl.util.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Maven插件，在编译阶，根据编译器的版本号，自动选择合适的JDK适配器方言接口的实现类，复制到源代码包中
 *
 * @author jeremy8551@qq.com
 * @createtime 2023-10-01
 */
@Mojo(name = "jdk", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public class DesignateJDK extends AbstractMojo {

    /**
     * Maven编译插件参数: ${maven.compiler.source} = 1.8
     */
    @Parameter(property = "easyetl.source", defaultValue = "${maven.compiler.source}")
    private String mavenCompilerSource;

    /**
     * ${project.build.sourceDirectory} 项目的主源码目录，默认为 src/main/java
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File mainSourceDir;

    /**
     * 源文件的字符集
     */
    @Parameter(defaultValue = "${maven.compiler.charset}")
    private String sourceEncoding;

    /**
     * 项目的根目录
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File projectBasedir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            this.run();
        } catch (Throwable e) {
            getLog().error("easyetl 插件发生错误", e);
            throw new RuntimeException(e);
        }
    }

    private void run() throws MojoFailureException, IOException {
        Log log = getLog();
        log.info("插件根据JDK版本，自动切换到对应的方言类!");
        log.info("项目的根目录: " + this.projectBasedir.getAbsolutePath());
        log.info("源代码的目录: " + this.mainSourceDir.getAbsolutePath());
        log.info("源代码字符集: " + this.sourceEncoding);
        log.info("源代码编译版本: " + this.mavenCompilerSource);

        // 搜索资源文件所在目录
        File resource = new File(this.mainSourceDir.getParentFile(), "resources");
        if (!resource.exists() || !resource.isDirectory()) {
            throw new MojoFailureException("目录: " + this.mainSourceDir.getAbsolutePath() + " 不存在!");
        }
        log.info("资源文件目录: " + resource.getAbsolutePath());

        // 搜索 JDK适配器实现类所在的目录
        File dir = MavenPluginUtils.search(resource, "JDK", ".txt");
        if (dir == null) {
            throw new MojoFailureException("适配JDK的方言类: JDK*.java 不存在!");
        }
        log.info("JDK适配器所在路径: " + dir.getAbsolutePath());

        // 查询所有方言类
        File[] impls = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File file = new File(dir, name);
                return file.isFile() && MavenPluginUtils.isJDK(name);
            }
        });

        // 按版本号排序
        Arrays.sort(impls, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return MavenPluginUtils.parseVersion(o1.getName()) - MavenPluginUtils.parseVersion(o2.getName());
            }
        });

        List<File> copyclasses = this.copyfiles(this.mainSourceDir, impls, log); // 复制方言接口的实现类
        this.changeIgnorefile(copyclasses);
    }

    /**
     * 因为JDK版本不同，对应的方言实现类也不同
     * 所以需要将方言接口的实现类写入 .gitignore 中，防止自动提交，只保留编译后的class文件
     *
     * @param copyfiles 复制的方言实现类文件
     * @throws MojoFailureException 修改文件发生错误
     */
    private void changeIgnorefile(List<File> copyfiles) throws MojoFailureException, IOException {
        File ignorefile = new File(this.projectBasedir, ".gitignore");
        if (ignorefile.exists() && ignorefile.isFile()) {
            getLog().info("配置规则文件: " + ignorefile.getAbsolutePath());

            Set<String> patterns = JavaFileUtils.readPatterns(copyfiles, this.projectBasedir);
//            getLog().info("patterns: " + StringUtils.toString(patterns));

            Set<String> rules = JavaFileUtils.readIgnorefile(ignorefile, this.sourceEncoding);
//            getLog().info("rules: " + StringUtils.toString(rules));

            patterns.removeAll(rules);
//            getLog().info("left rule: " + StringUtils.toString(patterns));

            if (patterns.isEmpty()) {
                return;
            }

            String ls = FileUtils.readLineSeparator(ignorefile);
            StringBuilder buf = new StringBuilder();
            buf.append(ls);
            buf.append("### 用于过滤JDK适配器方言接口的实现类 ###").append(ls);
            for (String pattern : patterns) {
                buf.append(pattern).append(ls);
            }
            buf.append(ls);
            getLog().info("在规则文件 " + ignorefile.getAbsolutePath() + " 配置如下规则: \n" + buf);
            FileUtils.write(ignorefile, this.sourceEncoding, true, buf);
        }
    }

    /**
     * 将JDK适配器方言接口实现类 {@code files}，复制到目录 {@code dir} 中
     *
     * @param dir   主要源文件的目录, src/main/java
     * @param files JDK适配器方言接口实现类
     * @param log   日志输出接口
     * @return
     * @throws MojoFailureException 发生错误
     */
    private List<File> copyfiles(File dir, File[] files, Log log) throws MojoFailureException, IOException {
        int major = this.getJdkMajor(); // JDK大版本号，如: 5, 6, 7, 8 ..
        log.info("当前Java编译器的大版本号是 " + major);

        List<File> list = new ArrayList<File>(files.length);
        for (File file : files) {
            int version = MavenPluginUtils.parseVersion(file.getName());
            if (version <= major) {
                log.info(file.getAbsolutePath() + ", 对应JDK的大版本号是: " + version);

                String packageName = JavaFileUtils.readPackageName(file, this.sourceEncoding);
                if (packageName == null) {
                    log.warn("读取Java源文件 " + file.getAbsolutePath() + " 中的包名失败！" + this.sourceEncoding);
                    continue;
                }

                File newfile = new File(dir, packageName.replace('.', '/') + "/" + FileUtils.changeFilenameExt(file.getName(), "java"));
                if (newfile.exists() && !newfile.isFile()) {
                    throw new MojoFailureException("JDK适配的方言实现类的目标错误: " + newfile.getAbsolutePath() + " 不是一个有效文件!");
                }

                if (newfile.exists()) {
                    /**
                     * 如果在源代码中JDK适配器方言接口实现类已经存在了
                     * 就判断一下是否有变化：
                     * 如果最近修改了 resources 目录下的类，则用 resources 目录下的类覆盖到源代码中
                     * 如果最近修改了源代码目录下的类信息，则用源代码中的类，覆盖到 resources 目录下
                     */
                    if (newfile.length() != file.length()) {
                        if (newfile.lastModified() >= file.lastModified()) {
                            MavenPluginUtils.copyfile(newfile, file, log);
                        } else {
                            MavenPluginUtils.copyfile(file, newfile, log);
                        }
                    }
                } else {
                    newfile.createNewFile();
                    MavenPluginUtils.copyfile(file, newfile, log);
                }
                list.add(newfile);
            }
        }
        return list;
    }

    /**
     * JDK大版本号，如: 5, 6, 7, 8
     *
     * @return JDK编译器的大版本号
     * @throws MojoFailureException 发生错误
     */
    public int getJdkMajor() throws MojoFailureException {
        String[] array = this.mavenCompilerSource.split("\\.");
        if (array.length == 1) {
            if (StringUtils.isNumber(array[0])) {
                return Integer.parseInt(array[0]);
            } else {
                throw new MojoFailureException("解析Java编译器版本号错误: " + this.mavenCompilerSource);
            }
        }

        if (array.length == 2 || array.length == 3) {
            if (StringUtils.isNumber(array[1])) {
                return Integer.parseInt(array[1]);
            } else {
                throw new MojoFailureException("解析Java编译器版本号错误: " + this.mavenCompilerSource);
            }
        }

        throw new UnsupportedOperationException(this.mavenCompilerSource);
    }

}

