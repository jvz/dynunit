package atg.servlet;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import atg.droplet.TagConverter;
import atg.naming.NameContext;
import atg.nucleus.Nucleus;
import atg.nucleus.RequestScopeManager;
import atg.nucleus.WindowScopeManager;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.naming.ComponentName;
import atg.nucleus.naming.ParameterName;
import atg.security.UserAuthenticator;
import atg.server.http.TestingHttpConnectionServletInputStream;
import atg.servlet.exittracking.ExitTrackingHandler;
import atg.servlet.minimal.WebApplicationInterface;
import atg.test.io.ByteBufferInputStream;
import atg.test.io.ByteBufferOutputStream;

/**
 * A wrapper around DynamoHttpServletRequest to allow for adding some test
 * specific methods.
 */
public class TestingDynamoHttpServletRequest extends atg.servlet.ServletTestUtils.TestingDynamoHttpServletRequest {
  
  // ------------------------------
  /**
   * Constructs a new TestingDynamoHttpServletRequest which wraps the given
   * request object.
   * 
   * NOTE: The getLog() method of DynamoHttpServletRequest is final and cannot be overriden in this
   * test version. Therefore you cannot depend upon the functionality of this method call in the test class.
   * @param pRequest
   */
  public TestingDynamoHttpServletRequest(DynamoHttpServletRequest pRequest,
      ByteBuffer pBuffer) {
    super(pRequest,pBuffer);
  }
  
}