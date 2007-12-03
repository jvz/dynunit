/**
 * 
 */
package atg.test.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author robert
 * 
 */
public class FileUtil {

  private static final Log log = LogFactory.getLog(FileUtil.class);

  private static final Map<String, Long> smartCopyMapDst = new HashMap<String, Long>();

  private static final File TMP_FILE = new File(System
      .getProperty("java.io.tmpdir")
      + File.separator + "atg-dust-rh.tmp");

  /**
   * 
   */
  public static final long VAGUE_TRESHHOLD_VALUE_FOR_SMART_COPY = 200L;

  /**
   * 
   * @param srcDir
   * @param dstDir
   * @param excludes
   * @param isSmartCopyWithGlobalScopeForce
   * @throws IOException
   */
  public static void copyDir(String srcDir, String dstDir,
      final List<String> excludes, final boolean isSmartCopyWithGlobalScopeForce)
      throws IOException {
    new File(dstDir).mkdirs();
    final String[] fileList = new File(srcDir).list();
    boolean dir;
    for (final String file : fileList) {
      final String source = srcDir + File.separator + file, destination = dstDir
          + File.separator + file;
      dir = new File(source).isDirectory();

      if (dir && !excludes.contains(file)) {
        copyDir(source, destination, excludes, isSmartCopyWithGlobalScopeForce);
      }
      else {
        if (!excludes.contains(file)) {
          if (isSmartCopyWithGlobalScopeForce) {
            smartCopyAndForceGlobaScope(source, destination);
          }
          else {
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
    final FileChannel srcChannel = new FileInputStream(src).getChannel();
    final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
    dstChannel.close();
    srcChannel.close();
  }

  /**
   * 
   * @param componentName
   *          The name of the nucleus component
   * @param configurationLocation
   *          A valid not <code>null</code> directory.
   * @param className
   *          The class implementing the nucleus component
   * @param settings
   *          An implementation of {@link Map} containing all needed properties
   *          the component is depended on (eg key = username, value = test).
   *          Can be <code>null</code> or empty.
   * @throws IOException
   */
  public static void createPropertyFile(final String componentName,
      final File configurationLocation, final String className,
      final Map<String, String> settings) throws IOException {

    configurationLocation.mkdirs();
    final File propertyFile = new File(configurationLocation, componentName
        .replace("/", File.separator)
        + ".properties");
    new File(propertyFile.getParent()).mkdirs();
    propertyFile.createNewFile();
    final BufferedWriter out = new BufferedWriter(new FileWriter(propertyFile));

    try {
      if (className != null) {
        out.write("$class=" + className);
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
        out.write(str);
        out.newLine();
      }

    }
    out.close();
    in.close();
    copyFile(TMP_FILE.getAbsolutePath(), file.getAbsolutePath());
  }

  /**
   * 
   * @param src
   * @param dst
   * @throws IOException
   */
  public static void smartCopyAndForceGlobaScope(final String src,
      final String dst) throws IOException {

    log.debug("Map  time dst [1]: " + smartCopyMapDst.get(dst));
    log.debug("File time dst [2]: " + new File(dst).lastModified());

    if ((smartCopyMapDst.get(dst) != null && (smartCopyMapDst.get(dst) - VAGUE_TRESHHOLD_VALUE_FOR_SMART_COPY) >= new File(
        dst).lastModified())) {
      // not copy
      log.debug("Not overwriting [3a]: " + dst);
    }
    else {
      // copy
      log.debug("Overwriting and forcing global scope [3b]: " + dst);
      copyFile(src, dst);
      FileUtil.searchAndReplace("$scope=", "$scope=global\n", new File(dst));
      smartCopyMapDst.put(dst, System.currentTimeMillis());
      smartCopyMapDst.put(src, System.currentTimeMillis());
    }
  }

  static {
    TMP_FILE.deleteOnExit();
  }
}
