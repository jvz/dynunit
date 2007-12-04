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
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.apache.log4j.Logger;

/**
 * @author robert
 * 
 */
public class FileUtil {

  private static Logger log = Logger.getLogger(FileUtil.class);

  private static final Map<String, Long> smartCopyMapDst = new HashMap<String, Long>();

  private static final File TMP_FILE = new File(System
      .getProperty("java.io.tmpdir")
      + File.separator + "atg-dust-rh.tmp");

  public static enum SmartCopyDirection {
    SOURCE, DESTINATION;
  }

  public static void copyDir(String srcDir, String dstDir,
      final List<String> excludes) throws IOException {
    copyDir(srcDir, dstDir, excludes, false);

  }

  public static void copyOrRestoreStagingConfigurationLocation(String srcDir,
      String dstDir, final List<String> excludes) throws IOException {
    copyDir(srcDir, dstDir, excludes, true);
  }

  /**
   * 
   * @param srcDir
   * @param dstDir
   * @param excludes
   * @param isSmartCopyWithGlobalScopeForce
   * @throws IOException
   */
  private static void copyDir(String srcDir, String dstDir,
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
  private static void copyFile(final String src, final String dst)
      throws IOException {
    final FileChannel srcChannel = new FileInputStream(src).getChannel();
    final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
    dstChannel.close();
    srcChannel.close();
    log.debug(String.format("Copied %s to %s", src, dst));
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
      out.flush();
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
    out.flush();
    out.close();
    in.close();
    copyFile(TMP_FILE.getAbsolutePath(), file.getAbsolutePath());
  }

  /**
   * This method only copies the target to the destination if the following is
   * true:
   * <ul>
   * <li> Destination file does not exist</li>
   * <li> Checksum of the newly created destination is not the same as the
   * previous destination checksum</li>
   * </ul>
   * 
   * @param target
   * @param dst
   * @throws IOException
   */
  private static void smartCopyAndForceGlobaScope(final String target,
      final String dst) throws IOException {
    final File destination = new File(dst);
    final long currentChecksum = getChecksum(destination), lastChecksum = smartCopyMapDst
        .get(dst) == null ? -1L : smartCopyMapDst.get(dst);
    log.debug("Last checksum [1]: " + lastChecksum);
    log.debug("Current checksum [2]: " + currentChecksum);

    if (lastChecksum == currentChecksum) {
      log.debug("Not overwriting [3a]: " + dst);
    }
    else {
      log.debug("Overwriting and forcing global scope [3b]: " + dst);
      copyFile(target, dst);
      FileUtil.searchAndReplace("$scope=", "$scope=global\n", destination);
      smartCopyMapDst.put(dst, getChecksum(destination));
    }
  }

  private static long getChecksum(final File file) {
    long checksum = -1;
    try {
      final CheckedInputStream cis = new CheckedInputStream(
          new FileInputStream(file), new Adler32());
      final byte[] buf = new byte[256];
      while (cis.read(buf) >= 0) {

      }
      checksum = cis.getChecksum().getValue();
      cis.close();
    }
    catch (IOException e) {
      log.error("Error: ", e);
    }
    return checksum;

  }

  static {
    TMP_FILE.deleteOnExit();
  }
}
