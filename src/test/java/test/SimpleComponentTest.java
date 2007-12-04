/**
 * 
 */
package test;

import java.io.File;

import atg.test.AtgDustCase;

import com.mycompany.SimpleComponent;

/**
 * 
 * Unit test to see if the framework can 'handle' session and request based
 * components.
 * 
 * <br/><br/>Based on {@link AtgDustCase}
 * 
 * @author robert
 * 
 */
public class SimpleComponentTest extends AtgDustCase {

  private SimpleComponent simpleComponent;

  protected void setUp() throws Exception {
    super.setUp();

    // The different exclude pattern (.svn, CVS) is needed in
    // here because some other tests in this package (SimpleRepositoryTest)
    // don't use
    // AtgDusCase and therefore bypass the whole optimized property copying
    // way implemented in AtgDusCase. This will result in the original property
    // file with scope request overwriting the chnaged property file with forced
    // global scope. Because the pattern is different from the other tests a
    // re-copy and re-global-scope-force will be executed.

    copyConfigurationFiles(new String[] { "src/test/resources/config".replace(
        "/", File.separator) }, "target/test-classes/config".replace("/",
        File.separator), ".svn", "CVS");

    simpleComponent = (SimpleComponent) resolveNucleusComponent("/test/SimpleComponentRequestScope");

  }

  /**
   * Can it handle a non globally scoped component?
   * 
   */
  public void testSimpleComponent() {
    assertNotNull(simpleComponent);
  }

  /**
   * Test to see if the output says something about "No config copy, because
   * they are still the same" ?
   * 
   */
  public void testSimpleComponentAgain() {
    assertNotNull(simpleComponent);
  }

}
