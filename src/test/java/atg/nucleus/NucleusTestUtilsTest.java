/*<ATGCOPYRIGHT>
 * Copyright (C) 2003 Art Technology Group, Inc.
 * All Rights Reserved.  No use, copying or distribution ofthis
 * work may be made except in accordance with a valid license
 * agreement from Art Technology Group.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES
 * ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * "Dynamo" is a trademark of Art Technology Group, Inc.
 </ATGCOPYRIGHT>*/

package atg.nucleus;

import junit.framework.TestCase;

/**
 * NucleusTestUtilsTest
 * @author adamb
 * @version $Id: //test/UnitTests/DAS/main/src/Java/atg/nucleus/NucleusTestUtilsTest.java#4 $
 * 
 * UnitTests for NucleusTestUtils
 *
 */
public class NucleusTestUtilsTest extends TestCase {

  /**
   * Constructor for NucleusTestUtilsTest.
   * @param arg0
   */
  public NucleusTestUtilsTest(String arg0) {
    super(arg0);
    System.setProperty("atg.license.read","true");
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(NucleusTestUtilsTest.class);
  }

  /**
   * Tests the addComponent method of NucleusTestUtils to make sure
   * that when a component is added that exact same one
   */
  public static void testAddComponent() {
    Nucleus n = null;
    try {
      n = new Nucleus(null);
    }
    catch (LicenseFailureException licExc) {
      ;
    }
    Object component = new String("I'm the test component");
    String path = "/foo/Test";
    Object component2 = new String("I'm the test component too");
    String path2 = "/foo/Test2";
    NucleusTestUtils.addComponent(n, path, component);
    NucleusTestUtils.addComponent(n, path2, component2);
    Object result = n.resolveName(path);
    Object result2 = n.resolveName(path2);
    // Make sure we get something
    assertNotNull(result);
    assertNotNull(result2);
    // Make sure it's the same component we added
    assertEquals(component, result);
    assertEquals(component2, result2);
  }
}
