package icu.etl.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Set;

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
     */
    public static String readPackageName(File file, String charsetName) throws MojoFailureException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().startsWith("package")) {
                    return line.substring("package".length()).replace(';', ' ').trim();
                }
            }
            return null;
        } catch (Exception e) {
            throw new MojoFailureException(file.getAbsolutePath(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    throw new MojoFailureException(file.getAbsolutePath(), e);
                }
            }
        }
    }

    public static Set<String> readIgnorefile(File file, String charsetName) throws MojoFailureException {
        Set<String> list = new LinkedHashSet<String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName));
            String line;
            while ((line = in.readLine()) != null) {
                list.add(line.trim());
            }
            return list;
        } catch (Exception e) {
            throw new MojoFailureException(file.getAbsolutePath(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    throw new MojoFailureException(file.getAbsolutePath(), e);
                }
            }
        }
    }

    public static void write(File file, String charsetName, boolean append, String content) {
        FileWriter out = null;
        try {
//            out = new OutputStreamWriter(new FileOutputStream(file, append), charsetName);
        } catch (Exception e) {
            throw new RuntimeException(file.getAbsolutePath(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {

                }
            }
        }
    }

}
