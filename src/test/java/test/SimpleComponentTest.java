/**
 * 
 */
package test;

import java.io.File;

import atg.test.AtgDustCase;

import com.mycompany.SimpleComponent;

/**
 * 
 * Little test to see if the framework can 'handle' session and request based
 * components
 * 
 * @author robert
 * 
 */
public class SimpleComponentTest extends AtgDustCase {

  private SimpleComponent simpleComponent;

  protected void setUp() throws Exception {
    super.setUp();

    copyConfigurationFiles(new String[] { "src/test/resources/config".replace(
        "/", File.separator) }, "target/test-classes/config".replace("/",
        File.separator), new String[] { ".svn" });

    simpleComponent = (SimpleComponent) resolveNucleusComponent("/test/SimpleComponent");

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
