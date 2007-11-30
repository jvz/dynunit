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

  public static void copyDir(String srcDir, String dstDir,
      final List<String> excludes) throws IOException {
    new File(dstDir).mkdirs();
    final String[] fileList = new File(srcDir).list();
    boolean dir;
    for (final String file : fileList) {
      final String source = srcDir + File.separator + file, destination = dstDir
          + File.separator + file;
      dir = new File(source).isDirectory();
      if (dir && !excludes.contains(file)) {
        copyDir(source, destination, excludes);
      }
      else {
        if (!excludes.contains(file)) {
          copyFile(source, destination);
        }
      }
    }

  }

  public static void copyFile(final String src, final String dst)
      throws IOException {
    final FileChannel srcChannel = new FileInputStream(src).getChannel();
    final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
    srcChannel.close();
    dstChannel.close();

  }

  // Delete all files and sub directories under dir.
  public static boolean deleteDir(final File dir) {
    if (dir.isDirectory()) {
      final String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        deleteDir(new File(dir, children[i]));
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }

  public static void searchAndReplace(final String searchString,
      final String value, final File file) throws IOException {
    // create tmp-file
    final File tmp = new File(System.getProperty("java.io.tmpdir")
        + File.separator + "atg-dust.tmp");
    tmp.deleteOnExit();
    final BufferedWriter out = new BufferedWriter(new FileWriter(tmp));

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
    copyFile(tmp.getAbsolutePath(), file.getAbsolutePath());
  }

  /**
   * 
   * @param aStartingDir
   * @return
   * @throws FileNotFoundException
   */
  public static List<File> getFileListing(File aStartingDir)
      throws FileNotFoundException {
    final List<File> result = new ArrayList<File>();

    final File[] filesAndDirs = aStartingDir.listFiles();
    final List<File> filesDirs = Arrays.asList(filesAndDirs);
    for (File file : filesDirs) {
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

  /**
   * 
   * @param componentName
   *          The name of the nucleus component
   * @param configurationLocation
   *          A valid not <code>null</code> directory.
   * @param clazz
   *          The class behind the nucleus component
   * @param settings
   *          An implementation of {@link Map} containing all needed properties
   *          the component is depended on. Can not be <code>null</code>, but
   *          empty map's are allowed.
   * @throws IOException
   */
  public static void createPropertyFile(final String componentName,
      final File configurationLocation, final String clazz,
      final Map<String, String> settings) throws IOException {
    configurationLocation.mkdirs();
    final File prop = new File(configurationLocation, componentName
        + ".properties");
    new File(prop.getParent()).mkdirs();

    if (prop.exists()) {
      prop.delete();
    }
    prop.createNewFile();
    final FileWriter fw = new FileWriter(prop);
    final String classLine = "$class=" + clazz + "\n";
    try {
      if (clazz != null) {
        fw.write(classLine);
      }
      for (final Iterator<Entry<String, String>> it = settings.entrySet()
          .iterator(); it.hasNext();) {
        final Entry<String, String> entry = it.next();
        fw.write(new StringBuilder(entry.getKey()).append("=").append(
            StringUtils.replace(entry.getValue(), '\\', "\\\\")).append("\n")
            .toString());
      }
    }
    finally {
      fw.flush();
      fw.close();
    }
  }
}
