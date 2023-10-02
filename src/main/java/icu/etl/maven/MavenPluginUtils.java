package icu.etl.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Maven插件的工具类
 *
 * @author jeremy8551@qq.com
 * @createtime 2023/10/1
 */
public class MavenPluginUtils {

    /**
     * 判断字符串参数 {@code name} 是否是一个JDK适配器方言类名
     *
     * @param name 字符串
     * @return true表示是JDK适配器方言类的类名
     */
    public static boolean isJDK(String name) {
        return name.startsWith("JDK") //
                && name.endsWith(".java") //
                && isNumber(name.substring("JDK".length(), name.length() - ".java".length()) //
        );
    }

    /**
     * 判断字符串 {@code str} 是否是整数
     *
     * @param str 字符串
     * @return true表示字符串是整数
     */
    public static boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析JDK适配器方言类名中的版本号
     *
     * @param name JDK适配器方言类名（含扩展名）
     * @return 版本号
     */
    public static int parseVersion(String name) {
        String str = name.substring("JDK".length(), name.length() - ".java".length());
        return Integer.parseInt(str);
    }

    /**
     * 将文件 {@code file} 内容复制到文件 {@code copy}中
     *
     * @param file 文件
     * @param copy 复制到那个文件中
     * @param log  日志接口
     * @throws MojoFailureException 发生错误
     */
    public static void copyfile(File file, File copy, Log log) throws MojoFailureException {
        log.info("复制文件 " + file.getAbsolutePath() + " 到 " + copy.getAbsolutePath() + " ..");

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(copy);
            writefile(file, out);
        } catch (Exception e) {
            throw new MojoFailureException(copy.getAbsolutePath(), e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                throw new MojoFailureException(copy.getAbsolutePath(), e);
            }
        }
    }

    /**
     * 读取文件 {@code file} 中的内容，并写入到输出流 {@code out} 中
     *
     * @param file 文件
     * @param out  输出流
     * @throws MojoFailureException 发生错误
     */
    public static void writefile(File file, FileOutputStream out) throws MojoFailureException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            for (int read; (read = in.read(bytes)) != -1; ) {
                out.write(bytes, 0, read);
            }
            out.flush();
        } catch (Exception e) {
            throw new MojoFailureException("", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                throw new MojoFailureException(file.getAbsolutePath(), e);
            }
        }
    }

    public static File search(File dir, String prefix, String ext) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(prefix) && name.endsWith(ext)) {
                return dir;
            }

            if (file.isDirectory()) {
                File result = MavenPluginUtils.search(file, prefix, ext);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

}
