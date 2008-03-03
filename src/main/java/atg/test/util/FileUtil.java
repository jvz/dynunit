/**
 * 
 */
package atg.test.util;

import static atg.test.AtgDustCase.TIMESTAMP_SER;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * @author robert
 * 
 */
@SuppressWarnings("unchecked")
public class FileUtil {

  private static Logger log = Logger.getLogger(FileUtil.class);

  private static final File TMP_FILE = new File(System
      .getProperty("java.io.tmpdir")
      + File.separator + "atg-dust-rh.tmp");

  private static Map<String, Long> CONFIG_FILES_TIMESTAMPS;

  /**
   * 
   * @param srcDir
   * @param dstDir
   * @param excludes
   * @throws IOException
   */
  public static void copyDirectory(String srcDir, String dstDir,
      final List<String> excludes) throws IOException {
    if (new File(srcDir).exists()) {
      new File(dstDir).mkdirs();
      for (final String file : new File(srcDir).list()) {
        final String source = srcDir + File.separator + file, destination = dstDir
            + File.separator + file;
        boolean dir = new File(source).isDirectory();
        if (dir && !excludes.contains(file)) {
          copyDirectory(source, destination, excludes);
        }
        else {
          if (!excludes.contains(file)) {
            copyFile(source, destination);
          }
        }
      }
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
    final File srcFile = new File(src);
    final File dstFile = new File(dst);

    if (CONFIG_FILES_TIMESTAMPS != null
        && CONFIG_FILES_TIMESTAMPS.get(src) != null
        && CONFIG_FILES_TIMESTAMPS.get(src) == srcFile.lastModified()) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("%s last modified hasn't changed", src));
      }
    }
    else {
      if (!src.contains(TMP_FILE.getPath())) {
        if (log.isDebugEnabled()) {
          log.debug(String.format("Src file %s ts %s : ", src, srcFile
              .lastModified()));
          log.debug(String.format("Dst file %s ts %s : ", dst, dstFile
              .lastModified()));
        }
      }

      final FileChannel srcChannel = new FileInputStream(src).getChannel();
      final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
      dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
      dstChannel.close();
      srcChannel.close();
    }

  }

  /**
   * 
   * @param componentName
   *          The name of the nucleus component
   * @param configurationStagingLocation
   *          A valid not <code>null</code> directory.
   * @param clazz
   *          The class implementing the nucleus component
   * @param settings
   *          An implementation of {@link Map} containing all needed properties
   *          the component is depended on (eg key = username, value = test).
   *          Can be <code>null</code> or empty.
   * @throws IOException
   */
  public static void createPropertyFile(final String componentName,
      final File configurationStagingLocation, final Class<?> clazz,
      final Map<String, String> settings) throws IOException {

    configurationStagingLocation.mkdirs();
    final File propertyFile = new File(configurationStagingLocation,
        componentName.replace("/", File.separator) + ".properties");
    new File(propertyFile.getParent()).mkdirs();
    propertyFile.createNewFile();
    final BufferedWriter out = new BufferedWriter(new FileWriter(propertyFile));

    try {
      if (clazz != null) {
        out.write("$class=" + clazz.getName());
        out.newLine();
      }
      if (settings != null) {
        for (final Iterator<Entry<String, String>> it = settings.entrySet()
            .iterator(); it.hasNext();) {
          final Entry<String, String> entry = it.next();
          out.write(new StringBuilder(entry.getKey()).append("=").append(
              entry.getValue()).toString());
          out.newLine();
        }
      }
    }
    finally {
      out.close();
    }
  }

  public static void searchAndReplace(final String originalValue,
      final String newValue, final File file) throws IOException {
    final BufferedWriter out = new BufferedWriter(new FileWriter(TMP_FILE));
    final BufferedReader in = new BufferedReader(new FileReader(file));
    String str;
    while ((str = in.readLine()) != null) {
      if (str.contains(originalValue)) {
        out.write(newValue);
      }
      else {
        out.write(str);
        out.newLine();
      }
    }
    out.close();
    in.close();
    copyFile(TMP_FILE.getAbsolutePath(), file.getAbsolutePath());
  }

  public static List<File> getFileListing(File startDirectory)
      throws FileNotFoundException {

    if (!startDirectory.exists()) {
      return new ArrayList<File>();
    }
    final List<File> result = new ArrayList<File>();
    for (final File file : startDirectory.listFiles()) {
      result.add(file); // always add, even if directory
      if (!file.isFile()) {
        // must be a directory
        // recursive call!
        List<File> deeperList = getFileListing(file);
        result.addAll(deeperList);
      }

    }
    Collections.sort(result);
    return result;
  }

  static {
    TMP_FILE.deleteOnExit();
    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(
          TIMESTAMP_SER));
      CONFIG_FILES_TIMESTAMPS = (Map<String, Long>) in.readObject();
    }
    catch (Exception e) {
    }
  }
}
