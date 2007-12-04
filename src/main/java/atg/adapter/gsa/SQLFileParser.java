/**
 * Copyright 2007 ATG DUST Project
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

package atg.adapter.gsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
* THIS IS A REALLY SIMPLE APPLICATION THAT
* TAKES A STRING ARRAY OF SQL FILES AND
* RETURNS ALL OF THE SQL STATEMENTS THAT
* MUST BE EXECUTED WITHIN THE FILES
* DATE: MAY 23, 2000
* 
*
* @author jb
* 7/14/2002
*/
public class SQLFileParser
{
  
  private static Logger log = Logger.getLogger(SQLFileParser.class);

// ****************************************
// THERE IS ONLY ONE CLASS VARIABLE
// FOR THIS CLASS.  THAT VARIABLE IS TO
// STORE THE FILE NAMES.
// ****************************************

    private String[] sFileNameArray = {""};
    /** sets the names of files to be parsed */
    private void setFileNameArray(String[] pFileNameArray)
    {
            sFileNameArray = pFileNameArray;
    }
    
//    /** returns the names of files to be parsed */
//    private String[] getFileNameArray()
//    {
//            return (sFileNameArray);
//    }

// ============== CONSTRUCTORS =================
      /**
      * no arg constructor
      */
      public SQLFileParser() {}

// ================= PUBLIC METHODS =================


	private String trimDebuggingCharacters(String sText)
	{
            if ( sText == null ) return sText;
            return sText.trim();
            /*
		char[] ch = new char [sText.length()];
		ch = sText.toCharArray();
		String sTemp = "";
		for (int i=0; i < sText.length(); i++)
		{
			if ((ch[i] == '\n') || (ch[i] == '\r') || (ch[i] == '\t') || (ch[i] == '	'))
			{
				// Keep going
				sTemp = sTemp + " ";
			}
			else
			{
				sTemp = sTemp + ch[i];
			}
		}
		if (sTemp.length() > DEBUGGING_LENGTH)
		{
			return (sTemp.substring(0,DEBUGGING_LENGTH));
		}
		else
		{
			return (sTemp);
		}
              */
	}

	private  String RemoveWhiteSpaceInFront (String sText)
	{
// **********************************************************************************
//  THIS FUNCTION REMOVES WHITESPACE IN THE FRONT OF A STRING. FOR INSTANCE
//  IF A STRING IS PASSED TO IT AS '   SPACE', THEN IT WILL RETURN 'SPACE.
// **********************************************************************************
		char[] ch = new char [sText.length()];
		ch = sText.toCharArray();
		//String sTemp;
		for (int i=0; i < sText.length(); i++)
		{
			if ((ch[i] == '\n') || (ch[i] == '\r') || (ch[i] == '\t')|| (ch[i] == ' ') || (ch[i] == '	'))
			{
				// Keep going
			}
			else
			{
				return (sText.substring (i, sText.length()));
			}
		}
		return (sText);
	}

//	private  String RemoveWhiteSpaceInEnd (String sText)
//	{
//// **********************************************************************************
////  THIS FUNCTION REMOVES WHITESPACE IN THE END OF A STRING. FOR INSTANCE
////  IF A STRING IS PASSED TO IT AS 'SPACE   ', THEN IT WILL RETURN 'SPACE.
//// **********************************************************************************
//		char[] ch = new char [sText.length()];
//		ch = sText.toCharArray();
//		String sTemp;
//		for (int i= sText.length() - 1; i > 0; i--)
//		{
//			if ((ch[i] == '\n') || (ch[i] == '\r') || (ch[i] == '\t')|| (ch[i] == ' ') || (ch[i] == '	'))
//			{
//				// Keep going
//			}
//			else
//			{
//				return (sText.substring (0, i +1));
//			}
//		}
//		return (sText);
//	}

	private  String RemoveWhiteSpaceFromString (String sText)
	{
            if ( sText == null ) return null;
            return sText.trim();

// **********************************************************************************
//  THIS FUNCTION REMOVES WHITESPACE IN THE FRONT AND END OF A STRING. FOR INSTANCE
//  IF A STRING IS PASSED TO IT AS '   SPACE   ', THEN IT WILL RETURN 'SPACE.
// **********************************************************************************
/*
		sText = RemoveWhiteSpaceInFront (sText);
		sText = RemoveWhiteSpaceInEnd (sText);
		return (sText);
*/
	}


