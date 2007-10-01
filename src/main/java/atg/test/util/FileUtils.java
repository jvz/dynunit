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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author robert
 * 
 */
public class FileUtils {

  private static final Log log = LogFactory.getLog(FileUtils.class);

  public static void copyDir(String srcDir, String dstDir,
      final List<String> excludes) throws IOException {

    // log.debug("src Dir = " + srcDir);
    // log.debug("dst Dir = " + dstDir);

    File fdstDir = new File(dstDir);

    if (!fdstDir.exists()) {
      fdstDir.mkdirs();
      // log.info("creating " + dstDir);
    }

    String[] fileList = new File(srcDir).list();

    boolean dir;

    for (final String file : fileList) {

      // log.debug("File: " + file);

      dir = new File(srcDir + File.separator + file).isDirectory();
      if (dir && !excludes.contains(file)) {
        copyDir(srcDir + File.separator + file, dstDir + File.separator + file,
            excludes);
      }
      else {
        if (!excludes.contains(file)) {
          copyFile(srcDir + File.separator + file, dstDir + File.separator
              + file);
        }
      }
    }

  }

  public static void copyFile(final String src, final String dst) {
    try {
      log.debug("Source: " + src);
      log.debug("Dest: " + dst);
      final FileChannel srcChannel = new FileInputStream(src).getChannel();
      final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
      dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
      srcChannel.close();
      dstChannel.close();
    }
    catch (IOException e) {
      log.error("Error: ", e);
    }

  }

  // Delete all files and sub directories under dir.
  // Returns true if all deletions were successful.
  // If a deletion fails, the method stops attempting to delete and returns
  // false.
  public static boolean deleteDir(final File dir) {
    if (dir.isDirectory()) {
      final String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        final boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }

  public static void searchAndReplace(final String searchString,
      final String value, final File file) throws IOException {
    // create tmp-file
    final File tmp = new File(System.getProperty("java.io.tmpdir") + File.separator
        + "atg-dust.tmp");
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

}
