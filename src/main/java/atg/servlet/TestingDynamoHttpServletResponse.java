package atg.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * A wrapper around DynamoHttpServletResponse to allow for adding test
 * specific methods
 */
public class TestingDynamoHttpServletResponse extends DynamoHttpServletResponse {
  
  private DynamoHttpServletResponse mResponse = null;
  public TestingDynamoHttpServletResponse(DynamoHttpServletResponse pResponse) {
    mResponse = pResponse;
  }
  
  // ------------------------------
  // DynamoHttpServletResponse delegate methods beyond this point
  //
  /**
   * @param pCookie
   * @see atg.servlet.DynamoHttpServletResponse#addCookie(javax.servlet.http.Cookie)
   */
  public void addCookie(Cookie pCookie) {
    mResponse.addCookie(pCookie);
  }
  /**
   * @param pCookie
   * @see atg.servlet.DynamoHttpServletResponse#addCookieToBuffer(javax.servlet.http.Cookie)
   */
  public void addCookieToBuffer(Cookie pCookie) {
    mResponse.addCookieToBuffer(pCookie);
  }
  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletResponse#addDateHeader(java.lang.String, long)
   */
  public void addDateHeader(String pName, long pValue) {
    mResponse.addDateHeader(pName, pValue);
  }
  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletResponse#addHeader(java.lang.String, java.lang.String)
   */
  public void addHeader(String pName, String pValue) {
    mResponse.addHeader(pName, pValue);
  }
  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletResponse#addIntHeader(java.lang.String, int)
   */
  public void addIntHeader(String pName, int pValue) {
    mResponse.addIntHeader(pName, pValue);
  }
  /**
   * @param pName
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#containsHeader(java.lang.String)
   */
  public boolean containsHeader(String pName) {
    return mResponse.containsHeader(pName);
  }
  /**
   * @param pUrl
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletResponse#encodeRedirectUrl(java.lang.String)
   */
  public String encodeRedirectUrl(String pUrl) {
    return mResponse.encodeRedirectUrl(pUrl);
  }
  /**
   * @param pUrl
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#encodeRedirectURL(java.lang.String)
   */
  public String encodeRedirectURL(String pUrl) {
    return mResponse.encodeRedirectURL(pUrl);
  }
  /**
   * @param pUrl
   * @return
   * @deprecated
   * @see atg.servlet.DynamoHttpServletResponse#encodeUrl(java.lang.String)
   */
  public String encodeUrl(String pUrl) {
    return mResponse.encodeUrl(pUrl);
  }
  /**
   * @param pUrl
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#encodeURL(java.lang.String)
   */
  public String encodeURL(String pUrl) {
    return mResponse.encodeURL(pUrl);
  }
  /**
   * @param pObj
   * @return
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object pObj) {
    return mResponse.equals(pObj);
  }
  /**
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#flushBuffer()
   */
  public void flushBuffer() throws IOException {
    mResponse.flushBuffer();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getBufferSize()
   */
  public int getBufferSize() {
    return mResponse.getBufferSize();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getCharacterEncoding()
   */
  public String getCharacterEncoding() {
    return mResponse.getCharacterEncoding();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getContentType()
   */
  public String getContentType() {
    return mResponse.getContentType();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getContentTypeSet()
   */
  public boolean getContentTypeSet() {
    return mResponse.getContentTypeSet();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getDoExitTracking()
   */
  public boolean getDoExitTracking() {
    return mResponse.getDoExitTracking();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getHeaders()
   */
  public Dictionary getHeaders() {
    return mResponse.getHeaders();
  }
  /**
   * @param pHeaderName
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getHeaders(java.lang.String)
   */
  public Enumeration getHeaders(String pHeaderName) {
    return mResponse.getHeaders(pHeaderName);
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getLocale()
   */
  public Locale getLocale() {
    return mResponse.getLocale();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getLocaleToCharsetMapper()
   */
  public LocaleToCharsetMapper getLocaleToCharsetMapper() {
    return mResponse.getLocaleToCharsetMapper();
  }
  /**
   * @return
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#getOutputStream()
   */
  public ByteArrayServletOutputStream getOutputStream() throws IOException {
    return (ByteArrayServletOutputStream) mResponse.getOutputStream();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getResponse()
   */
  public HttpServletResponse getResponse() {
    return mResponse.getResponse();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getStatus()
   */
  public int getStatus() {
    return mResponse.getStatus();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#getWrapper()
   */
  public ServletResponseWrapper getWrapper() {
    return mResponse.getWrapper();
  }
  /**
   * @return
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#getWriter()
   */
  public PrintWriter getWriter() throws IOException {
    return mResponse.getWriter();
  }
  /**
   * @return
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return mResponse.hashCode();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#isCommitted()
   */
  public boolean isCommitted() {
    return mResponse.isCommitted();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#isResponseComplete()
   */
  public boolean isResponseComplete() {
    return mResponse.isResponseComplete();
  }
  /**
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#isWriterUsed()
   */
  public boolean isWriterUsed() {
    return mResponse.isWriterUsed();
  }
  /**
   * 
   * @see atg.servlet.DynamoHttpServletResponse#reset()
   */
  public void reset() {
    mResponse.reset();
  }
  /**
   * 
   * @see atg.servlet.DynamoHttpServletResponse#resetBuffer()
   */
  public void resetBuffer() {
    mResponse.resetBuffer();
  }
  /**
   * @param pCode
   * @param pMessage
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#sendError(int, java.lang.String)
   */
  public void sendError(int pCode, String pMessage) throws IOException {
    mResponse.sendError(pCode, pMessage);
  }
  /**
   * @param pCode
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#sendError(int)
   */
  public void sendError(int pCode) throws IOException {
    mResponse.sendError(pCode);
  }
  /**
   * @param pLocation
   * @param pRequest
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#sendLocalRedirect(java.lang.String, atg.servlet.DynamoHttpServletRequest)
   */
  public void sendLocalRedirect(String pLocation,
      DynamoHttpServletRequest pRequest) throws IOException {
    mResponse.sendLocalRedirect(pLocation, pRequest);
  }
  /**
   * @param pLocation
   * @throws IOException
   * @see atg.servlet.DynamoHttpServletResponse#sendRedirect(java.lang.String)
   */
  public void sendRedirect(String pLocation) throws IOException {
    mResponse.sendRedirect(pLocation);
  }
  /**
   * @param pBufferSize
   * @see atg.servlet.DynamoHttpServletResponse#setBufferSize(int)
   */
  public void setBufferSize(int pBufferSize) {
    mResponse.setBufferSize(pBufferSize);
  }
  /**
   * @param pCharset
   * @see atg.servlet.DynamoHttpServletResponse#setCharacterEncoding(java.lang.String)
   */
  public void setCharacterEncoding(String pCharset) {
    mResponse.setCharacterEncoding(pCharset);
  }
  /**
   * @param pLength
   * @see atg.servlet.DynamoHttpServletResponse#setContentLength(int)
   */
  public void setContentLength(int pLength) {
    mResponse.setContentLength(pLength);
  }
  /**
   * @param pContentType
   * @see atg.servlet.DynamoHttpServletResponse#setContentType(java.lang.String)
   */
  public void setContentType(String pContentType) {
    mResponse.setContentType(pContentType);
  }
  /**
   * @param pContentTypeSet
   * @see atg.servlet.DynamoHttpServletResponse#setContentTypeSet(boolean)
   */
  public void setContentTypeSet(boolean pContentTypeSet) {
    mResponse.setContentTypeSet(pContentTypeSet);
  }
  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletResponse#setDateHeader(java.lang.String, long)
   */
  public void setDateHeader(String pName, long pValue) {
    mResponse.setDateHeader(pName, pValue);
  }
  /**
   * @param pDoExitTracking
   * @see atg.servlet.DynamoHttpServletResponse#setDoExitTracking(boolean)
   */
  public void setDoExitTracking(boolean pDoExitTracking) {
    mResponse.setDoExitTracking(pDoExitTracking);
  }
  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletResponse#setHeader(java.lang.String, java.lang.String)
   */
  public void setHeader(String pName, String pValue) {
    mResponse.setHeader(pName, pValue);
  }
  /**
   * @param pName
   * @param pValue
   * @see atg.servlet.DynamoHttpServletResponse#setIntHeader(java.lang.String, int)
   */
  public void setIntHeader(String pName, int pValue) {
    mResponse.setIntHeader(pName, pValue);
  }
  /**
   * @param pLocale
   * @see atg.servlet.DynamoHttpServletResponse#setLocale(java.util.Locale)
   */
  public void setLocale(Locale pLocale) {
    mResponse.setLocale(pLocale);
  }
  /**
   * @param pMapper
   * @see atg.servlet.DynamoHttpServletResponse#setLocaleToCharsetMapper(atg.servlet.LocaleToCharsetMapper)
   */
  public void setLocaleToCharsetMapper(LocaleToCharsetMapper pMapper) {
    mResponse.setLocaleToCharsetMapper(pMapper);
  }
  /**
   * @param pOutputStream
   * @see atg.servlet.DynamoHttpServletResponse#setOutputStream(javax.servlet.ServletOutputStream)
   */
  public void setOutputStream(ServletOutputStream pOutputStream) {
    mResponse.setOutputStream(pOutputStream);
  }
  /**
   * @param pRequest
   * @see atg.servlet.DynamoHttpServletResponse#setRequest(atg.servlet.DynamoHttpServletRequest)
   */
  public void setRequest(DynamoHttpServletRequest pRequest) {
    mResponse.setRequest(pRequest);
  }
  /**
   * @param pResponse
   * @see atg.servlet.DynamoHttpServletResponse#setResponse(javax.servlet.http.HttpServletResponse)
   */
  public void setResponse(HttpServletResponse pResponse) {
    mResponse.setResponse(pResponse);
  }
  /**
   * @param pCode
   * @param pMessage
   * @deprecated
   * @see atg.servlet.DynamoHttpServletResponse#setStatus(int, java.lang.String)
   */
  public void setStatus(int pCode, String pMessage) {
    mResponse.setStatus(pCode, pMessage);
  }
  /**
   * @param pCode
   * @see atg.servlet.DynamoHttpServletResponse#setStatus(int)
   */
  public void setStatus(int pCode) {
    mResponse.setStatus(pCode);
  }
  /**
   * @param pStrict
   * @return
   * @see atg.servlet.DynamoHttpServletResponse#setStrictOutputAccess(boolean)
   */
  public boolean setStrictOutputAccess(boolean pStrict) {
    return mResponse.setStrictOutputAccess(pStrict);
  }
  /**
   * @param pWrapper
   * @see atg.servlet.DynamoHttpServletResponse#setWrapper(javax.servlet.ServletResponseWrapper)
   */
  public void setWrapper(ServletResponseWrapper pWrapper) {
    mResponse.setWrapper(pWrapper);
  }
  /**
   * @param pWriter
   * @see atg.servlet.DynamoHttpServletResponse#setWriter(java.io.PrintWriter)
   */
  public void setWriter(PrintWriter pWriter) {
    mResponse.setWriter(pWriter);
  }
  /**
   * @return
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return mResponse.toString();
  }
}