	private int countFileArraySize(String[] sArray)
	{

// ****************************************
// THIS IS A USEFUL FUNCTION TO
// COUNT THE SIZE OF AN ARRAY. IT
// TAKES AN ARRAY AS A PARAMETER
// AND IT RETURNS AN INTEGER AS
// THE COUNT.  IN THIS CASE WE
// DO NOT NEED TO PASS THE ARRAY
// SINCE IT IS A CLASS VARIABLE
// ****************************************

		List<String> newList = Arrays.asList(sArray);
		return (newList.size());
	}

	private boolean checkForKeywords (String sLineRead)
	{

// ****************************************
// THIS IS ONE OF THE MOST CRITICAL
// FUNCTIONS IN THIS PROGRAM.  IT
// TAKES THE RESERVED WORDS FROM
// SQL INSTALL SCRIPTS AND RESERVES
// THEM WITHIN THIS APPLICATION.
// EACH WORD LISTED BELOW STARTS
// A SQL STATEMENT.
// ****************************************

		String[] sKeywordList = {"BEGIN ", "CREATE ", "INSERT ", "UPDATE ", "DELETE ", "COMMIT ", "COMMIT", "ALTER ", "DROP ", "GRANT ", "GO", "IF OBJECT_ID"};
		List<String> lList = Arrays.asList(sKeywordList);
		int iArraySize = lList.size();

		sLineRead = RemoveWhiteSpaceInFront(sLineRead);
                //sLineRead = sLineRead.trim();
                // something about using trim() doesn't work with the overall logic of the program.

		for (int i=0; i< iArraySize; i++)
		{
			if (sLineRead.toUpperCase().startsWith(sKeywordList[i].toUpperCase()) )
			{
                            // we need to check more than just whether the line starts
                            // with the keyword.  we also have to try to determine that
                            // the keyword we found is actually NOT just the beginning of a
                            // bigger word.  for example, GOogle.
                            // so...
                            if ( sLineRead.length() == sKeywordList[i].trim().length() ) {
                                // if the whole line is just the keyword...
				return true;
                            } else if ( sLineRead.charAt( sKeywordList[i].trim().length() ) == ' ' ) {
                                // if the char after the word is a space
                                return true;
                            } else if ( sLineRead.charAt( sKeywordList[i].trim().length() ) == '(' ) {
                                // if the char after the word is a right paren "("
                                return true;
                            }
			}
		}
		return (false);
	}

	private String checkForComments(String sLineRead)
	{

// ****************************************
// THIS FUNCTION HAS ONE DUTY.  IT CHECKS
// FOR LINES IN  SQL SCRIPT THAT HAVE
// COMMENTS IN THEM.  IF IT FINDS A LINE
// IN A SQL SCRIPT THAT WOULD BE RESERVED
// FOR A COMMENT IT WILL SKIP THAT LINE.
// ****************************************

		String[] sKeywordList = {"--", "//"};
		List<String> lList = Arrays.asList(sKeywordList);
		int iArraySize = lList.size();

		//sLineRead = sLineRead;
		for (int i=0; i< iArraySize; i++)
		{
			if (sLineRead.toUpperCase().indexOf(sKeywordList[i]) > -1)
			{
				return (sLineRead.substring(0,sLineRead.indexOf(sKeywordList[i])));
			}
		}
		return (sLineRead);
	}

	private String checkForSemiColons(String sCurrentLine)
	{

// ****************************************
// THIS FUNCTION HAS ONE DUTY.  IT CHECKS
// FOR SQL STATEMENTS THAT END IN THE
// SEMICOLON PUNCTUATION AND REMOVES
// THE SEMICOLON.
// ****************************************

		if (sCurrentLine.indexOf(";") > -1)
		{
			sCurrentLine = sCurrentLine.substring (0, sCurrentLine.indexOf(";"));
		}
		return (sCurrentLine);

	}

	private String checkForEndLines(String sCurrentLine)
	{

// ****************************************
// THIS FUNCTION HAS ONE DUTY.  IT CHECKS
// FOR SQL STATEMENTS THAT END IN THE
// SEMICOLON PUNCTUATION AND REMOVES
// THE SEMICOLON.  Sometimes if there
// is division in a view that is created
// the end line will be used, and sometimes
// it is used to execute a statement.
// ****************************************
		sCurrentLine = RemoveWhiteSpaceFromString(sCurrentLine);
		int iLength = sCurrentLine.length() -2;
		//int iIndex = sCurrentLine.indexOf("/");
		if ((sCurrentLine.indexOf("/") > -1) && (sCurrentLine.indexOf("/") >= iLength))
		{
			sCurrentLine = sCurrentLine.substring (0, sCurrentLine.indexOf("/"));
		}
		else if ((sCurrentLine.indexOf("/") > -1) && (sCurrentLine.indexOf("/") == 0))
		{
			sCurrentLine = "";
		}

		sCurrentLine = sCurrentLine + " ";
		return (sCurrentLine);
	}



