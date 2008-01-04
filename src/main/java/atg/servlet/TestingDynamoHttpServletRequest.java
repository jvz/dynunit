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
public class TestingDynamoHttpServletRequest extends DynamoHttpServletRequest {
  /**
   * 
   */
  private DynamoHttpServletRequest mRequest = null;
  private ByteBuffer mBuffer = null;
  ByteBufferOutputStream mClientOutputStream = null;
  ServletInputStream mInputStream = null;

  // ------------------------------
  /**
   * Constructs a new TestingDynamoHttpServletRequest which wraps the given
   * request object.
   * 
   * NOTE: The getLog() method of DynamoHttpServletRequest is final and cannot be overriden in this
   * test version. Therefore you cannot depend upon the functionality of this method call in the test class.
   * @param pRequest
   * 
   */
  public TestingDynamoHttpServletRequest(DynamoHttpServletRequest pRequest,
      ByteBuffer pBuffer) {
    mRequest = pRequest;
    mBuffer = pBuffer;
  }

  // ------------------------------
  /**
   * Returns an output stream to which data can be written. This simulates the
   * stream to which an HTTP client would write to a server. IMPORTANT: Be
   * sure to call <code>prepareForRead()</code> after you are done writing
   * to this stream. This allows that data to be read from the input stream
   * obtained by calling getInputStream() on the underlying request.
   * 
   * 
   * @param pNew
   *          if true, a new stream is always created. Otherwise the previous
   *          stream is returned.
   */
  public OutputStream getClientOutputStream(boolean pNew) {
    if (mClientOutputStream == null || pNew)
      mClientOutputStream = new ByteBufferOutputStream(mBuffer);
    return mClientOutputStream;
  }

  // ------------------------------
  /**
   * Flips the underlying buffer to the client output stream so that it can be
   * read from. This method must be invoked after content has been written to
   * the output stream obtained by calling <code>getClientOutputStream</code>
   * 
   */
  public void prepareForRead() {
    if (mBuffer != null)
      mBuffer.flip();
    mInputStream = new TestingHttpConnectionServletInputStream(
        new ByteBufferInputStream(mBuffer), mBuffer.limit());
    mRequest.setInputStream(mInputStream);
  }

  // ------------------------------
  // DynamoHttpServletRequest delegate methods beyond this point
  //
  
  /**
   * @param pKey
   * @param pValue
   * @see atg.servlet.DynamoHttpServletRequest#addPersistentQueryParameter(java.lang.String, java.lang.String)
   */
  public void addPersistentQueryParameter(String pKey, String pValue) {
    mRequest.addPersistentQueryParameter(pKey, pValue);
  }

  /**
   * @param pKey
   * @param pValue
   * @see atg.servlet.DynamoHttpServletRequest#addQueryParameter(java.lang.String, java.lang.String)
   */
  public void addQueryParameter(String pKey, String pValue) {
    mRequest.addQueryParameter(pKey, pValue);
  }

  /**
   * @param pKey
   * @param pValue
   * @see atg.servlet.DynamoHttpServletRequest#addURLParameter(java.lang.String, java.lang.String)
   */
  public void addURLParameter(String pKey, String pValue) {
    mRequest.addURLParameter(pKey, pValue);
  }

  /**
   * @param pURL
   * @param pEncodeParameters
   * @param pClearParameters
   * @param pIsImageURL
   * @param pInterpretURIs
   * @param pDoExitTracking
   * @param pPrependMode
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#encodeURL(java.lang.String, boolean, boolean, boolean, boolean, boolean, int)
   */
  public String encodeURL(String pURL, boolean pEncodeParameters,
      boolean pClearParameters, boolean pIsImageURL, boolean pInterpretURIs,
      boolean pDoExitTracking, int pPrependMode) {
    return mRequest.encodeURL(pURL, pEncodeParameters, pClearParameters,
        pIsImageURL, pInterpretURIs, pDoExitTracking, pPrependMode);
  }

  /**
   * @param pURL
   * @param pEncodeParameters
   * @param pClearParameters
   * @param pIsImageURL
   * @param pInterpretURIs
   * @param pDoExitTracking
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#encodeURL(java.lang.String, boolean, boolean, boolean, boolean, boolean)
   */
  public String encodeURL(String pURL, boolean pEncodeParameters,
      boolean pClearParameters, boolean pIsImageURL, boolean pInterpretURIs,
      boolean pDoExitTracking) {
    return mRequest.encodeURL(pURL, pEncodeParameters, pClearParameters,
        pIsImageURL, pInterpretURIs, pDoExitTracking);
  }

  /**
   * @param pURL
   * @param pEncodeParameters
   * @param pClearParameters
   * @param pIsImageURL
   * @param pInterpretURIs
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#encodeURL(java.lang.String, boolean, boolean, boolean, boolean)
   */
  public String encodeURL(String pURL, boolean pEncodeParameters,
      boolean pClearParameters, boolean pIsImageURL, boolean pInterpretURIs) {
    return mRequest.encodeURL(pURL, pEncodeParameters, pClearParameters,
        pIsImageURL, pInterpretURIs);
  }

  /**
   * @param pURL
   * @param pEncodeParameters
   * @param pClearParameters
   * @param pIsImageURL
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#encodeURL(java.lang.String, boolean, boolean, boolean)
   */
  public String encodeURL(String pURL, boolean pEncodeParameters,
      boolean pClearParameters, boolean pIsImageURL) {
    return mRequest.encodeURL(pURL, pEncodeParameters, pClearParameters,
        pIsImageURL);
  }

  /**
   * @param pURL
   * @param pClearParameters
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#encodeURL(java.lang.String, boolean)
   */
  public String encodeURL(String pURL, boolean pClearParameters) {
    return mRequest.encodeURL(pURL, pClearParameters);
  }

  /**
   * @param pURL
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#encodeURL(java.lang.String)
   */
  public String encodeURL(String pURL) {
    return mRequest.encodeURL(pURL);
  }

  /**
   * 
   * @see atg.servlet.DynamoHttpServletRequest#endRequest()
   */
  public void endRequest() {
    mRequest.endRequest();
  }

  /**
   * @param pObj
   * @return
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object pObj) {
    return mRequest.equals(pObj);
  }

  /**
   * @param pP0
   * @param pCreate
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getAttribute(java.lang.String, boolean)
   */
  public Object getAttribute(String pP0, boolean pCreate) {
    return mRequest.getAttribute(pP0, pCreate);
  }

