package test;



import java.io.IOException;

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.repository.MutableRepository;

/**
 * This test is an example showing how to startup a Nucleus instance which resolves its configuration path (CONFIGPATH)
 * from a set of modules within an ATG installation.
 * This saves the test author from having to maintain separate copies of files which exist in the CONFIGPATH of a given module.
 * The location of DYNAMO_HOME is determined by checking the following items in order for the first non-null value:
 * <ul>
 *  <li>System Property "atg.dynamo.root"
 *  <li>Environment Variable "DYNAMO_ROOT"
 *  <li>System Property "atg.dynamo.home"
 *  <li>Environment Variable "DYNAMO_HOME"
 * </ul>
 * 
 * @author adamb
 *
 */
public class StartWithModulesTest extends TestCase {
  
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
      mNucleus = NucleusTestUtils.startNucleusWithModules(new String[] { "DAF.Deployment" },
          this.getClass(),
          this.getClass().getName(),
          "/atg/deployment/DeploymentRepository");
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
  
  // ----------------------------------------
  /**
   * Resolves a component that is defined within the ATG platform and is not specifically
   * part of this test's configpath.
   * Confirms that Nucleus can start given a set of modules and a properly set DYNAMO_HOME
   * environment variable. (ex: DYNAMO_HOME=/home/user/ATG/ATG9.0/home)
   */
  public void testResolveComponentWithNucleus() {
    assertNotNull(mNucleus);
    MutableRepository catalog = (MutableRepository) mNucleus.resolveName("/atg/deployment/DeploymentRepository");
    assertNotNull("DeploymentRepository should not be null.",catalog);
    // Good enough for this test.
    // Don't want to disturb any data that might be in this repository.        
  }
    
}