	private List<String> readFileAndLoadData ()
	{

// ****************************************
// THIS FUNCTION IS THE ENGINE FOR THIS
// APPLICATION. THIS FUNCTION OPENS
// THE FIRST FILE FROM THE FILE ARRAY
// LIST.  THEN IT READS THROUGH THE
// FILE LINE BY LINE BUILDING A VECTOR
// OF SQL SCRIPT COMMANDS.  WHEN IT IS
// DONE PROCESSING IT PASSES A NEAT
// VECTOR OF SQL COMMANDS TO THE
// PROGRAM THAT CALLED THIS FUNCTION
// ****************************************
// THIS PARTICULAR METHOD IS LONG AND
// TERRIBLE TO READ BECAUSE IT WAS WRITTEN
// IN ONE SITTING. AND SINCE IT SEEMED
// LIKE THE TYPE OF FUNCTION THAT LOOKS
// SO BAD TO A CODE REVIEWER, BY LEAVING
// IT IN THE CODE IT PROVIDES GOOD
// HUMOR RELIEF FOR THE DEVELOPER GIVING
// A CODE REVIEW TO THIS PROGRAM.
// ****************************************

// ****************************************
// VARIABLE DECLARATIONS
		String sFileName = "";
		String line = "";
		String lineTemp = "";
		String sSqlBuffer = "";
		//String sFindSemiColon = "";

		boolean bKeywordAlreadyFound = false;
		boolean bKeywordInString = false;

		List<String> vSql = new ArrayList<String>();

		BufferedReader in = null;

		File fFile = null;
		FileInputStream fis = null;

		InputStreamReader Fin = null;

		//Object obj = null;

		int iSizeOfVector = countFileArraySize(sFileNameArray);
// END VARIABLE DECLARATIONS
// ****************************************

// ****************************************
// LOOPING THROUGH THE VECTOR OF FILE NAMES
		for (int iFile = 0; iFile < iSizeOfVector; iFile++)
		{

		// GETTING THE FIRST FILE NAME FROM THE VECTOR
			sFileName = sFileNameArray[iFile];

			try // Try to open the file
			{
				//File fFile = new File (sDirectory, sFilename);
				fFile = new File (sFileName);
				fis = new FileInputStream(fFile);
				Fin = new InputStreamReader(fis);
				in = new BufferedReader(Fin);

				//if (isLoggingDebug()) logDebug ("Able to open file: " + sFileName);
			}
			catch (Exception e)
			{
			//	logError("* Error opening file :" + sFileName + ": *");
				return (vSql);
			} // Unable to open the file


			try // Try to read the file line by line
			{
				//String sSqlStatement = "";
				sSqlBuffer = "";
				// Open file and read first line
				line = in.readLine();

				lineTemp = line;

				while (line != null) // It is not the end of the file, LOOK FOR KEY WORDS
				{
					//log.info ("DEBUG:" + line);

					lineTemp = line;
					lineTemp = checkForComments(lineTemp);
					bKeywordInString = checkForKeywords(lineTemp);

					// if this line is starting a stored procedure declaration, parse it
					// specially. added this block to correctly parse stored procedures in oracle.
					// marty - 7/11/2002
					if ( bKeywordInString && isStartOfStoredProcedure( lineTemp ) ) {

						String command = parseStoredProcedure( lineTemp, in );
						vSql.add( command );
						line = in.readLine();
						continue;
					}

                                        // if this line is starting an 'if exists...' statement for MS SQL Server, parse
                                        // if specially.  added to work around bug 64622
                                        if ( bKeywordInString && isStartOfIfExistsCommand( lineTemp ) ) {
                                                String command = parseIfExistsCommand( lineTemp, in );
                                                vSql.add( command );
                                                line = in.readLine();
                                                continue;
                                        }

					if (bKeywordAlreadyFound)
					{
						if (bKeywordInString)
						{
							sSqlBuffer = checkForSemiColons(sSqlBuffer);
							sSqlBuffer = checkForEndLines(sSqlBuffer);
							vSql.add(sSqlBuffer);
							sSqlBuffer = lineTemp;
						}
						else // (!bKeywordInString}
						{
							sSqlBuffer = sSqlBuffer + " " + lineTemp;
						}
					}
					else
					{
						if (bKeywordInString)
						{
							bKeywordAlreadyFound = bKeywordInString;
							sSqlBuffer = sSqlBuffer + lineTemp;
						}
					}

					//Get the next line
					line = in.readLine();
				} // End while

				try
				{
					sSqlBuffer = checkForSemiColons(sSqlBuffer);
					sSqlBuffer = checkForEndLines(sSqlBuffer);
					vSql.add(sSqlBuffer);
				}
				catch (Exception ex)
				{
					//logError ("No element to add" + ex);

				}

			}
			catch (Exception e)
			{
				//logError("Could not read one line in file: " + sFileName + "\n");
				return (vSql);
			} // Unable to read one of the lines in the file

			try // To close the file streams
			{
				Fin.close();
				fis.close();
				//fFile.close();
			}
			catch (Exception ex)
			{
				//logError ("Unable to close files\n");
				return (vSql);
			} // If the files could not be closed
		}
		return (vSql);
	}

