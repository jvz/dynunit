/**
 * 
 */
package atg.test.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import atg.core.util.StringUtils;

/**
 * @author robert
 * 
 */
public class FileUtil {

  // private static final Log log = LogFactory.getLog(FileUtil.class);

  private static final File TMP_FILE = new File(System
      .getProperty("java.io.tmpdir")
      + File.separator + "atg-dust-rh.tmp");

  public static void copyDir(String srcDir, String dstDir,
      final List<String> excludes) throws IOException {
    new File(dstDir).mkdirs();
    final String[] fileList = new File(srcDir).list();
    boolean dir;
    for (final String file : fileList) {
      final String source = srcDir + File.separator + file, destination = dstDir
          + File.separator + file;
      dir = new File(source).isDirectory();

      // if (checkTs(new File(source), 10000L)) {

      if (dir && !excludes.contains(file)) {
        copyDir(source, destination, excludes);
      }
      else {
        if (!excludes.contains(file)) {
          copyFile(source, destination);
        }
      }
      // }
    }

  }

  /**
   * 
   * @param src
   * @param dst
   * @throws IOException
   */
  public static void copyFile(final String src, final String dst)
      throws IOException {
    final FileChannel srcChannel = new FileInputStream(src).getChannel();
    final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
    srcChannel.close();
    dstChannel.close();

  }

  // Delete all files and sub directories under directory.
  public static boolean deleteDir(final File dirctory) {
    // traverse directory
    if (dirctory.isDirectory()) {
      final String[] children = dirctory.list();
      for (final String child : children) {
        // recursively traverse directory
        deleteDir(new File(dirctory, child));
      }
    }
    return dirctory.delete();
  }

  public static void searchAndReplace(final String searchString,
      final String value, final File file) throws IOException {
    final BufferedWriter out = new BufferedWriter(new FileWriter(TMP_FILE));
    final BufferedReader in = new BufferedReader(new FileReader(file));
    String str;
    while ((str = in.readLine()) != null) {
      if (str.contains(searchString)) {
        out.write(value);
      }
      else {
        out.write(str + "\n");
      }

    }
    out.close();
    in.close();
    copyFile(TMP_FILE.getAbsolutePath(), file.getAbsolutePath());
  }

  /**
   * 
   * @param startingDirectory
   * @return
   * @throws FileNotFoundException
   */
  public static List<File> getFileListing(File startingDirectory)
      throws FileNotFoundException {
    final List<File> result = new ArrayList<File>(), filesDirs = Arrays
        .asList(startingDirectory.listFiles());
    for (final File file : filesDirs) {
      result.add(file);
      if (!file.isFile()) {
        result.addAll(getFileListing(file));
      }

    }
    Collections.sort(result);
    return result;
  }

  /**
   * 
   * @param componentName
   *          The name of the nucleus component
   * @param configurationLocation
   *          A valid not <code>null</code> directory.
   * @param className
   *          The class behind the nucleus component
   * @param settings
   *          An implementation of {@link Map} containing all needed properties
   *          the component is depended on. Can not be <code>null</code>, but
   *          empty map's are allowed.
   * @throws IOException
   */
  public static void createPropertyFile(final String componentName,
      final File configurationLocation, final String className,
      final Map<String, String> settings) throws IOException {
    configurationLocation.mkdirs();
    final File prop = new File(configurationLocation, componentName
        + ".properties");
    prop.delete();
    new File(prop.getParent()).mkdirs();
    prop.createNewFile();
    final FileWriter fileWriter = new FileWriter(prop);
    final String classLine = "$class=" + className + "\n";
    try {
      if (className != null) {
        fileWriter.write(classLine);
      }
      for (final Iterator<Entry<String, String>> it = settings.entrySet()
          .iterator(); it.hasNext();) {
        final Entry<String, String> entry = it.next();
        fileWriter.write(new StringBuilder(entry.getKey()).append("=").append(
            StringUtils.replace(entry.getValue(), '\\', "\\\\")).append("\n")
            .toString());
      }
    }
    finally {
      fileWriter.flush();
      fileWriter.close();
    }
  }

  // private static boolean checkTs(final File file, final long allowedDelta) {
  // if (file.exists()) {
  // final long current = System.currentTimeMillis();
  // final long lastModified = file.lastModified();
  // if (current > lastModified + allowedDelta) {
  // return true;
  // }
  // }
  // return false;
  // }

  static {
    TMP_FILE.deleteOnExit();
  }
}
