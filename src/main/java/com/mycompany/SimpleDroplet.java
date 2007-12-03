package com.mycompany;

import java.io.IOException;

import javax.servlet.ServletException;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.DynamoServlet;

public class SimpleDroplet extends DynamoServlet {

  @Override
  public void service(final DynamoHttpServletRequest request,
      final DynamoHttpServletResponse response) throws ServletException,
      IOException {
    logInfo("Starting: " + this.getClass().getName());
    request.serviceParameter("some value", request, response);
  }
}