        /** this method returns true if this line is the start of a check for whether an object exists
         *  in MS SQL Server.  otherwise it returns false.
         */
        private boolean isStartOfIfExistsCommand( String pLine ) {
            // i think this is only on MS SQL Server.  hard-coding the logic.
            return pLine.trim().toUpperCase().startsWith("IF OBJECT_ID");
        }

        /** used to parse an if-exists method.  the parsing logic is different than our usual behavior
         *  so it has been special cased.
	 *  @return String the parsed SQL command
	 *  @exception Exception if an error occurs
         */
        private String parseIfExistsCommand( String pCommand, BufferedReader pIn )
            throws Exception
        {
		// a pretty big hack, but just parse until we find the next "go"
		String line = pIn.readLine();
		if ( line != null ) line = line.trim();
 		while ( line != null && (! line.toUpperCase().startsWith("GO")) )
 		{
			pCommand = pCommand + " " + line;
			line = pIn.readLine();
			if ( line != null ) line = line.trim();
		}

		return pCommand;
        }

	/** this method returns true if this line is the start of a definition for a stored procedure.
	 *  otherwise, it returns false.
	 */
	 private boolean isStartOfStoredProcedure( String pLine ) {
		 // i only know of this on oracle, so i'm temporarily coding it to work specifically
		 // with oracle.
		 return pLine.trim().toUpperCase().startsWith( "CREATE OR REPLACE PROCEDURE" );
	 }

	/** this method is used to parse a stored procedure.  since stored procedures may contain
	 *  sub-commands within them, we use a special process to parse them.
	 *
	 *  @return String the parsed SQL command
	 *  @exception Exception if an error occurs
	 */
	private String parseStoredProcedure( String pCommand, BufferedReader pIn )
		throws Exception
	{
		// this is pretty much a total hack, but i just want to get it working for now
		// ASSUME we just parse until we find a line that starts with "/"
		String line = pIn.readLine();
		if ( line != null ) line = line.trim();
 		while ( line != null && (! line.startsWith("/")) )
 		{
			pCommand = pCommand + " " + line;
			line = pIn.readLine();
			if ( line != null ) line = line.trim();
		}

		return pCommand;
	}


	public synchronized Collection<String> parseSQLFiles( String[] pFiles )
	{
                setFileNameArray( pFiles );
// ****************************************
// THIS FUNCTION CALLS THE FUNCTION THAT
// READS THROUGH THE ARRAY OF FILES PASSED
// TO IT AND IT RETURNS A VECTOR OF SQL
// STATEMENTS.  MOST OF THESE WILL TEND
// TO BE CREATE AND DROP STATEMENTS.
// ****************************************
		List<String> v = new ArrayList<String>();
		v = readFileAndLoadData();
		String s = "";
		for (int i=0;i<v.size();i++)
		{
			s = v.get(i).toString();
			s = trimDebuggingCharacters (s);

			if ( logToSystemOut ) {
				//log.info("\n\n" + s );
			} else {
			  //  if (isLoggingInfo ()) logInfo(s);
			}
		}

		return v;
	}

        public Collection<String> parseSQLFile( String pFile )
        {
            String[] files = { pFile };
            return parseSQLFiles( files );
        }

	// This is useful for debugging this application
	public boolean logToSystemOut = false;
	public static void main (String[] args)
	{
		SQLFileParser t = new SQLFileParser();
		t.logToSystemOut = true;
		Iterator<String> cmds = t.parseSQLFiles( args ).iterator();
		  while ( cmds.hasNext() ) {
		   log.info("\n\n" + cmds.next() );
		  }
	}



}

