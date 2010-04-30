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
public class TestingDynamoHttpServletResponse extends atg.servlet.ServletTestUtils.TestingDynamoHttpServletResponse {
     public TestingDynamoHttpServletResponse
      (DynamoHttpServletResponse pResponse) 
    {
      super(pResponse);
    } 
}
