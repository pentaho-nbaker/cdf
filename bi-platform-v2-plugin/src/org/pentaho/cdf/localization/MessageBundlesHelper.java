package org.pentaho.cdf.localization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.pentaho.cdf.CdfConstants;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.data.simple.SimpleRepositoryFileData;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.pluginmgr.PluginClassLoader;
import org.pentaho.platform.util.messages.LocaleHelper;

/**
 * Created by IntelliJ IDEA.
 * User: sramazzina
 * Date: 7-lug-2010
 * Time: 11.27.43
 * To change this template use File | Settings | File Templates.
 */
public class MessageBundlesHelper {

    private File globalBaseMessageFile;
    
    // Name the dashboard target i18n messages file. If we have a dashboard specific language file it will be named
    // the same otherwise it will have the name of the global message file. The target message file contains globals and local translations
    // (if the dashboard has a specific set of translations) or the name of the global one if no translations are specified.
    // This way we eliminate fake error messages that are given by the unexpected unavailability of message files.
    private File targetDashboardBaseMsgFile;
    private File globalMsgCacheFile;
    private String sourceDashboardBaseMsgFile;
    private static final String PENTAHO_CDF_GLOBAL_LANGUAGES_DIR = "resources/languages";
    private String languagesCacheUrl;
    private Object msgsDir;
    private File pluginRootDir;


    public MessageBundlesHelper(File msgsDir,
        String msgsBaseFileName) {
      
      this.msgsDir = msgsDir;
      String relativeDirPath = FilenameUtils.getPath(msgsDir.getAbsolutePath()) + msgsDir.getName();
      sourceDashboardBaseMsgFile = msgsBaseFileName;
      languagesCacheUrl = File.separator + relativeDirPath;
    }
    
    
    public MessageBundlesHelper(RepositoryFile msgsDir,
                                String msgsBaseFileName) {
      this.msgsDir = msgsDir;
      String relativeDirPath = FilenameUtils.getPath(msgsDir.getPath()) + msgsDir.getName();
      sourceDashboardBaseMsgFile = msgsBaseFileName;
      languagesCacheUrl = File.separator + relativeDirPath;
    }

    protected File getGlobalBaseMessageFile() {
      if (globalBaseMessageFile == null) {
        globalBaseMessageFile = new File(getPluginRootDir(), PENTAHO_CDF_GLOBAL_LANGUAGES_DIR + File.separator + CdfConstants.BASE_GLOBAL_MESSAGE_SET_FILENAME);
      }
      return globalBaseMessageFile;
    }
    
    protected File getTargetDashboardBaseMessageFile() {
      if (targetDashboardBaseMsgFile == null) {
        String relativeDirPath = null;
        if (msgsDir instanceof RepositoryFile) {
          RepositoryFile repositoryFile = (RepositoryFile)msgsDir;
          relativeDirPath = FilenameUtils.getPath(repositoryFile.getPath()) + repositoryFile.getName();
        } else {
          File file = (File)msgsDir;
          relativeDirPath = FilenameUtils.getPath(file.getAbsolutePath()) + file.getName();
        }
        targetDashboardBaseMsgFile = new File(getPluginRootDir(), CdfConstants.BASE_CDF_CACHE_DIR + File.separator + relativeDirPath + File.separator + (sourceDashboardBaseMsgFile!=null ? sourceDashboardBaseMsgFile : CdfConstants.BASE_GLOBAL_MESSAGE_SET_FILENAME));
      }
      return targetDashboardBaseMsgFile;

    }

    protected File getGlobalMsgCacheFile() {
      if (globalMsgCacheFile == null) {
        String relativeDirPath = null;
        if (msgsDir instanceof RepositoryFile) {
          RepositoryFile repositoryFile = (RepositoryFile)msgsDir;
          relativeDirPath = FilenameUtils.getPath(repositoryFile.getPath()) + repositoryFile.getName();
        } else {
          File file = (File)msgsDir;
          relativeDirPath = FilenameUtils.getPath(file.getAbsolutePath()) + file.getName();
        }
        globalMsgCacheFile = new File(getPluginRootDir(), CdfConstants.BASE_CDF_CACHE_DIR + File.separator + relativeDirPath + File.separator + CdfConstants.BASE_GLOBAL_MESSAGE_SET_FILENAME + ".properties");
      }
      return globalMsgCacheFile;
      
    }

    public File getPluginRootDir() {
      return pluginRootDir != null ? pluginRootDir : ((PluginClassLoader)MessageBundlesHelper.class.getClassLoader()).getPluginDir();
    }


