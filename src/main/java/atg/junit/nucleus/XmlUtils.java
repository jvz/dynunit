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

package atg.junit.nucleus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import atg.nucleus.logging.ApplicationLoggingImpl;
import atg.xml.tools.DefaultErrorHandler;
import atg.xml.tools.DefaultXMLToolsFactory;
import atg.xml.tools.XMLToDOMParser;
import atg.xml.tools.XMLToolsFactory;

/** A utility class to help with common XML manipulation functions.
 *
 * @version 1.0
 */
public class XmlUtils
{
  
  private static Logger log = Logger.getLogger(XmlUtils.class);
      /** Initializes the XML file to be parsed and gets the Document tree for it.
       *  @param File the XML file to parse
       *  @param boolean true if the file should be validated against its DTD; otherwise false.
       *  @exception FileNotFoundException if the specified file can not be located.
       *  @exception Exception if an error occurs parsing the file to a DOM.
       **/
      public static Document initializeFile( File pXmlFile, boolean pValidateDoc )
          throws FileNotFoundException, Exception
      {
        XMLToolsFactory factory = DefaultXMLToolsFactory.getInstance();
        XMLToDOMParser parser = factory.createXMLToDOMParser();
        ApplicationLoggingImpl logger = new ApplicationLoggingImpl( "[UnitTests.base:atg.junit.nucleus.XmlUtils]" );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(logger, true, true);
        return parser.parse(new FileInputStream( pXmlFile ), pValidateDoc, errorHandler);
      }

      /** retrieves the Node(s) represented within the DOM hierarchy at the location
       *  designated by the 'nested' child nodes.
       *  @param File the XML document in which to look
       *  @param boolean true if the file should be validated against its DTD; otherwise false.
       *  @param String[] the nested child nodes to retrieve. for example, if you specified
       *  an array { "foo", "bar", "flippy" } this method would return all 'flippy' Nodes for the XML
       *  document:
       *  <pre>
       *    &lt;foo&lt;
       *      &lt;bar&lt;
       *        &lt;flippy .../&lt;
       *        &lt;flippy .../&lt;
       *      &lt;/bar&lt;
       *    &lt;/foo&lt;
       *  </pre>
       *  @return List the requested child Nodes.  an empty List if no child Nodes exist.
       *  @exception FileNotFoundException if the specified file can not be located.
       *  @exception Exception if an error occurs parsing the file to a DOM.
       */
      public static List<Node>  getNodes( File pXmlFile, boolean pValidateDoc, String[] pChildren )
          throws FileNotFoundException, Exception
      {
          return getNodes( initializeFile( pXmlFile, pValidateDoc ), pChildren );
      }

      /** retrieves the Node(s) represented within the DOM hierarchy at the location
       *  designated by the 'nested' child nodes.
       *  @param Document the XML document parsed to a DOM
       *  @param String[] the nested child nodes to retrieve. for example, if you specified
       *  an array { "foo", "bar", "flippy" } this method would return all 'flippy' Nodes for the XML
       *  document:
       *  <pre>
       *    &lt;foo&lt;
       *      &lt;bar&lt;
       *        &lt;flippy .../&lt;
       *        &lt;flippy .../&lt;
       *      &lt;/bar&lt;
       *    &lt;/foo&lt;
       *  </pre>
       *  @return List the requested child Nodes.  an empty List if no child Nodes exist.
       *  null if the specified Document was null.
       */
      public static List<Node>  getNodes( Document pDocument, String[] pChildren )
      {
          if ( pDocument == null ) return null;
          return getNodes( pDocument.getDocumentElement(), pChildren );
      }

      /** retrieves the Node(s) represented within the DOM hierarchy at the location
       *  designated by the 'nested' child nodes.
       *  @param Node the Node at which to start searching
       *  @param String[] the nested child nodes to retrieve. for example, if you specified
       *  an array { "foo", "bar", "flippy" } this method would return all 'flippy' Nodes for the XML
       *  document:
       *  <pre>
       *    &lt;foo&lt;
       *      &lt;bar&lt;
       *        &lt;flippy .../&lt;
       *        &lt;flippy .../&lt;
       *      &lt;/bar&lt;
       *    &lt;/foo&lt;
       *  </pre>
       *  @return List the requested child Nodes.  an empty List if no child Nodes exist.
       */
      public static List<Node> getNodes( Node pNode, String[] pChildren ) {
          List<Node>  nodes = new LinkedList<Node>();

          if ( pNode == null ) {
              // do nothing
          } else if ( pChildren == null || pChildren.length == 0 ) {
              // if there are no more children, just return this Node
              nodes.add( pNode );
          } else {
              // otherwise recurse and get the children nodes...
              String[] children = new String[ pChildren.length - 1 ];
              for ( int j=0; j<children.length; j++ ) { children[j] = pChildren[j+1]; }

              NodeList nl = ((Element)pNode).getElementsByTagName( pChildren[0] );
              for ( int i=0; i<nl.getLength(); i++ ) {
                  nodes.addAll( getNodes(nl.item(i), children) );
              }
          }
          return nodes;
      }

