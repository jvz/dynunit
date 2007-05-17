/*
 * <ATGCOPYRIGHT>
 * Copyright (C) 1997-2005 Art Technology Group, Inc.
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
 * </ATGCOPYRIGHT>
 */

package atg.adapter.gsa;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author adamb
 * @version $Revision: #4 $
 */
public class GSATestUtilsTest extends TestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(GSATestUtilsTest.class);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Constructor for GSATestUtilsTest.
   * @param arg0
   */
  public GSATestUtilsTest(String arg0) {
    super(arg0);
  }

  public void testInitializeMinimalConfigpath() throws Exception{
    String [] defFiles = {"atg/adapter/gsa/rep1.xml","atg/adapter/gsa/rep2.xml"};

    File root = new File("./testingconfig");
    File propFile = new File(root.getAbsolutePath() + "/foo/bar/Repository.properties");
    if (propFile.exists()) propFile.delete();
    assertFalse(propFile.exists());
    Properties props = new Properties();
    props.put("driver", "org.hsqldb.jdbcDriver");
    props.put("URL", "jdbc:hsqldb:mem:testdb");
    props.put("user", "sa");
    props.put("password", "");
    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(root, "/foo/bar/Repository", defFiles, props, null, null, null);
    assertTrue(propFile.exists());
    System.out.println(propFile.getAbsolutePath());
    GSATestUtils.getGSATestUtils().cleanup();

  }

}
