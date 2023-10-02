package icu.etl.maven;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import icu.etl.util.FileUtils;
import icu.etl.util.IO;
import icu.etl.util.StringUtils;
import org.apache.maven.plugin.MojoFailureException;

/**
 * JAVA类文件的帮助类
 *
 * @author jeremy8551@qq.com
 * @createtime 2023/10/2
 */
public class JavaFileUtils {

    /**
     * 读取类文件中的包名
     *
     * @param file        类的源文件
     * @param charsetName 源文件的字符集
     * @return 包名
     * @throws MojoFailureException 读取文件发生错误
     */
    public static String readPackageName(File file, String charsetName) throws MojoFailureException {
        try {
            String content = FileUtils.readline(file, charsetName, 0);
            List<CharSequence> list = new ArrayList<CharSequence>();
            StringUtils.splitLines(content, list);

            for (CharSequence line : list) {
                if (StringUtils.startsWith(line, "package", 0, true, true)) {
                    return StringUtils.trimBlank(StringUtils.splitByBlank(line)[1], ';');
                }
            }
            return null;
        } catch (Exception e) {
            throw new MojoFailureException(file.getAbsolutePath(), e);
        }
    }

    /**
     * 读取忽略文件中的所有规则
     *
     * @param file        忽略文件
     * @param charsetName 字符集
     * @return 所有规则
     * @throws MojoFailureException 读取文件发生错误
     */
    public static Set<String> readIgnorefile(File file, String charsetName) throws MojoFailureException {
        Set<String> list = new LinkedHashSet<String>();
        BufferedReader in = null;
        try {
            in = IO.getBufferedReader(file, charsetName);
            String line;
            while ((line = in.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    list.add(StringUtils.ltrimBlank(StringUtils.rtrimBlank(line, '/')));
                }
            }
            return list;
        } catch (Exception e) {
            throw new MojoFailureException(file.getAbsolutePath(), e);
        } finally {
            IO.close(in);
        }
    }

    public static Set<String> readPatterns(List<File> files, File root) {
        Set<String> set = new LinkedHashSet<String>();
        for (File file : files) {
            String filename = FileUtils.changeFilenameExt(file.getName(), "java");
            String pattern = filename.substring(0, "JDK".length()) + "*" + filename.substring(filename.lastIndexOf('.'));

            List<String> list = new ArrayList<String>();
            list.add(pattern); // JDK*.java
            File parent = file.getParentFile();
            while (parent != null && !parent.equals(root)) {
                list.add(0, parent.getName());
                parent = parent.getParentFile();
            }

            StringBuilder buf = new StringBuilder();
            for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
                buf.append(it.next());
                if (it.hasNext()) {
                    buf.append("/");
                }
            }
            set.add(buf.toString());
        }

        return set;
    }

}
