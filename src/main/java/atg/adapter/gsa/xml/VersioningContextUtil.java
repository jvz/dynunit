/**
 * Copyright 2009 ATG DUST Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

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











