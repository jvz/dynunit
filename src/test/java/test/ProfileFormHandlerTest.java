package test;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.repository.MutableRepository;
import junit.framework.TestCase;

/**
 * Tests ProfileFormHandler using the config layer from the ATG build.
 * @author adamb
 *
 */
public class ProfileFormHandlerTest extends TestCase {

  public static final String PROFILE_ADAPTER_REPOSITORY_PATH = "/atg/userprofiling/ProfileAdapterRepository";
  Logger mLogger = Logger.getLogger(this.getClass());
  Nucleus mNucleus = null;

  // ------------------------------------
  /**
   * Starts Nucleus. Fails the test if there is a problem starting Nucleus.
   */
  @Override
  public void setUp() {
    mLogger.log(Level.INFO, "Start Nucleus.");
    try {
      System.setProperty("derby.locks.deadlockTrace","true");
      mNucleus = NucleusTestUtils.startNucleusWithModules(new String[] { "DPS","DafEar.base"},
          this.getClass(),
          this.getClass().getName(),
          PROFILE_ADAPTER_REPOSITORY_PATH);
    } catch (ServletException e) {
      fail(e.getMessage());
    }

  }
  
  // ------------------------------------
  /**
   * If there is a running Nucleus, this method shuts it down.
   * The test will fail if there is an error while shutting down Nucleus.
   */
  @Override
  public void tearDown() {
    mLogger.log(Level.INFO, "Stop Nucleus");
    if (mNucleus != null) {
      try {
        NucleusTestUtils.shutdownNucleus(mNucleus);
      } catch (ServiceException e) {
        fail(e.getMessage());
      } catch (IOException e) {
        fail(e.getMessage());
      }
    }    
  }
  
  /**
   * TODO: Make it possible to resolve request scoped objects in a test.
   * Ideally we setup a fake request and make all the code "just work".
   * @throws Exception
   */
  public void testProfileFormHandler() throws Exception {
    MutableRepository par = (MutableRepository) mNucleus.resolveName(PROFILE_ADAPTER_REPOSITORY_PATH);
    assertNotNull(par);
  }


}