  /**
   * @param pP0
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getAttribute(java.lang.String)
   */
  public Object getAttribute(String pP0) {
    return mRequest.getAttribute(pP0);
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getAttributeNames()
   */
  public Enumeration getAttributeNames() {
    return mRequest.getAttributeNames();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getAuthType()
   */
  public String getAuthType() {
    return mRequest.getAuthType();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getBaseDirectory()
   */
  public String getBaseDirectory() {
    return mRequest.getBaseDirectory();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getBrowserTyper()
   */
  public BrowserTyper getBrowserTyper() {
    return mRequest.getBrowserTyper();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getCharacterEncoding()
   */
  public String getCharacterEncoding() {
    return mRequest.getCharacterEncoding();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getContentLength()
   */
  public int getContentLength() {
    return mRequest.getContentLength();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getContentType()
   */
  public String getContentType() {
    return mRequest.getContentType();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getContextPath()
   */
  public String getContextPath() {
    return mRequest.getContextPath();
  }

  /**
   * @param pKey
   * @param pIndex
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getCookieParameter(java.lang.String, int)
   */
  public String getCookieParameter(String pKey, int pIndex) {
    return mRequest.getCookieParameter(pKey, pIndex);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getCookieParameter(java.lang.String)
   */
  public String getCookieParameter(String pKey) {
    return mRequest.getCookieParameter(pKey);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getCookieParameterCount(java.lang.String)
   */
  public int getCookieParameterCount(String pKey) {
    return mRequest.getCookieParameterCount(pKey);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getCookieParameterNames()
   */
  public Enumeration getCookieParameterNames() {
    return mRequest.getCookieParameterNames();
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getCookieParameterValues(java.lang.String)
   */
  public String[] getCookieParameterValues(String pKey) {
    return mRequest.getCookieParameterValues(pKey);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getCookies()
   */
  public Cookie[] getCookies() {
    return mRequest.getCookies();
  }

  /**
   * @param pP0
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getDateHeader(java.lang.String)
   */
  public long getDateHeader(String pP0) {
    return mRequest.getDateHeader(pP0);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getDisableExitTracking()
   */
  public boolean getDisableExitTracking() {
    return mRequest.getDisableExitTracking();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getDocRootServicePrefix()
   */
  public String getDocRootServicePrefix() {
    return mRequest.getDocRootServicePrefix();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getEncodeContextPathMode()
   */
  public int getEncodeContextPathMode() {
    return mRequest.getEncodeContextPathMode();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getEncodeServletPath()
   */
  public boolean getEncodeServletPath() {
    return mRequest.getEncodeServletPath();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getEncodeURL()
   */
  public boolean getEncodeURL() {
    return mRequest.getEncodeURL();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getEventFlags()
   */
  public int getEventFlags() {
    return mRequest.getEventFlags();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getExitTrackingHandler()
   */
  public ExitTrackingHandler getExitTrackingHandler() {
    return mRequest.getExitTrackingHandler();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getExitTrackingParameterName()
   */
  public String getExitTrackingParameterName() {
    return mRequest.getExitTrackingParameterName();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getFormEventsSent()
   */
  public boolean getFormEventsSent() {
    return mRequest.getFormEventsSent();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getGenerateRequestLocales()
   */
  public boolean getGenerateRequestLocales() {
    return mRequest.getGenerateRequestLocales();
  }

  /**
   * @param pP0
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getHeader(java.lang.String)
   */
  public String getHeader(String pP0) {
    return mRequest.getHeader(pP0);
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getHeaderNames()
   */
  public Enumeration getHeaderNames() {
    return mRequest.getHeaderNames();
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getHeaders(java.lang.String)
   */
  public Enumeration getHeaders(String pName) {
    return mRequest.getHeaders(pName);
  }

  /**
   * @return
   * @throws IOException
   * @see atg.servlet.MutableHttpServletRequest#getInputStream()
   */
  public ServletInputStream getInputStream() throws IOException {
    if (mRequest.getInputStream() == null)
      throw new NullPointerException("Underlying input stream is null. Did you call prepareForRead() ?");
    return mRequest.getInputStream();
  }

  /**
   * @param pP0
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getIntHeader(java.lang.String)
   */
  public int getIntHeader(String pP0) {
    return mRequest.getIntHeader(pP0);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getLinkEncoding()
   */
  public String getLinkEncoding() {
    return mRequest.getLinkEncoding();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getLocalAddr()
   */
  public String getLocalAddr() {
    return mRequest.getLocalAddr();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getLocale()
   */
  public Locale getLocale() {
    return mRequest.getLocale();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getLocales()
   */
  public Enumeration getLocales() {
    return mRequest.getLocales();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getLocalName()
   */
  public String getLocalName() {
    return mRequest.getLocalName();
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getLocalParameter(atg.nucleus.naming.ParameterName)
   */
  public Object getLocalParameter(ParameterName pName) {
    return mRequest.getLocalParameter(pName);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getLocalParameter(java.lang.String)
   */
  public Object getLocalParameter(String pName) {
    return mRequest.getLocalParameter(pName);
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getLocalPort()
   */
  public int getLocalPort() {
    return mRequest.getLocalPort();
  }
  
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getMapForCurrentFrame()
   */
  public Map getMapForCurrentFrame() {
    return mRequest.getMapForCurrentFrame();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getMethod()
   */
  public String getMethod() {
    return mRequest.getMethod();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getMimeType()
   */
  public String getMimeType() {
    return mRequest.getMimeType();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getMimeTyper()
   */
  public MimeTyper getMimeTyper() {
    return mRequest.getMimeTyper();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getNucleus()
   */
  public Nucleus getNucleus() {
    return mRequest.getNucleus();
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getObjectParameter(atg.nucleus.naming.ParameterName)
   */
  public Object getObjectParameter(ParameterName pName) {
    return mRequest.getObjectParameter(pName);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getObjectParameter(java.lang.String)
   */
  public Object getObjectParameter(String pName) {
    return mRequest.getObjectParameter(pName);
  }

  /**
   * @param pKey
   * @param pIndex
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getObjectURLParameter(java.lang.String, int)
   */
  public Object getObjectURLParameter(String pKey, int pIndex) {
    return mRequest.getObjectURLParameter(pKey, pIndex);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParameter(atg.nucleus.naming.ParameterName)
   */
  public String getParameter(ParameterName pName) {
    return mRequest.getParameter(pName);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParameter(java.lang.String)
   */
  public String getParameter(String pName) {
    return mRequest.getParameter(pName);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParameterDelimiter()
   */
  public String getParameterDelimiter() {
    return mRequest.getParameterDelimiter();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getParameterMap()
   */
  public Map getParameterMap() {
    return mRequest.getParameterMap();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParameterNames()
   */
  public Enumeration getParameterNames() {
    return mRequest.getParameterNames();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParameterNamesInStack()
   */
  public Enumeration getParameterNamesInStack() {
    return mRequest.getParameterNamesInStack();
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParameterValues(java.lang.String)
   */
  public String[] getParameterValues(String pName) {
    return mRequest.getParameterValues(pName);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getParamMapForTopFrame()
   */
  public Map getParamMapForTopFrame() {
    return mRequest.getParamMapForTopFrame();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getPathInfo()
   */
  public String getPathInfo() {
    return mRequest.getPathInfo();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getPathTranslated()
   */
  public String getPathTranslated() {
    return mRequest.getPathTranslated();
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getPermanentAttribute(atg.servlet.AttributeFactory)
   */
  public Object getPermanentAttribute(AttributeFactory pKey) {
    return mRequest.getPermanentAttribute(pKey);
  }

  /**
   * @param pKey
   * @param pIndex
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getPostParameter(java.lang.String, int)
   */
  public String getPostParameter(String pKey, int pIndex) {
    return mRequest.getPostParameter(pKey, pIndex);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getPostParameter(java.lang.String)
   */
  public String getPostParameter(String pKey) {
    return mRequest.getPostParameter(pKey);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getPostParameterCount(java.lang.String)
   */
  public int getPostParameterCount(String pKey) {
    return mRequest.getPostParameterCount(pKey);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getPostParameterNames()
   */
  public Enumeration getPostParameterNames() {
    return mRequest.getPostParameterNames();
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getPostParameterValues(java.lang.String)
   */
  public String[] getPostParameterValues(String pKey) {
    return mRequest.getPostParameterValues(pKey);
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getProtocol()
   */
  public String getProtocol() {
    return mRequest.getProtocol();
  }

  /**
   * @param pKey
   * @param pIndex
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getQueryParameter(java.lang.String, int)
   */
  public String getQueryParameter(String pKey, int pIndex) {
    return mRequest.getQueryParameter(pKey, pIndex);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getQueryParameter(java.lang.String)
   */
  public String getQueryParameter(String pKey) {
    return mRequest.getQueryParameter(pKey);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getQueryParameterCount(java.lang.String)
   */
  public int getQueryParameterCount(String pKey) {
    return mRequest.getQueryParameterCount(pKey);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getQueryParameterNames()
   */
  public Enumeration getQueryParameterNames() {
    return mRequest.getQueryParameterNames();
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getQueryParameterValues(java.lang.String)
   */
  public String[] getQueryParameterValues(String pKey) {
    return mRequest.getQueryParameterValues(pKey);
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getQueryString()
   */
  public String getQueryString() {
    return mRequest.getQueryString();
  }

  /**
   * @return
   * @throws IOException
   * @see atg.servlet.MutableHttpServletRequest#getReader()
   */
  public BufferedReader getReader() throws IOException {
    return mRequest.getReader();
  }

  /**
   * @param pPath
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRealPath(java.lang.String)
   */
  public String getRealPath(String pPath) {
    return mRequest.getRealPath(pPath);
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getRemoteAddr()
   */
  public String getRemoteAddr() {
    return mRequest.getRemoteAddr();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getRemoteHost()
   */
  public String getRemoteHost() {
    return mRequest.getRemoteHost();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getRemotePort()
   */
  public int getRemotePort() {
    return mRequest.getRemotePort();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getRemoteUser()
   */
  public String getRemoteUser() {
    return mRequest.getRemoteUser();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getRequest()
   */
  public HttpServletRequest getRequest() {
    return mRequest.getRequest();
  }

  /**
   * @param pContext
   * @param pPath
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestDispatcher(javax.servlet.ServletContext, java.lang.String)
   */
  public RequestDispatcher getRequestDispatcher(ServletContext pContext,
      String pPath) {
    return mRequest.getRequestDispatcher(pContext, pPath);
  }

  /**
   * @param pPath
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestDispatcher(java.lang.String)
   */
  public RequestDispatcher getRequestDispatcher(String pPath) {
    return mRequest.getRequestDispatcher(pPath);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestedSessionId()
   */
  public String getRequestedSessionId() {
    return mRequest.getRequestedSessionId();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestForComparison()
   */
  public DynamoHttpServletRequest getRequestForComparison() {
    return mRequest.getRequestForComparison();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestLocale()
   */
  public RequestLocale getRequestLocale() {
    return mRequest.getRequestLocale();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestLocalePath()
   */
  public ComponentName getRequestLocalePath() {
    return mRequest.getRequestLocalePath();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestScope()
   */
  public NameContext getRequestScope() {
    return mRequest.getRequestScope();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestScopeManager()
   */
  public RequestScopeManager getRequestScopeManager() {
    return mRequest.getRequestScopeManager();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestURI()
   */
  public String getRequestURI() {
    return mRequest.getRequestURI();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRequestURIWithQueryString()
   */
  public String getRequestURIWithQueryString() {
    return mRequest.getRequestURIWithQueryString();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getRequestURL()
   */
  public StringBuffer getRequestURL() {
    return mRequest.getRequestURL();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getResponse()
   */
  public DynamoHttpServletResponse getResponse() {
    return mRequest.getResponse();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getRestorableSessionIdFromURL()
   */
  public String getRestorableSessionIdFromURL() {
    return mRequest.getRestorableSessionIdFromURL();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getScheme()
   */
  public String getScheme() {
    return mRequest.getScheme();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getScrambleKey()
   */
  public byte[] getScrambleKey() {
    return mRequest.getScrambleKey();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getServerName()
   */
  public String getServerName() {
    return mRequest.getServerName();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getServerPort()
   */
  public int getServerPort() {
    return mRequest.getServerPort();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#getServletPath()
   */
  public String getServletPath() {
    return mRequest.getServletPath();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getSession()
   */
  public HttpSession getSession() {
    return mRequest.getSession();
  }

  /**
   * @param pCreate
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getSession(boolean)
   */
  public HttpSession getSession(boolean pCreate) {
    return mRequest.getSession(pCreate);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getSessionConfirmationNumber()
   */
  public long getSessionConfirmationNumber() {
    return mRequest.getSessionConfirmationNumber();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getSessionNameContext()
   */
  public NameContext getSessionNameContext() {
    return mRequest.getSessionNameContext();
  }

  /**
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#getSessionRequest()
   */
  public HttpSessionRequest getSessionRequest() {
    return mRequest.getSessionRequest();
  }

  /**
   * @param pCreate
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#getSessionRequest(boolean)
   */
  public HttpSessionRequest getSessionRequest(boolean pCreate) {
    return mRequest.getSessionRequest(pCreate);
  }

  /**
   * @param pKey
   * @param pIndex
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLParameter(java.lang.String, int)
   */
  public String getURLParameter(String pKey, int pIndex) {
    return mRequest.getURLParameter(pKey, pIndex);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLParameter(java.lang.String)
   */
  public String getURLParameter(String pKey) {
    return mRequest.getURLParameter(pKey);
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLParameterCount(java.lang.String)
   */
  public int getURLParameterCount(String pKey) {
    return mRequest.getURLParameterCount(pKey);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLParameterNames()
   */
  public Enumeration getURLParameterNames() {
    return mRequest.getURLParameterNames();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLParameterString()
   */
  public String getURLParameterString() {
    return mRequest.getURLParameterString();
  }

  /**
   * @param pKey
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLParameterValues(java.lang.String)
   */
  public String[] getURLParameterValues(String pKey) {
    return mRequest.getURLParameterValues(pKey);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getURLSessionIdSpecifier()
   */
  public String getURLSessionIdSpecifier() {
    return mRequest.getURLSessionIdSpecifier();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getUserAuthenticator()
   */
  public UserAuthenticator getUserAuthenticator() {
    return mRequest.getUserAuthenticator();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getUserPrincipal()
   */
  public Principal getUserPrincipal() {
    return mRequest.getUserPrincipal();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getWebApplication()
   */
  public WebApplicationInterface getWebApplication() {
    return mRequest.getWebApplication();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getWebPools()
   */
  public WebPools getWebPools() {
    return mRequest.getWebPools();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getWindowScope()
   */
  public NameContext getWindowScope() {
    return mRequest.getWindowScope();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getWindowScopeManager()
   */
  public WindowScopeManager getWindowScopeManager() {
    return mRequest.getWindowScopeManager();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getWorkingDirectory()
   */
  public String getWorkingDirectory() {
    return mRequest.getWorkingDirectory();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#getWrapper()
   */
  public ServletRequestWrapper getWrapper() {
    return mRequest.getWrapper();
  }

  /**
   * @return
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return mRequest.hashCode();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#hasRequestScope()
   */
  public boolean hasRequestScope() {
    return mRequest.hasRequestScope();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isAfterGetsClaimed()
   */
  public boolean isAfterGetsClaimed() {
    return mRequest.isAfterGetsClaimed();
  }

  /**
   * @param pFeature
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isBrowserType(java.lang.String)
   */
  public boolean isBrowserType(String pFeature) {
    return mRequest.isBrowserType(pFeature);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isDelayedRequest()
   */
  public boolean isDelayedRequest() {
    return mRequest.isDelayedRequest();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isDynamoPipeline()
   */
  public boolean isDynamoPipeline() {
    return mRequest.isDynamoPipeline();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isInitialized()
   */
  public boolean isInitialized() {
    return mRequest.isInitialized();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isInTemplatePage()
   */
  public boolean isInTemplatePage() {
    return mRequest.isInTemplatePage();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isLoggingDebug()
   */
  public boolean isLoggingDebug() {
    return mRequest.isLoggingDebug();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isLoggingError()
   */
  public boolean isLoggingError() {
    return mRequest.isLoggingError();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isLoggingInfo()
   */
  public boolean isLoggingInfo() {
    return mRequest.isLoggingInfo();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isLoggingWarning()
   */
  public boolean isLoggingWarning() {
    return mRequest.isLoggingWarning();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isRequestedSessionIdFromCookie()
   */
  public boolean isRequestedSessionIdFromCookie() {
    return mRequest.isRequestedSessionIdFromCookie();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isRequestedSessionIdFromUrl()
   */
  public boolean isRequestedSessionIdFromUrl() {
    return mRequest.isRequestedSessionIdFromUrl();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#isRequestedSessionIdFromURL()
   */
  public boolean isRequestedSessionIdFromURL() {
    return mRequest.isRequestedSessionIdFromURL();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isRequestedSessionIdValid()
   */
  public boolean isRequestedSessionIdValid() {
    return mRequest.isRequestedSessionIdValid();
  }

  /**
   * @return
   * @see atg.servlet.MutableHttpServletRequest#isSecure()
   */
  public boolean isSecure() {
    return mRequest.isSecure();
  }

  /**
   * @param pRole
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#isUserInRole(java.lang.String)
   */
  public boolean isUserInRole(String pRole) {
    return mRequest.isUserInRole(pRole);
  }

  /**
   * @param pMessage
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logDebug(java.lang.String, java.lang.Throwable)
   */
  public void logDebug(String pMessage, Throwable pThrowable) {
    mRequest.logDebug(pMessage, pThrowable);
  }

  /**
   * @param pMessage
   * @see atg.servlet.DynamoHttpServletRequest#logDebug(java.lang.String)
   */
  public void logDebug(String pMessage) {
    mRequest.logDebug(pMessage);
  }

  /**
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logDebug(java.lang.Throwable)
   */
  public void logDebug(Throwable pThrowable) {
    mRequest.logDebug(pThrowable);
  }

  /**
   * @param pMessage
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logError(java.lang.String, java.lang.Throwable)
   */
  public void logError(String pMessage, Throwable pThrowable) {
    mRequest.logError(pMessage, pThrowable);
  }

  /**
   * @param pMessage
   * @see atg.servlet.DynamoHttpServletRequest#logError(java.lang.String)
   */
  public void logError(String pMessage) {
    mRequest.logError(pMessage);
  }

  /**
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logError(java.lang.Throwable)
   */
  public void logError(Throwable pThrowable) {
    mRequest.logError(pThrowable);
  }

  /**
   * @param pMessage
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logInfo(java.lang.String, java.lang.Throwable)
   */
  public void logInfo(String pMessage, Throwable pThrowable) {
    mRequest.logInfo(pMessage, pThrowable);
  }

  /**
   * @param pMessage
   * @see atg.servlet.DynamoHttpServletRequest#logInfo(java.lang.String)
   */
  public void logInfo(String pMessage) {
    mRequest.logInfo(pMessage);
  }

  /**
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logInfo(java.lang.Throwable)
   */
  public void logInfo(Throwable pThrowable) {
    mRequest.logInfo(pThrowable);
  }

  /**
   * @param pMessage
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logWarning(java.lang.String, java.lang.Throwable)
   */
  public void logWarning(String pMessage, Throwable pThrowable) {
    mRequest.logWarning(pMessage, pThrowable);
  }

  /**
   * @param pMessage
   * @see atg.servlet.DynamoHttpServletRequest#logWarning(java.lang.String)
   */
  public void logWarning(String pMessage) {
    mRequest.logWarning(pMessage);
  }

  /**
   * @param pThrowable
   * @see atg.servlet.DynamoHttpServletRequest#logWarning(java.lang.Throwable)
   */
  public void logWarning(Throwable pThrowable) {
    mRequest.logWarning(pThrowable);
  }

  /**
   * @param pURL
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#performExitTracking(java.lang.String)
   */
  public String performExitTracking(String pURL) {
    return mRequest.performExitTracking(pURL);
  }

  /**
   * 
   * @see atg.servlet.DynamoHttpServletRequest#popDefaultParameters()
   */
  public void popDefaultParameters() {
    mRequest.popDefaultParameters();
  }

  /**
   * 
   * @see atg.servlet.DynamoHttpServletRequest#popFrame()
   */
  public void popFrame() {
    mRequest.popFrame();
  }

  /**
   * 
   * @see atg.servlet.DynamoHttpServletRequest#popParameters()
   */
  public void popParameters() {
    mRequest.popParameters();
  }

  /**
   * @param pOut
   * @see atg.servlet.DynamoHttpServletRequest#printRequest(java.io.PrintStream)
   */
  public void printRequest(PrintStream pOut) {
    mRequest.printRequest(pOut);
  }

  /**
   * @param pDict
   * @see atg.servlet.DynamoHttpServletRequest#pushDefaultParameters(java.util.Dictionary)
   */
  public void pushDefaultParameters(Dictionary pDict) {
    mRequest.pushDefaultParameters(pDict);
  }

  /**
   * 
   * @see atg.servlet.DynamoHttpServletRequest#pushFrame()
   */
  public void pushFrame() {
    mRequest.pushFrame();
  }

  /**
   * @param pDict
   * @see atg.servlet.DynamoHttpServletRequest#pushParameters(java.util.Dictionary)
   */
  public void pushParameters(Dictionary pDict) {
    mRequest.pushParameters(pDict);
  }

  /**
   * @param pName
   * @see atg.servlet.MutableHttpServletRequest#removeAttribute(java.lang.String)
   */
  public void removeAttribute(String pName) {
    mRequest.removeAttribute(pName);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#removeParameter(java.lang.String)
   */
  public Object removeParameter(String pName) {
    return mRequest.removeParameter(pName);
  }

  /**
   * @param pKey
   * @see atg.servlet.DynamoHttpServletRequest#removePersistentQueryParameter(java.lang.String)
   */
  public void removePersistentQueryParameter(String pKey) {
    mRequest.removePersistentQueryParameter(pKey);
  }

  /**
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#removeSessionFromRequest()
   */
  public void removeSessionFromRequest() {
    mRequest.removeSessionFromRequest();
  }

  /**
   * @param pName
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#resolveGlobalName(atg.nucleus.naming.ComponentName)
   */
  public Object resolveGlobalName(ComponentName pName) {
    return mRequest.resolveGlobalName(pName);
  }

  /**
   * @param pName
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#resolveGlobalName(java.lang.String)
   */
  public Object resolveGlobalName(String pName) {
    return mRequest.resolveGlobalName(pName);
  }

  /**
   * @param pName
   * @param pCreate
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#resolveName(atg.nucleus.naming.ComponentName, boolean)
   */
  public Object resolveName(ComponentName pName, boolean pCreate) {
    return mRequest.resolveName(pName, pCreate);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#resolveName(atg.nucleus.naming.ComponentName)
   */
  public Object resolveName(ComponentName pName) {
    return mRequest.resolveName(pName);
  }

  /**
   * @param pName
   * @param pCreate
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#resolveName(java.lang.String, boolean)
   */
  public Object resolveName(String pName, boolean pCreate) {
    return mRequest.resolveName(pName, pCreate);
  }

  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#resolveName(java.lang.String)
   */
  public Object resolveName(String pName) {
    return mRequest.resolveName(pName);
  }

  /**
   * @param pName
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#resolveRequestName(atg.nucleus.naming.ComponentName)
   */
  public Object resolveRequestName(ComponentName pName) {
    return mRequest.resolveRequestName(pName);
  }

  /**
   * @param pName
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#resolveRequestName(java.lang.String)
   */
  public Object resolveRequestName(String pName) {
    return mRequest.resolveRequestName(pName);
  }

  /**
   * @param pName
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#resolveSessionName(atg.nucleus.naming.ComponentName)
   */
  public Object resolveSessionName(ComponentName pName) {
    return mRequest.resolveSessionName(pName);
  }

  /**
   * @param pName
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletRequest#resolveSessionName(java.lang.String)
   */
  public Object resolveSessionName(String pName) {
    return mRequest.resolveSessionName(pName);
  }

  /**
   * @param pName
   * @param pReq
   * @param pRes
   * @return
   * @throws ServletException
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletRequest#serviceLocalParameter(atg.nucleus.naming.ParameterName, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
   */
  public boolean serviceLocalParameter(ParameterName pName,
      ServletRequest pReq, ServletResponse pRes) throws ServletException,
      IOException {
    return mRequest.serviceLocalParameter(pName, pReq, pRes);
  }

  /**
   * @param pName
   * @param pReq
   * @param pRes
   * @return
   * @throws ServletException
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletRequest#serviceLocalParameter(java.lang.String, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
   */
  public boolean serviceLocalParameter(String pName, ServletRequest pReq,
      ServletResponse pRes) throws ServletException, IOException {
    return mRequest.serviceLocalParameter(pName, pReq, pRes);
  }

  /**
   * @param pName
   * @param pReq
   * @param pRes
   * @param pCvt
   * @param pCvtArgs
   * @return
   * @throws ServletException
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletRequest#serviceParameter(atg.nucleus.naming.ParameterName, javax.servlet.ServletRequest, javax.servlet.ServletResponse, atg.droplet.TagConverter, java.util.Properties)
   */
  public boolean serviceParameter(ParameterName pName, ServletRequest pReq,
      ServletResponse pRes, TagConverter pCvt, Properties pCvtArgs)
      throws ServletException, IOException {
    return mRequest.serviceParameter(pName, pReq, pRes, pCvt, pCvtArgs);
  }

  /**
   * @param pName
   * @param pReq
   * @param pRes
   * @return
   * @throws ServletException
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletRequest#serviceParameter(atg.nucleus.naming.ParameterName, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
   */
  public boolean serviceParameter(ParameterName pName, ServletRequest pReq,
      ServletResponse pRes) throws ServletException, IOException {
    return mRequest.serviceParameter(pName, pReq, pRes);
  }

  /**
   * @param pName
   * @param pReq
   * @param pRes
   * @param pCvt
   * @param pCvtArgs
   * @return
   * @throws ServletException
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletRequest#serviceParameter(java.lang.String, javax.servlet.ServletRequest, javax.servlet.ServletResponse, atg.droplet.TagConverter, java.util.Properties)
   */
  public boolean serviceParameter(String pName, ServletRequest pReq,
      ServletResponse pRes, TagConverter pCvt, Properties pCvtArgs)
      throws ServletException, IOException {
    return mRequest.serviceParameter(pName, pReq, pRes, pCvt, pCvtArgs);
  }

  /**
   * @param pName
   * @param pReq
   * @param pRes
   * @return
   * @throws ServletException
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletRequest#serviceParameter(java.lang.String, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
   */
  public boolean serviceParameter(String pName, ServletRequest pReq,
      ServletResponse pRes) throws ServletException, IOException {
    return mRequest.serviceParameter(pName, pReq, pRes);
  }

  /**
   * @param pAfterGetsClaimed
   * @see atg.servlet.DynamoHttpServletRequest#setAfterGetsClaimed(boolean)
   */
  public void setAfterGetsClaimed(boolean pAfterGetsClaimed) {
    mRequest.setAfterGetsClaimed(pAfterGetsClaimed);
  }

  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.MutableHttpServletRequest#setAttribute(java.lang.String, java.lang.Object)
   */
  public void setAttribute(String pName, Object pValue) {
    mRequest.setAttribute(pName, pValue);
  }

  /**
   * @param pName
   * @param pFactory
   * @see atg.servlet.MutableHttpServletRequest#setAttributeFactory(java.lang.String, atg.servlet.AttributeFactory)
   */
  public void setAttributeFactory(String pName, AttributeFactory pFactory) {
    mRequest.setAttributeFactory(pName, pFactory);
  }

  /**
   * @param pAuthType
   * @see atg.servlet.MutableHttpServletRequest#setAuthType(java.lang.String)
   */
  public void setAuthType(String pAuthType) {
    mRequest.setAuthType(pAuthType);
  }

  /**
   * @param pBaseDir
   * @see atg.servlet.DynamoHttpServletRequest#setBaseDirectory(java.lang.String)
   */
  public void setBaseDirectory(String pBaseDir) {
    mRequest.setBaseDirectory(pBaseDir);
  }

  /**
   * @param pBrowserTyper
   * @see atg.servlet.DynamoHttpServletRequest#setBrowserTyper(atg.servlet.BrowserTyper)
   */
  public void setBrowserTyper(BrowserTyper pBrowserTyper) {
    mRequest.setBrowserTyper(pBrowserTyper);
  }

  /**
   * @param pEncoding
   * @throws UnsupportedEncodingException
   * @see atg.servlet.MutableHttpServletRequest#setCharacterEncoding(java.lang.String)
   */
  public void setCharacterEncoding(String pEncoding)
      throws UnsupportedEncodingException {
    mRequest.setCharacterEncoding(pEncoding);
  }

  /**
   * @param pContentLength
   * @see atg.servlet.MutableHttpServletRequest#setContentLength(int)
   */
  public void setContentLength(int pContentLength) {
    mRequest.setContentLength(pContentLength);
  }

  /**
   * @param pContentType
   * @see atg.servlet.MutableHttpServletRequest#setContentType(java.lang.String)
   */
  public void setContentType(String pContentType) {
    mRequest.setContentType(pContentType);
  }

  /**
   * @param pContextPath
   * @see atg.servlet.MutableHttpServletRequest#setContextPath(java.lang.String)
   */
  public void setContextPath(String pContextPath) {
    mRequest.setContextPath(pContextPath);
  }

  /**
   * @param pDisableExitTracking
   * @see atg.servlet.DynamoHttpServletRequest#setDisableExitTracking(boolean)
   */
  public void setDisableExitTracking(boolean pDisableExitTracking) {
    mRequest.setDisableExitTracking(pDisableExitTracking);
  }

  /**
   * @param pDocRootServicePrefix
   * @see atg.servlet.DynamoHttpServletRequest#setDocRootServicePrefix(java.lang.String)
   */
  public void setDocRootServicePrefix(String pDocRootServicePrefix) {
    mRequest.setDocRootServicePrefix(pDocRootServicePrefix);
  }

  /**
   * @param pDynamoPipeline
   * @see atg.servlet.DynamoHttpServletRequest#setDynamoPipeline(boolean)
   */
  public void setDynamoPipeline(boolean pDynamoPipeline) {
    mRequest.setDynamoPipeline(pDynamoPipeline);
  }

  /**
   * @param pEncodeMode
   * @see atg.servlet.DynamoHttpServletRequest#setEncodeContextPathMode(int)
   */
  public void setEncodeContextPathMode(int pEncodeMode) {
    mRequest.setEncodeContextPathMode(pEncodeMode);
  }

  /**
   * @param pEncode
   * @see atg.servlet.DynamoHttpServletRequest#setEncodeServletPath(boolean)
   */
  public void setEncodeServletPath(boolean pEncode) {
    mRequest.setEncodeServletPath(pEncode);
  }

  /**
   * @param pEncodeURL
   * @see atg.servlet.DynamoHttpServletRequest#setEncodeURL(boolean)
   */
  public void setEncodeURL(boolean pEncodeURL) {
    mRequest.setEncodeURL(pEncodeURL);
  }

  /**
   * @param pEventFlags
   * @see atg.servlet.DynamoHttpServletRequest#setEventFlags(int)
   */
  public void setEventFlags(int pEventFlags) {
    mRequest.setEventFlags(pEventFlags);
  }

  /**
   * @param pExitTrackingHandler
   * @see atg.servlet.DynamoHttpServletRequest#setExitTrackingHandler(atg.servlet.exittracking.ExitTrackingHandler)
   */
  public void setExitTrackingHandler(ExitTrackingHandler pExitTrackingHandler) {
    mRequest.setExitTrackingHandler(pExitTrackingHandler);
  }

  /**
   * @param pFormEventsSent
   * @see atg.servlet.DynamoHttpServletRequest#setFormEventsSent(boolean)
   */
  public void setFormEventsSent(boolean pFormEventsSent) {
    mRequest.setFormEventsSent(pFormEventsSent);
  }

  /**
   * @param pValue
   * @see atg.servlet.DynamoHttpServletRequest#setGenerateRequestLocales(boolean)
   */
  public void setGenerateRequestLocales(boolean pValue) {
    mRequest.setGenerateRequestLocales(pValue);
  }

  /**
   * @param pInitialized
   * @see atg.servlet.DynamoHttpServletRequest#setInitialized(boolean)
   */
  public void setInitialized(boolean pInitialized) {
    mRequest.setInitialized(pInitialized);
  }

  /**
   * @param pInputStream
   * @see atg.servlet.MutableHttpServletRequest#setInputStream(javax.servlet.ServletInputStream)
   */
  public void setInputStream(ServletInputStream pInputStream) {
    mRequest.setInputStream(pInputStream);
  }

  /**
   * @param pInTemplatePage
   * @see atg.servlet.DynamoHttpServletRequest#setInTemplatePage(boolean)
   */
  public void setInTemplatePage(boolean pInTemplatePage) {
    mRequest.setInTemplatePage(pInTemplatePage);
  }

  /**
   * @param pLinkEncoding
   * @see atg.servlet.DynamoHttpServletRequest#setLinkEncoding(java.lang.String)
   */
  public void setLinkEncoding(String pLinkEncoding) {
    mRequest.setLinkEncoding(pLinkEncoding);
  }

  /**
   * @param pLog
   * @see atg.servlet.DynamoHttpServletRequest#setLog(atg.nucleus.logging.ApplicationLogging)
   */
  public void setLog(ApplicationLogging pLog) {
    mRequest.setLog(pLog);
  }

  /**
   * @param pLogging
   * @see atg.servlet.DynamoHttpServletRequest#setLoggingDebug(boolean)
   */
  public void setLoggingDebug(boolean pLogging) {
    mRequest.setLoggingDebug(pLogging);
  }

  /**
   * @param pLogging
   * @see atg.servlet.DynamoHttpServletRequest#setLoggingError(boolean)
   */
  public void setLoggingError(boolean pLogging) {
    mRequest.setLoggingError(pLogging);
  }

  /**
   * @param pLogging
   * @see atg.servlet.DynamoHttpServletRequest#setLoggingInfo(boolean)
   */
  public void setLoggingInfo(boolean pLogging) {
    mRequest.setLoggingInfo(pLogging);
  }

  /**
   * @param pLogging
   * @see atg.servlet.DynamoHttpServletRequest#setLoggingWarning(boolean)
   */
  public void setLoggingWarning(boolean pLogging) {
    mRequest.setLoggingWarning(pLogging);
  }

  /**
   * @param pMethod
   * @see atg.servlet.MutableHttpServletRequest#setMethod(java.lang.String)
   */
  public void setMethod(String pMethod) {
    mRequest.setMethod(pMethod);
  }

  /**
   * @param pMimeType
   * @see atg.servlet.DynamoHttpServletRequest#setMimeType(java.lang.String)
   */
  public void setMimeType(String pMimeType) {
    mRequest.setMimeType(pMimeType);
  }

  /**
   * @param pMimeTyper
   * @see atg.servlet.DynamoHttpServletRequest#setMimeTyper(atg.servlet.MimeTyper)
   */
  public void setMimeTyper(MimeTyper pMimeTyper) {
    mRequest.setMimeTyper(pMimeTyper);
  }

  /**
   * @param pNucleus
   * @see atg.servlet.DynamoHttpServletRequest#setNucleus(atg.nucleus.Nucleus)
   */
  public void setNucleus(Nucleus pNucleus) {
    mRequest.setNucleus(pNucleus);
  }

  /**
   * @param pName
   * @param pValue
   * @param pCvt
   * @param pCvtArgs
   * @throws ServletException
   * @see atg.servlet.DynamoHttpServletRequest#setParameter(java.lang.String, java.lang.Object, atg.droplet.TagConverter, java.util.Properties)
   */
  public void setParameter(String pName, Object pValue, TagConverter pCvt,
      Properties pCvtArgs) throws ServletException {
    mRequest.setParameter(pName, pValue, pCvt, pCvtArgs);
  }

  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletRequest#setParameter(java.lang.String, java.lang.Object)
   */
  public void setParameter(String pName, Object pValue) {
    mRequest.setParameter(pName, pValue);
  }

  /**
   * @param pParameterDelimiter
   * @see atg.servlet.DynamoHttpServletRequest#setParameterDelimiter(java.lang.String)
   */
  public void setParameterDelimiter(String pParameterDelimiter) {
    mRequest.setParameterDelimiter(pParameterDelimiter);
  }

  /**
   * @param pParameterHandler
   * @see atg.servlet.DynamoHttpServletRequest#setParameterHandler(atg.servlet.ParameterHandler)
   */
  public void setParameterHandler(ParameterHandler pParameterHandler) {
    mRequest.setParameterHandler(pParameterHandler);
  }

  /**
   * @param pPathInfo
   * @see atg.servlet.MutableHttpServletRequest#setPathInfo(java.lang.String)
   */
  public void setPathInfo(String pPathInfo) {
    mRequest.setPathInfo(pPathInfo);
  }

  /**
   * @param pPathTranslated
   * @see atg.servlet.MutableHttpServletRequest#setPathTranslated(java.lang.String)
   */
  public void setPathTranslated(String pPathTranslated) {
    mRequest.setPathTranslated(pPathTranslated);
  }

  /**
   * @param pProtocol
   * @see atg.servlet.MutableHttpServletRequest#setProtocol(java.lang.String)
   */
  public void setProtocol(String pProtocol) {
    mRequest.setProtocol(pProtocol);
  }

  /**
   * @param pQueryString
   * @see atg.servlet.MutableHttpServletRequest#setQueryString(java.lang.String)
   */
  public void setQueryString(String pQueryString) {
    mRequest.setQueryString(pQueryString);
  }

  /**
   * @param pRemoteAddr
   * @see atg.servlet.MutableHttpServletRequest#setRemoteAddr(java.lang.String)
   */
  public void setRemoteAddr(String pRemoteAddr) {
    mRequest.setRemoteAddr(pRemoteAddr);
  }

  /**
   * @param pRemoteHost
   * @see atg.servlet.MutableHttpServletRequest#setRemoteHost(java.lang.String)
   */
  public void setRemoteHost(String pRemoteHost) {
    mRequest.setRemoteHost(pRemoteHost);
  }

  /**
   * @param pRemoteUser
   * @see atg.servlet.MutableHttpServletRequest#setRemoteUser(java.lang.String)
   */
  public void setRemoteUser(String pRemoteUser) {
    mRequest.setRemoteUser(pRemoteUser);
  }

  /**
   * @param pRequest
   * @see atg.servlet.DynamoHttpServletRequest#setRequest(javax.servlet.http.HttpServletRequest)
   */
  public void setRequest(HttpServletRequest pRequest) {
    mRequest.setRequest(pRequest);
  }

  /**
   * @param pLocale
   * @see atg.servlet.DynamoHttpServletRequest#setRequestLocale(atg.servlet.RequestLocale)
   */
  public void setRequestLocale(RequestLocale pLocale) {
    mRequest.setRequestLocale(pLocale);
  }

  /**
   * @param pValue
   * @see atg.servlet.DynamoHttpServletRequest#setRequestLocalePath(atg.nucleus.naming.ComponentName)
   */
  public void setRequestLocalePath(ComponentName pValue) {
    mRequest.setRequestLocalePath(pValue);
  }

  /**
   * @param pRequestScope
   * @see atg.servlet.DynamoHttpServletRequest#setRequestScope(atg.naming.NameContext)
   */
  public void setRequestScope(NameContext pRequestScope) {
    mRequest.setRequestScope(pRequestScope);
  }

  /**
   * @param pRequestScopeManager
   * @see atg.servlet.DynamoHttpServletRequest#setRequestScopeManager(atg.nucleus.RequestScopeManager)
   */
  public void setRequestScopeManager(RequestScopeManager pRequestScopeManager) {
    mRequest.setRequestScopeManager(pRequestScopeManager);
  }

  /**
   * @param pRequestURI
   * @see atg.servlet.MutableHttpServletRequest#setRequestURI(java.lang.String)
   */
  public void setRequestURI(String pRequestURI) {
    mRequest.setRequestURI(pRequestURI);
  }

  /**
   * @param pRequestURIHasQueryString
   * @see atg.servlet.DynamoHttpServletRequest#setRequestURIHasQueryString(boolean)
   */
  public void setRequestURIHasQueryString(boolean pRequestURIHasQueryString) {
    mRequest.setRequestURIHasQueryString(pRequestURIHasQueryString);
  }

  /**
   * @param pResponse
   * @see atg.servlet.DynamoHttpServletRequest#setResponse(atg.servlet.DynamoHttpServletResponse)
   */
  public void setResponse(DynamoHttpServletResponse pResponse) {
    mRequest.setResponse(pResponse);
  }

  /**
   * @param pRestorableSessionIdFromURL
   * @see atg.servlet.DynamoHttpServletRequest#setRestorableSessionIdFromURL(java.lang.String)
   */
  public void setRestorableSessionIdFromURL(String pRestorableSessionIdFromURL) {
    mRequest.setRestorableSessionIdFromURL(pRestorableSessionIdFromURL);
  }

  /**
   * @param pScheme
   * @see atg.servlet.MutableHttpServletRequest#setScheme(java.lang.String)
   */
  public void setScheme(String pScheme) {
    mRequest.setScheme(pScheme);
  }

  /**
   * @param pScrambleKey
   * @see atg.servlet.DynamoHttpServletRequest#setScrambleKey(byte[])
   */
  public void setScrambleKey(byte[] pScrambleKey) {
    mRequest.setScrambleKey(pScrambleKey);
  }

  /**
   * @param pServerName
   * @see atg.servlet.MutableHttpServletRequest#setServerName(java.lang.String)
   */
  public void setServerName(String pServerName) {
    mRequest.setServerName(pServerName);
  }

  /**
   * @param pServerPort
   * @see atg.servlet.MutableHttpServletRequest#setServerPort(int)
   */
  public void setServerPort(int pServerPort) {
    mRequest.setServerPort(pServerPort);
  }

  /**
   * @param pServletPath
   * @see atg.servlet.MutableHttpServletRequest#setServletPath(java.lang.String)
   */
  public void setServletPath(String pServletPath) {
    mRequest.setServletPath(pServletPath);
  }

  /**
   * @param pSessionRequest
   * @see atg.servlet.DynamoHttpServletRequest#setSessionRequest(atg.servlet.HttpSessionRequest)
   */
  public void setSessionRequest(HttpSessionRequest pSessionRequest) {
    mRequest.setSessionRequest(pSessionRequest);
  }

  /**
   * 
   * @see atg.servlet.DynamoHttpServletRequest#setupLoopbackTemplateEmailRequest()
   */
  public void setupLoopbackTemplateEmailRequest() {
    mRequest.setupLoopbackTemplateEmailRequest();
  }

  /**
   * @param pURLSessionIdSpecifier
   * @see atg.servlet.DynamoHttpServletRequest#setURLSessionIdSpecifier(java.lang.String)
   */
  public void setURLSessionIdSpecifier(String pURLSessionIdSpecifier) {
    mRequest.setURLSessionIdSpecifier(pURLSessionIdSpecifier);
  }

  /**
   * @param pUserAuthenticator
   * @see atg.servlet.DynamoHttpServletRequest#setUserAuthenticator(atg.security.UserAuthenticator)
   */
  public void setUserAuthenticator(UserAuthenticator pUserAuthenticator) {
    mRequest.setUserAuthenticator(pUserAuthenticator);
  }

  /**
   * @param pWebApplication
   * @see atg.servlet.DynamoHttpServletRequest#setWebApplication(atg.servlet.minimal.WebApplicationInterface)
   */
  public void setWebApplication(WebApplicationInterface pWebApplication) {
    mRequest.setWebApplication(pWebApplication);
  }

  /**
   * @param pWebPools
   * @see atg.servlet.DynamoHttpServletRequest#setWebPools(atg.servlet.WebPools)
   */
  public void setWebPools(WebPools pWebPools) {
    mRequest.setWebPools(pWebPools);
  }

  /**
   * @param pScopeManager
   * @see atg.servlet.DynamoHttpServletRequest#setWindowScopeManager(atg.nucleus.WindowScopeManager)
   */
  public void setWindowScopeManager(WindowScopeManager pScopeManager) {
    mRequest.setWindowScopeManager(pScopeManager);
  }

  /**
   * @param pWrapper
   * @see atg.servlet.DynamoHttpServletRequest#setWrapper(javax.servlet.ServletRequestWrapper)
   */
  public void setWrapper(ServletRequestWrapper pWrapper) {
    mRequest.setWrapper(pWrapper);
  }

  /**
   * @param pURL
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#shouldExitTrack(java.lang.String)
   */
  public boolean shouldExitTrack(String pURL) {
    return mRequest.shouldExitTrack(pURL);
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#tamperedURLParameters()
   */
  public boolean tamperedURLParameters() {
    return mRequest.tamperedURLParameters();
  }

  /**
   * @return
   * @see atg.servlet.DynamoHttpServletRequest#toString()
   */
  public String toString() {
    return mRequest.toString();
  }
}