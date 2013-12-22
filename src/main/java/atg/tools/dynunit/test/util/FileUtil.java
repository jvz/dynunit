/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package atg.tools.dynunit.test.util;

import atg.core.util.JarUtils;
import atg.tools.dynunit.util.ComponentUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This class is practically replaceable with FileUtils.
 *
 * @author robert
 * @author msicker
 */
public final class FileUtil {

    private FileUtil() {
    }

    private static final Logger logger = LogManager.getLogger();

    private static boolean dirty = false;

    @Deprecated
    private static Map<String, Long> CONFIG_FILES_GLOBAL_FORCE;

    private static final ConcurrentHashMap<String, Long> configFilesLastModified = new ConcurrentHashMap<String, Long>();

    private static final File configFilesLastModifiedCache = new File(
            FileUtils.getTempDirectory(), "dynunit-config-cache.ser"
    );

    public static File newTempFile()
            throws IOException {
        logger.entry();
        final File tempFile = File.createTempFile("dynunit-", null);
        tempFile.deleteOnExit();
        return logger.exit(tempFile);
    }

    public static void copyDirectory(@NotNull String srcDir,
                                     @NotNull String dstDir,
                                     @NotNull final List<String> excludes)
            throws IOException {
        logger.entry(srcDir, dstDir, excludes);
        Validate.notEmpty(srcDir);
        Validate.notEmpty(dstDir);
        final File source = new File(srcDir);
        if (!source.exists()) {
            throw logger.throwing(new FileNotFoundException(srcDir));
        }
        final File destination = new File(dstDir);
        FileUtils.copyDirectory(source, destination, new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return excludes.contains(file.getName());
            }
        });
        logger.exit();
    }

    /**
     * @see atg.tools.dynunit.util.ComponentUtil#newComponent(java.io.File, String, Class, java.util.Properties)
     */
    @Deprecated
    public static void createPropertyFile(@NotNull final String componentName,
                                          @NotNull final File configurationStagingLocation,
                                          @Nullable final Class<?> clazz,
                                          @Nullable final Map<String, String> settings)
            throws IOException {
        logger.entry(componentName, configurationStagingLocation, clazz, settings);
        final Properties properties = new Properties();
        properties.putAll(settings);
        ComponentUtil.newComponent(configurationStagingLocation, componentName, clazz, properties);
        logger.exit();
    }

    public static void forceGlobalScope(final File file)
            throws IOException {
        forceComponentScope(file, "global");
    }

    public static void forceComponentScope(final File file, final String scope)
            throws IOException {
        final long oldLastModified = getConfigFileLastModified(file);
        if (oldLastModified < file.lastModified()) {
            final File tempFile = newTempFile();
            BufferedWriter out = null;
            BufferedReader in = null;
            try {
                out = new BufferedWriter(new FileWriter(tempFile));
                in = new BufferedReader(new FileReader(file));
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    if (line.contains("$scope")) {
                        out.write("$scope=");
                        out.write(scope);
                    }
                    else {
                        out.write(line);
                    }
                    out.newLine();
                }
                FileUtils.copyFile(tempFile, file);
                setConfigFileLastModified(file);
                dirty = true;
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private static void persistConfigCache()
            throws IOException {
        FileUtils.touch(configFilesLastModifiedCache);
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(configFilesLastModifiedCache));
        SerializationUtils.serialize(configFilesLastModified, out);
    }

    /**
     * @see #forceGlobalScope(java.io.File)
     * @see #forceComponentScope(java.io.File, String)
     */
    @Deprecated
    public static void searchAndReplace(@NotNull final String originalValue,
                                        @NotNull final String newValue,
                                        @NotNull final File file)
            throws IOException {
        final File tempFile = newTempFile();

        if (CONFIG_FILES_GLOBAL_FORCE != null
                && CONFIG_FILES_GLOBAL_FORCE.get(file.getPath()) != null
                && CONFIG_FILES_GLOBAL_FORCE.get(file.getPath()) == file.lastModified()
                && file.exists()) {
            dirty = false;
            logger.debug(
                    "{} last modified hasn't changed and file still exists, "
                            + "no need for global scope force", file.getPath()
            );
        }
        else {
            final BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
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
            JarUtils.copy(tempFile, file, true, false);
            CONFIG_FILES_GLOBAL_FORCE.put(file.getPath(), file.lastModified());
            dirty = true;
        }

    }

    /**
     * @see org.apache.commons.io.FileUtils#forceDeleteOnExit(java.io.File)
     */
    @Deprecated
    public static void deleteDirectoryOnShutdown(@NotNull final File tmpDir) {
        try {
            FileUtils.forceDeleteOnExit(tmpDir);
        } catch (IOException e) {
            logger.catching(Level.ERROR, e);
        }
    }

    /**
     * @see org.apache.commons.lang3.SerializationUtils#serialize(java.io.Serializable, java.io.OutputStream)
     */
    @Deprecated
    public static void serialize(@NotNull final File file, final Object o)
            throws IOException {
        Validate.isInstanceOf(Serializable.class, o);
        final Serializable object = (Serializable) o;
        SerializationUtils.serialize(object, new FileOutputStream(file));
    }

    /**
     * @see org.apache.commons.lang3.SerializationUtils#deserialize(java.io.InputStream)
     */
    @Nullable
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Map<String, Long> deserialize(@NotNull final File file, final long serialTtl) {

        if (file.exists() && file.lastModified() < System.currentTimeMillis() - serialTtl) {
            logger.debug(
                    "Deleting previous serial {} because it's older than {} ms",
                    file.getPath(),
                    serialTtl
            );
            file.delete();
        }

        Map<String, Long> o = null;
        try {
            if (file.exists()) {
                final ObjectInputStream in = new ObjectInputStream(
                        new FileInputStream(
                                file
                        )
                );
                try {
                    o = (Map<String, Long>) in.readObject();
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            logger.catching(e);
        }
        if (o == null) {
            o = new HashMap<String, Long>();
        }
        return o;
    }

    public static void setConfigFilesTimestamps(Map<String, Long> config_files_timestamps) {
    }

    @Deprecated
    public static void setConfigFilesGlobalForce(Map<String, Long> config_files_global_force) {
        CONFIG_FILES_GLOBAL_FORCE = config_files_global_force;
    }

    public static boolean isDirty() {
        return dirty;
    }

    @Deprecated
    public static Map<String, Long> getConfigFilesTimestamps() {
        return CONFIG_FILES_GLOBAL_FORCE;
    }

    private static long getConfigFileLastModified(final String configFile) {
        if (configFile == null || !configFilesLastModified.containsKey(configFile)) {
            return 0L;
        }
        return configFilesLastModified.get(configFile);
    }

    private static long getConfigFileLastModified(final File configFile) {
        if (configFile == null) {
            return 0L;
        }
        return getConfigFileLastModified(configFile.getPath());
    }

    private static void setConfigFileLastModified(final String configFile, final long lastModified) {
        Validate.notEmpty(configFile);
        configFilesLastModified.put(configFile, lastModified);
    }

    private static void setConfigFileLastModified(final File configFile) {
        Validate.notNull(configFile);
        setConfigFileLastModified(configFile.getPath(), configFile.lastModified());
    }

}
