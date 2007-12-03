package atg.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import atg.nucleus.naming.ParameterName;

public class MockDynamoHttpServletRequest extends DynamoHttpServletRequest {

  private final Map<String, String> cookieParameters = new HashMap<String, String>(),
      headers = new HashMap<String, String>();

  private final Map<String, Object> parameters = new HashMap<String, Object>(),
      components = new HashMap<String, Object>(),
      attributes = new HashMap<String, Object>();

  private final List<String> serviceParameters = new ArrayList<String>(),
      servicedLocalParameter = new ArrayList<String>();;

  public MockDynamoHttpServletRequest() {
    super();
  }

  public String encodeURL(String str) {
    return str + ";sessionId=" + System.currentTimeMillis();
  }

  public Object getAttribute(String attribute) {
    return attributes.get(attribute);
  }

  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames() {

    // Cheesy way converting an iterator to an enumeration
    final Properties properties = new Properties();
    for (final Iterator<String> it = attributes.keySet().iterator(); it
        .hasNext();) {
      final String s = it.next();
      properties.put(s, s);
    }
    return properties.elements();
  }

  @Override
  public String getCookieParameter(String parameter) {
    return cookieParameters.get(parameter);
  }

  public String getHeader(String param) {
    return (String) headers.get(param);
  }

  public Object getLocalParameter(String name) {
    return parameters.get(name);
  }

  public Object getObjectParameter(ParameterName name) {
    return getObjectParameter(name.getName());
  }

  public Object getObjectParameter(String name) {
    return parameters.get(name);
  }

  public String getParameter(String name) {
    return (String) parameters.get(name);
  }

  public List<String> getServiceParameters() {
    return serviceParameters;
  }

  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  public boolean serviceLocalParameter(String name, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    servicedLocalParameter.add(name);
    return true;
  }

  public boolean serviceParameter(String name, ServletRequest request,
      ServletResponse response) throws ServletException, IOException {
    serviceParameters.add(name);
    return serviceParameter(name, request, response, null, null);
  }

  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  public void setCookieParameter(String name, String value) {
    cookieParameters.put(name, value);
  }

  public void setHeaders(String name, String value) {
    headers.put(name, value);
  }

  public void setInputParameter(ParameterName name, Object value) {
    setInputParameter(name.getName(), value);
  }

  public void setInputParameter(String name, Object value) {
    parameters.put(name, value);
  }

  public void setNamedParameter(String name, Object value) {
    components.put(name, value);
  }

  public void setParameter(String key, Object value) {
    parameters.put(key, value);
  }

}
