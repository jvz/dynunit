package com.mycompany;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;

import atg.nucleus.naming.ParameterName;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.DynamoServlet;

public class SimpleDroplet extends DynamoServlet {
  /**
   * 
   */
  public static final String USERNAME = "username";
  String mUsername = null;
  String mUsernameFromInputStream = null;

  /**
   * Called to execute this droplet
   */
  @Override
  public void service(final DynamoHttpServletRequest request,
      final DynamoHttpServletResponse response) throws ServletException,
      IOException {
    logInfo("Starting: " + this.getClass().getName());
    request.serviceLocalParameter(ParameterName.getParameterName("output"), request, response);
    request.setParameter("entry", "The Value");
    response.getOutputStream().write("Some content from the simple droplet".getBytes());
    mUsername = request.getParameter(USERNAME);
    // try to read data from the client if it is available
    if ("POST".equals(request.getMethod())) {
      ServletInputStream s = request.getInputStream();
      Properties p = new Properties();
      p.load(s);
      mUsernameFromInputStream = p.getProperty(USERNAME);
    }
  }

  /**
   * Returns the value of the username parameter
   * 
   * @return
   */
  public String getUsername() {
    return mUsername;
  }

  /**
   * Returns the value of the username as written to the request input stream
   * 
   * @return
   */
  public String getUsernameFromInputStream() {
    return mUsernameFromInputStream;
  }
}