    public void setPluginRootDir(File pluginRootDir) {
      this.pluginRootDir = pluginRootDir;
    }


    public void saveI18NMessageFilesToCache() throws IOException {
        getTargetDashboardBaseMessageFile().getParentFile().mkdirs();
        copyStdGlobalMessageFileToCache();
        if (sourceDashboardBaseMsgFile != null) {
            appendMessageFiles(sourceDashboardBaseMsgFile, getGlobalBaseMessageFile(), getTargetDashboardBaseMessageFile());
        } else {
            appendMessageFiles(getGlobalBaseMessageFile(), getTargetDashboardBaseMessageFile());
        }
    }


    public String getMessageFilesCacheUrl() {
        return languagesCacheUrl.replace(File.separator, "/");
    }

    protected void appendMessageFiles(File globalBaseMessageFile,
                                      File targetDashboardBaseMsgFile) throws IOException {
        appendMessageFiles(null, globalBaseMessageFile, targetDashboardBaseMsgFile);
    }
    
    protected void appendMessageFiles(String sourceDashboardBaseMsgFile,
                                    File globalBaseMessageFile,
                                    File targetDashboardBaseMsgFile) throws IOException {

        Locale locale = LocaleHelper.getLocale();
        File fBaseMsgGlobal = new File(globalBaseMessageFile + "_" + locale.getLanguage() + ".properties");
        File fBaseMsgTarget = new File(targetDashboardBaseMsgFile  + "_" + locale.getLanguage() + ".properties");
        
        String theLine;
        if (!fBaseMsgTarget.exists()) {
            fBaseMsgTarget.createNewFile();
            BufferedWriter bwBaseMsgTarget = new BufferedWriter(new FileWriter(fBaseMsgTarget, true));
            // If localized global message file doesn't exists then use the standard base global message file
            // and generate a fake global message file. So this way we're sure that we always have the file
            if (!fBaseMsgGlobal.exists())
                fBaseMsgGlobal = new File(globalBaseMessageFile  + ".properties");
            BufferedReader brBaseMsgGlobal = new BufferedReader(new FileReader(fBaseMsgGlobal));
            while ((theLine = brBaseMsgGlobal.readLine()) != null) {
                bwBaseMsgTarget.write(theLine + "\n");
            }
            brBaseMsgGlobal.close();

            // Append specific message file only if it exists otherwise just use the global message files
            if (sourceDashboardBaseMsgFile != null) {
              if (msgsDir instanceof File) {
                File fBaseMsgDashboard = new File((File)msgsDir, sourceDashboardBaseMsgFile + "_" + locale.getLanguage() + ".properties");
                if (fBaseMsgDashboard.exists()) {
                  BufferedReader brBaseMsgDashboard = new BufferedReader(new FileReader(fBaseMsgDashboard));
                  while ((theLine = brBaseMsgDashboard.readLine()) != null) {
                      bwBaseMsgTarget.write(theLine + "\n");
                  }
                  brBaseMsgDashboard.close();
                }
              } else if (msgsDir instanceof RepositoryFile) {
                RepositoryFile repositoryFile = (RepositoryFile)msgsDir;
                IUnifiedRepository unifiedRepository = PentahoSystem.get(IUnifiedRepository.class, null);
                RepositoryFile fBaseMsgDashboard = unifiedRepository.getFileById(((RepositoryFile) msgsDir).getPath() + RepositoryFile.SEPARATOR + sourceDashboardBaseMsgFile + "_" + locale.getLanguage() + ".properties");
                if (fBaseMsgDashboard != null) {
                  InputStream is = unifiedRepository.getDataForRead(fBaseMsgDashboard.getId(), SimpleRepositoryFileData.class).getStream();
                  BufferedReader brBaseMsgDashboard = new BufferedReader(new InputStreamReader(is));
                  while ((theLine = brBaseMsgDashboard.readLine()) != null) {
                      bwBaseMsgTarget.write(theLine + "\n");
                  }
                  brBaseMsgDashboard.close();
                }
              }
            }
            bwBaseMsgTarget.close();
        }
    }

    protected void copyStdGlobalMessageFileToCache() throws IOException {

        File outputFile = getGlobalMsgCacheFile();
        if (outputFile.exists()) return;
        outputFile.createNewFile();
        
        File inputFile = new File(getPluginRootDir(), PENTAHO_CDF_GLOBAL_LANGUAGES_DIR + File.separator + CdfConstants.BASE_GLOBAL_MESSAGE_SET_FILENAME + ".properties");
        IOUtils.copyLarge(new FileReader(inputFile), new FileWriter(outputFile));
    }
}
