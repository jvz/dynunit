/*<ATGCOPYRIGHT>
 * Copyright (C) 2009 Art Technology Group, Inc.
 * All Rights Reserved.  No use, copying or distribution of this
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

package atg.adapter.gsa.xml;

import java.util.HashSet;

import atg.epub.project.CreateProject;
import atg.epub.project.ProjectException;

/**
 *
 * @author qma
 *
 * This class is a helper class. It uses to help access the package private class VersioningContext.
 *
 */
public class VersioningContextUtil
{

  //-------------------------------------
  /** Class version string */

  public static String CLASS_VERSION = "$Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/xml/VersioningContextUtil.java#5 $$Change: 535542 $";

 

/**
 * If supply projectName argument, We will use projectName, projectType and user to create project and get its workspaceId.
 * If you set DoCheckin to true, you have to supply comment.
 */
public static VersioningContext versioningContextHelper(String pProjectName, String pProjectType, String pUser, String pWorkspaceId, String pBranchId, String pComment, boolean pDoCheckin) {

    throw new IllegalArgumentException(
		"VersioningContextUtil class needs to be updated to support recent publishing changes.  See bug 84830.");
/*
	VersioningContext versioningContext = new VersioningContext(pProjectName, pProjectType, pUser, pWorkspaceId, pBranchId, pComment, pDoCheckin);

	try {
		pWorkspaceId = CreateProject.createProject(pProjectName, pProjectType, pUser);
		versioningContext.setWorkspaceId(pWorkspaceId);
	}
	catch (ProjectException e) {
		e.printStackTrace();
		System.out.println("Can not create project with these info: projectName=" + pProjectName + ", projectType=" + pProjectType + ", user=" + pUser +"!");
	}

*/

	/*
	System.out.println("=============pProjectName = " + versioningContext.getProjectName());
	System.out.println("=============pWorkflowName = " + versioningContext.getWorkflowName());
	System.out.println("=============pUser = " + versioningContext.getUser());
	System.out.println("=============pWrokspaceId = " + versioningContext.getWorkspaceId());
	System.out.println("=============pBranchId = " + versioningContext.getBranchId());
	System.out.println("=============pComment = " + versioningContext.getComment());
	System.out.println("=============pDoCheckin = " + versioningContext.isDoCheckin());
	*/

//	return versioningContext;
}

/*
 * If supply workspacdId argument, you must supply branchId.
 * If you set DoCheckin to true, you have to supply comment.
 */
public static VersioningContext versioningContextHelper(String pWorkspaceId, String pBranchId, String pComment, boolean pDoCheckin) {

    throw new IllegalArgumentException(
		"VersioningContextUtil class needs to be updated to support recent publishing changes.  See bug 84830.");

//	VersioningContext versioningContext = new VersioningContext(pWorkspaceId, pBranchId, pComment, pDoCheckin);

    /*
	System.out.println("=============pWrokspaceId = " + versioningContext.getWorkspaceId());
	System.out.println("=============pBranchId = " + versioningContext.getBranchId());
	System.out.println("=============pComment = " + versioningContext.getComment());
	System.out.println("=============pDoCheckin = " + versioningContext.isDoCheckin());
	*/
//	return versioningContext;
}

  public static VersioningContext createVersioningContext(String pProcessName, String pWorkflowName,
          String pUser, String pComment, boolean pCheckin) {
   return new VersioningContext(pProcessName, pWorkflowName, pUser,pComment, pCheckin);
  }

  public static VersioningContext createVersioningContext(String pWorkspaceName, String pComment, boolean pCheckin) {
    return new VersioningContext(pWorkspaceName, pComment, pCheckin);
  }
}