      /** returns the Element NodeList for the specified child of the parent Node.
       *  @param Node the parent node
       *  @param String the name of the child node(s)
       *  @return NodeList the children of the parent Node.  null if pChild or pNode is null.
       */
      public static NodeList getNodes( Node pNode, String pChild ) {
          if ( pNode == null || pChild == null ) return null;
          return ((Element)pNode).getElementsByTagName( pChild );
      }

      /** Returns the String value of the content of the Node specified.
       *  @param Node the node whose content to get.
       *  @return String the value of the content of the Node.  An empty String if the Node
       *  does not have any content.
       */
      public static String getNodeTextValue(Node pElement) {
        NodeList children = pElement.getChildNodes();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.getLength(); i++) {
          if (children.item(i) instanceof Text) {
            Text n = (Text) children.item(i);
            sb.append(n.getNodeValue());
          }
        }
        String v = sb.toString();
        return v.toString();
      }

      /** gets the value of the named attribute.
       *  @param Node the node whose attribute should be retrieved.
       *  @param String the name of attribute whose value should be retrieved.
       *  @return String the value of the attribute.  null if the attribute is not defined
       *  or if a value has not been specified for it. */
      public static String getAttribute( Node pNode, String pName ) {
          return getAttribute( pNode, pName, null );
      }

      /** returns the value of the named attribute, or the specified default if the attribute
       *  value is null.
       *  @param Node the node whose attribute should be retrieved.
       *  @param String the name of attribute whose value should be retrieved.
       *  @param String the default value to return if a value does not exist for the specified
       *  attribute, or if the specified attribute is not defined in this Node.
       *  @return String the value of the attribute.  */
      public static String getAttribute( Node pNode, String pName, String pDefault ) {
          if ( pNode.getAttributes().getNamedItem(pName) == null ) return pDefault;
          return pNode.getAttributes().getNamedItem(pName).getNodeValue();
      }

      /** returns the value of the named attribute as an Integer object.
       *  @param Node the node whose attribute should be retrieved.
       *  @param String the name of attribute whose value should be retrieved.
       *  @param String the default value to return if a value does not exist for the specified
       *  attribute, or if the specified attribute is not defined in this Node.
       *  @return Integer the value of the attribute. If pAllowNull is true and
       *  no value is specified for the attribute then null is returned.
       *  @exception FileFormatException if the value specified by the attribute can not be converted to an Integer,
       *  or if pAllowNull is false and no value was specified for the attribute. */
      public static Integer getIntegerAttribute( Node pNode, String pName, boolean pAllowNull )
          throws FileFormatException
      {
          Node n = pNode.getAttributes().getNamedItem( pName );
          if ( n == null && pAllowNull ) return null;
          if ( n == null && !pAllowNull ) throw new FileFormatException("No value specified for required attribute '" + pName + "'.");
          return getIntegerValue( n.getNodeValue(), pName, pAllowNull );
      }

      /** converts the specified String into an Integer.
       *  @param String the value to convert.
       *  @param String a descriptor of what the value is for.
       *  @param boolean true if the input value can be null; otherwise false.
       *  @return Integer the Integer value of the String input value.
       *  @exception FileFormatException if the String can not be converted to an Integer or if it is
       *  null and pAllowNull is false.  */
      public static Integer getIntegerValue( String pValue, String pDescriptor, boolean pAllowNull )
          throws FileFormatException
      {
          if ( (pValue == null || pValue.trim().length() == 0 ) && pAllowNull ) return null;
          try {
            return new Integer( pValue.trim() );
          } catch (Throwable t) {
              throw new FileFormatException("Invalid Integer value '" + pValue + "' specified for attribute '" + pDescriptor + "'.");
          }
      }

      /** returns the value of the named attribute as a Long object.
       *  @param Node the node whose attribute should be retrieved.
       *  @param String the name of attribute whose value should be retrieved.
       *  @param String the default value to return if a value does not exist for the specified
       *  attribute, or if the specified attribute is not defined in this Node.
       *  @return Long the value of the attribute. If pAllowNull is true and
       *  no value is specified for the attribute then null is returned.
       *  @exception FileFormatException if the value specified by the attribute can not be converted to a Long,
       *  or if pAllowNull is false and no value was specified for the attribute. */
      public static Long getLongAttribute( Node pNode, String pName, boolean pAllowNull )
          throws FileFormatException
      {
          Node n = pNode.getAttributes().getNamedItem( pName );
          if ( n == null && pAllowNull ) return null;
          if ( n == null && !pAllowNull ) throw new FileFormatException("No value specified for required attribute '" + pName + "'.");
          return getLongValue( n.getNodeValue(), pName, pAllowNull );
      }

      /** converts the specified String into a Long.
       *  @param String the value to convert.
       *  @param String a descriptor of what the value is for.
       *  @param boolean true if the input value can be null; otherwise false.
       *  @return Long the Long value of the String input value.
       *  @exception FileFormatException if the String can not be converted to a Long or if it is
       *  null and pAllowNull is false.  */
      public static Long getLongValue( String pValue, String pDescriptor, boolean pAllowNull )
          throws FileFormatException
      {
          if ( (pValue == null || pValue.trim().length() == 0 ) && pAllowNull ) return null;
          try {
            return new Long( pValue.trim() );
          } catch (Throwable t) {
              throw new FileFormatException("Invalid Long value '" + pValue + "' specified for attribute '" + pDescriptor + "'.");
          }
      }

      /** returns the value of the named attribute as a boolean.
       *  @param Node the node whose attribute should be retrieved.
       *  @param String the name of attribute whose value should be retrieved.
       *  @param boolean the default value to return if a value does not exist for the specified
       *  attribute, or if the specified attribute is not defined in this Node.
       *  @return boolean the value of the attribute. If no value was specified for the
       *  attribute then the default value is returned.
       *  @exception FileFormatException if the value specified by the attribute can not be converted to a boolean.
       */
      public static boolean getBooleanAttribute( Node pNode, String pName, boolean pDefault )
          throws FileFormatException
      {
          Node n = pNode.getAttributes().getNamedItem( pName );
          if ( n == null ) return pDefault;
          return getBooleanValue( n.getNodeValue(), pName, pDefault );
      }

      /** converts the specified value into a boolean.
       *  @param String the value which should be converted.
       *  @param String a descriptor of what the value is for.
       *  @param boolean the default value to return if the specified value is null or empty.
       *  @return boolean the converted value. If the specified value was null or empty
       *  then the default value is returned.
       *  @exception FileFormatException if the specified value can not be converted to a boolean.
       */
      public static boolean getBooleanValue( String pValue, String pDescriptor, boolean pDefault )
          throws FileFormatException
      {
          if ( pValue == null || pValue.trim().length() == 0 ) return pDefault;
          if ( ! pValue.trim().equalsIgnoreCase("true") && ! pValue.trim().equalsIgnoreCase("false") )
              throw new FileFormatException("Invalid Boolean value '" + pValue + "' specified for attribute '" + pDescriptor +
                                                   "'. Must be [true|false]");
          return new Boolean(pValue.trim()).booleanValue();
      }

      /** returns the value of the named attribute as a Boolean object.
       *  @param Node the node whose attribute should be retrieved.
       *  @param String the name of attribute whose value should be retrieved.
       *  @param Boolean the default value to return if a value does not exist for the specified
       *  attribute, or if the specified attribute is not defined in this Node.
       *  @return Boolean the value of the attribute. If no value was specified for the
       *  attribute then the default value is returned.
       *  @exception FileFormatException if the value specified by the attribute can not be converted to a boolean.
       */
      public static Boolean getBooleanAttribute( Node pNode, String pName, Boolean pDefault )
          throws FileFormatException
      {
          Node n = pNode.getAttributes().getNamedItem( pName );
          if ( n == null ) return pDefault;
          String v = n.getNodeValue();
          if ( v == null || v.length() == 0 ) return pDefault;
          return new Boolean( getBooleanValue( v, pName, true ) );  // the default 'true' passed to this call should not never to be used
      }

      /* a main method for testing these functions */
      public static void main( String[] pArgs ) {
          String file = "C:\\temp\\registry.xml";
		  //String file = "/work/systest/qma/temp/registry.xml";
          log.info("bea's registry file:=[" + file + "]");
          String[] children = { "host", "product", "release" };
          try {
            List<Node>  nodes = getNodes( new File(file), false, children );
            if ( nodes == null ) {
                System.out.print("Nodes is null.");
            } else if ( nodes.size() == 0 ) {
                System.out.print("Nodes is empty.");
            } else {
                Iterator<Node>  iter = nodes.iterator();
                while ( iter.hasNext() ) {
                    Node n = iter.next();
                    log.info("Got Node: " + getAttribute(n, "level") + "/" + getAttribute(n,"ServicePackLevel") );
                }
            }
          } catch (Throwable t) {
              t.printStackTrace();
          }

          log.info("-------------------");
          log.info("BEA Version: " + atg.junit.nucleus.TestUtils.getBeaVersion() );
          //System.setProperty("bea.home","c:\\bea");
          //log.info("BEA Version: " + atg.junit.nucleus.TestUtils.getBeaVersion() );

      }
}