/**
 * Copyright 2010 ATG DUST Project Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package atg.droplet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import atg.nucleus.Nucleus;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletTestUtils;
import atg.servlet.ServletUtil;
import atg.servlet.ServletTestUtils.ServiceParameterCallback;
import atg.servlet.ServletTestUtils.TestingDynamoHttpServletRequest;
import atg.servlet.ServletTestUtils.TestingDynamoHttpServletResponse;

/**
 * A utility class for invoking droplets and capturing the results. This class
 * provides invokeDroplet methods that return a DropletResult, which captures
 * which OPARAMs were rendered and what the stack frame contained at that time.
 * <P>
 * If the forms of invokeDroplet() which have no request and response parameters
 * are invoked, then the DropletInvoker instance's request and response
 * properties will be used (auto-creating a request and response if needed).
 * <P>
 * The request and response wil be re-used on later invokeDroplet() invocations
 * with explicit request and response parameters. Use resetRequestResponse() to
 * clear the existing request and response.
 * <P>
 * DropletInvoker provides protected methods as extension points. For example,
 * if you need to save additional information in a RenderedOutputParameter, you
 * can override createRenderedOutputParameter to create a sub-class.
 * <P>
 * Note that you can also set your own Servlets on the request, and they will be
 * invoked as normal OPARAMETERs. This does not prevent RenderedOutputParameters
 * from being created for that invocation unless an exception is thrown.
 * <P>
 * You can also set oparamsExistByDefault to control whether serviceParameter
 * and serviceLocalParameter return true even if no Servlet is found for the
 * specified OPARM name. Additional, you can use setOparamExistsOverride() to
 * control what the request returns for rendering an OPARAM with the given name.
 * <P>
 * Created: October 19 2009
 * 
 * @author Charles Morehead
 * @version $Id:
 *          //test/UnitTests/base/main/src/Java/atg/droplet/DropletInvoker.java
 *          #5 $
 **/

public class DropletInvoker {

  // -------------------------------------
  // Class version string
  /** Class version string from source code control system. */
  public static final String CLASS_VERSION = "$Id: //test/UnitTests/base/main/src/Java/atg/droplet/DropletInvoker.java#5 $";

  // -------------------------------------
  // Constants

  // -------------------------------------
  // Member variables

  Map<String, Boolean> mOparamExistsOverrideMap = new HashMap<String, Boolean>();
  ServletTestUtils mServletTestUtils;
  TestingDynamoHttpServletRequest mRequest;
  TestingDynamoHttpServletResponse mResponse;
  String mSessionId;
  Nucleus mNucleus;

  // -------------------------------------
  // Properties

  // -------------------------------------
  // property: servletTestUtils

  /**
   * Return out instance of servletTestUtils. Invokes createServletTestUtils to
   * create one if none exists.
   * 
   * @return the servletTestUtils instance used to create requests/responses.
   */
  public ServletTestUtils getServletTestUtils() {
    if (mServletTestUtils == null) {
      mServletTestUtils = createServletTestUtils();
    }
    return mServletTestUtils;
  }

  // ---------------------------------------------------------------------------
  // readonly property: nucleus

  /**
   * Return the nucleus property.
   * 
   * @return our Nucleus instance
   */
  public Nucleus getNucleus() {
    return mNucleus;
  }

  // -------------------------------------
  // property: request

  /**
   * Return the TestingDynamoHttpServletRequest, creating one, if needed. This
   * is the response used by invokeDroplet() if no existing
   * TestingDynamoHttpServletRequest is given.
   * 
   * @return the request used for invokeDroplet if no explicit request and
   *         response are given.
   */
  public TestingDynamoHttpServletRequest getRequest() {
    if (mRequest == null) {
      mRequest = createRequest();
    }
    return mRequest;
  }

  /**
   * Return the TestingDynamoHttpServletRequest, creating one, if needed. This
   * is the response used by invokeDroplet() if no existing request and response
   * are given.
   * 
   * @return the response used for invokeDroplet if no explicit request and
   *         response are given.
   */
  public TestingDynamoHttpServletResponse getResponse() {
    if (mResponse == null) {
      mResponse = (TestingDynamoHttpServletResponse) (getRequest()
          .getResponse());
    }
    return mResponse;
  }

  /** Reset the request and response. */
  public TestingDynamoHttpServletRequest resetRequestResponse() {
    mRequest = null;
    mResponse = null;
    return getRequest();
  }

  // -------------------------------------
  // property: sessionId

  /**
   * Sets the session ID property. The session ID will be used for created our
   * request property, if a request is needed..
   * 
   * @param pSessionId
   *          the new value for session ID
   */
  public void setSessionId(String pSessionId) {
    mSessionId = pSessionId;
  }

  /**
   * Returns the session ID property. The session ID will be used for created
   * our request property, if a request is needed.
   * 
   * @return the session ID used for creating our request property's request.
   */
  public String getSessionId() {
    return mSessionId;
  }

  // -------------------------------------
  // property: oparamsExistByDefault

  private boolean mOparamsExistByDefault = true;

  /**
   * Sets whether OPARAMs exist by default.
   * 
   * @param pOparamsExistByDefault
   *          true if serviceParameter() and serviceLocalParameter() will return
   *          true by default, even if no parameter by that name was found.
   */
  public void setOparamsExistByDefault(boolean pOparamsExistByDefault) {
    mOparamsExistByDefault = pOparamsExistByDefault;
  }

  /**
   * Returns property oparamsExistByDefault
   * 
   * @return true if serviceParameter() and serviceLocalParameter() will return
   *         true by default, even if no parameter by that name was found.
   */
  public boolean getOparamsExistByDefault() {
    return mOparamsExistByDefault;
  }

  // -------------------------------------
  // oparamExistsOverrides

  /** Add an override for whether the given parameter exists. */
  public void clearOparamExistsOverrides() {
    mOparamExistsOverrideMap.clear();
  }

  /**
   * Add an override for whether the given parameter exists.
   * 
   * @param pName
   *          the name of the OPARAM to say exists (or doesn't).
   * @param pValue
   *          TRUE means say that the specified OPARAM exists, FALSE means say
   *          the the OPARAM does not exist.
   */
  public void setOparamExistsOverride(String pName, Boolean pValue) {
    if (pValue == null) {
      mOparamExistsOverrideMap.remove(pName);
    } else {
      mOparamExistsOverrideMap.put(pName, pValue);
    }
  }

  // -------------------------------------------------------

  /** Create a new DropletInvoker that will use the specified Nucleus. */
  public DropletInvoker(Nucleus pNucleus) {
    mNucleus = pNucleus;
  }

  /**
   * Invoke the specified droplet, using the the DropletInvoker instance's
   * request and response.
   * 
   * @param pDropletName
   *          the Nucleus name of the droplet to invoke.
   * @param pAdditionalParameters
   *          additional parameters to set on the request. These parameters are
   *          set inside the droplet-specific parameter stack frame.
   * @return a droplet result object representing the OPARAMs rendered.
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an error occurs
   */
  public DropletResult invokeDroplet(String pDropletName,
      Map<String, Object> pAdditionalParameters) throws ServletException,
      IOException {
    return invokeDroplet(pDropletName, pAdditionalParameters, getRequest(),
        getResponse());
  }

  /**
   * Invoke the specified droplet, using the the DropletInvoker instance's
   * request and response.
   * 
   * @param pDropletName
   *          the Nucleus name of the droplet to invoke.
   * @return a droplet result object representing the OPARAMs rendered.
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an error occurs
   */
  public DropletResult invokeDroplet(String pDropletName)
      throws ServletException, IOException {
    return invokeDroplet(pDropletName, null, getRequest(), getResponse());
  }

  /**
   * Invoke the specified droplet, using the specified request and response.
   * Note that the specified request and response may be the invoker's request
   * and response, or may be externally provided.
   * 
   * @param pDropletName
   *          the Nucleus name of the droplet to invoke.
   * @param pAdditionalParameters
   *          additional parameters to set on the request. These parameters are
   *          set inside the droplet-specific parameter stack frame.
   * @param pRequest
   *          the request to use for droplet invocation.
   * @param pResponse
   *          the response to use for droplet invocation.
   * @return a droplet result object representing the OPARAMs rendered.
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an error occurs
   */
  public DropletResult invokeDroplet(String pDropletName,
      Map<String, Object> pAdditionalParameters,
      TestingDynamoHttpServletRequest pRequest,
      TestingDynamoHttpServletResponse pResponse) throws ServletException,
      IOException {
    DynamoHttpServletRequest requestRestore = ServletUtil
        .setCurrentRequest(pRequest);
    try {
      Servlet droplet = (Servlet) getRequest().resolveName(pDropletName);
      if (droplet == null) {
        throw new IllegalArgumentException("Droplet " + pDropletName
            + " not found.");
      }
      return invokeDroplet(droplet, pAdditionalParameters, pRequest, pResponse);

    } finally {
      ServletUtil.setCurrentRequest(requestRestore);
    }
  }

  /**
   * Invoke the specified droplet, using the specified request and response.
   * Note that the specified request and response may be the invoker's request
   * and response, or may be externally provided.
   * 
   * @param pDropletName
   *          the Nucleus name of the droplet to invoke.
   * @param pRequest
   *          the request to use for droplet invocation.
   * @param pResponse
   *          the response to use for droplet invocation.
   * @return a droplet result object representing the OPARAMs rendered.
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an error occurs
   */
  public DropletResult invokeDroplet(String pDropletName,
      TestingDynamoHttpServletRequest pRequest,
      TestingDynamoHttpServletResponse pResponse) throws ServletException,
      IOException {
    return invokeDroplet(pDropletName, null, pRequest, pResponse);
  }

  /**
   * Invoke the specified droplet, using the specified request and response.
   * Note that the specified request and response may be the invoker's request
   * and response, or may be externally provided.
   * 
   * @param pDroplet
   *          the droplet to invoke.
   * @param pAdditionalParameters
   *          additional parameters to set on the request. These parameters are
   *          set inside the droplet-specific parameter stack frame.
   * @param pRequest
   *          the request to use for droplet invocation.
   * @param pResponse
   *          the response to use for droplet invocation.
   * @return a droplet result object representing the OPARAMs rendered.
   * @exception ServletException
   *              if an error occurs
   * @exception IOException
   *              if an error occurs
   */
  public DropletResult invokeDroplet(Servlet pDroplet,
      Map<String, Object> pAdditionalParameters,
      TestingDynamoHttpServletRequest pRequest,
      TestingDynamoHttpServletResponse pResponse) throws ServletException,
      IOException {
    DropletResult result = createDropletResult(pDroplet, pRequest, pResponse);
    DynamoHttpServletRequest requestRestore = ServletUtil
        .setCurrentRequest(pRequest);
    ServiceParameterCallback callbackRestore = pRequest
        .setServiceParameterCallback(createServiceParameterCallback(result));
    try {
      pRequest.pushFrame();

      if (pAdditionalParameters != null) {
        for (Map.Entry<String, Object> entryCur : pAdditionalParameters
            .entrySet()) {
          pRequest.setParameter(entryCur.getKey(), entryCur.getValue());
        }
      }

      pDroplet.service(pRequest, pResponse);
    } finally {
      pRequest.setServiceParameterCallback(callbackRestore);
      pRequest.popFrame();
      ServletUtil.setCurrentRequest(requestRestore);
    }
    return result;
  }

  /**
   * Create a new DropletResult. This method exists so can be overridden is
   * subclasses, as needed.
   * 
   * @param pDroplet
   *          the droplet whose invocation our DropletResult will represent
   * @param pRequest
   *          the request used for the droplet invocation
   * @param pResponse
   *          the response used for the droplet invocation
   * @return the result representation.
   */
  protected DropletResult createDropletResult(Servlet pDroplet,
      DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse) {

    return new DropletResult(pDroplet, pRequest, pResponse);
  }

  /**
   * Create a new ServiceParameterCallback.
   * 
   * @param pResult
   *          the DropletResult whose rendered parameters the
   *          ServiceParameterCallback should add to.
   * @return a newly created ServiceParameterCallback
   */
  protected ServiceParameterCallback createServiceParameterCallback(
      DropletResult pResult) {
    return new ServiceParameterCallbackImpl(pResult);
  }

  /**
   * Return newly created ServletTestUtils. Provided so that sub-classes can
   * override.
   * 
   * @return a newly created ServletTestUtils.
   */
  protected ServletTestUtils createServletTestUtils() {
    return new ServletTestUtils();
  }

  /**
   * Return newly created TestingDynamoHttpServletResponse. This method is used
   * to create a request for our request property to be used if the form of
   * invokeDroplet sans request/resposne is invoked.
   * 
   * @return a new request
   */
  protected TestingDynamoHttpServletRequest createRequest() {
    TestingDynamoHttpServletRequest request = getServletTestUtils()
        .createDynamoHttpServletRequestForSession(getNucleus(),
            createValueParametersMapForNewRequest(), 1024, "GET",
            getSessionId());

    if (request.getResponse() instanceof TestingDynamoHttpServletResponse) {
      TestingDynamoHttpServletResponse testResponse = ((TestingDynamoHttpServletResponse) request
          .getResponse());
      testResponse.setBlockDispatches(true);
      testResponse.setRecordDispatches(true);
    }

    return request;
  }

  /**
   * Called to create a RenderedOutputParameter. Can be overridden to create a
   * RenderedOutputParameter subclass to record additional information.
   * 
   * @param pName
   *          the name of the OPARAM being request
   * @param pRequest
   *          the request used to render the OPARAM
   * @param pResponse
   *          the response used to render the OPARAM
   * @return a newly created RenderedOutputParameter (or subclass)
   */
  protected RenderedOutputParameter createRenderedOutputParameter(String pName,
      DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse) {
    return new RenderedOutputParameter(pName, pRequest, pResponse);
  }

  /**
   * Create a value parameters map for the new request. For the time being, just
   * returns an empty Map.
   * 
   * @return a Map for a new request.
   */
  protected Map<String, Object> createValueParametersMapForNewRequest() {
    Map<String, Object> mapResult = new HashMap<String, Object>();
    return mapResult;
  }

  // -------------------------------------
  /**
   * A representation of a rendered OParam. Takes a snapshot of of the
   * FrameParameters.
   */
  public static class RenderedOutputParameter {
    Map<String, Object> mFrameParameters;
    String mName;

    /**
     * Created a representation of a rendered OutputParameter.
     * 
     * @param pName
     *          the name out the OPARAM
     * @param pRequest
     *          the request at the time the OPARAM should be rendered.
     * @param pResponse
     *          the response at the time the OPARAM should be rendered.
     */
    public RenderedOutputParameter (String pName,
                                    DynamoHttpServletRequest pRequest,
                                    DynamoHttpServletResponse pResponse)  {
      mName = pName;
//      mFrameParameters = new HashMap(pRequest.getMapForCurrentFrame());
      mFrameParameters = new HashMap();

      // because of a bug with entrySet in 9.0, we go through keys
      // and copy manually, here.

      Map mapFrame = pRequest.getMapForCurrentFrame();

      Iterator iterKeys = mapFrame.keySet().iterator();
      while (iterKeys.hasNext()) {
        String strKey = (String)iterKeys.next();
        mFrameParameters.put(strKey, mapFrame.get(strKey));
      }
    }

    /**
     * Return the name of the rendered OPARAM
     * 
     * @return the name of OPARAM whose rendering this object represents.
     */
    public String getName() {
      return mName;
    }

    /**
     * Return the map of frame parameters.
     * 
     * @return the map of the parameters defined in the stack frame when the
     *         OPARAM was rendered.
     */
    public Map<String, Object> getFrameParameters() {
      return mFrameParameters;
    }

    /**
     * Get the specified frame parameter from the paramter dictionary.
     * 
     * @param pName
     *          the name of the parameter to return
     * @return the parameter value, or null if no parameter by that name was
     *         found.
     */
    public Object getFrameParameter(String pName) {
      return mFrameParameters.get(pName);
    }

    public String toString() {
      return getClass().getName() + "(name=" + getName() + ", frameParameters="
          + getFrameParameters() + ")";
    }
  } // end inner-class RenderedOutputParameter

  /**
   * Represents the result of a droplet invocation. Contains properties
   * representing the droplet, request and response. Also trackes which OPARAMs
   * were rendered during the course of the droplet.
   */
  public class DropletResult {
    Servlet mDroplet;
    DynamoHttpServletRequest mRequest;
    DynamoHttpServletResponse mResponse;
    List<RenderedOutputParameter> mRenderedParameters = new ArrayList<RenderedOutputParameter>();
    Map<String, List<RenderedOutputParameter>> mNameToRenderedParameters = new HashMap<String, List<RenderedOutputParameter>>();

    /**
     * Create a new DropletResult.
     * 
     * @param pDroplet
     *          the droplet being invoke
     * @param pRequest
     *          the request used for the droplet invocation
     * @param pResponse
     *          the response used for the droplet invocation
     */
    public DropletResult(Servlet pDroplet, DynamoHttpServletRequest pRequest,
        DynamoHttpServletResponse pResponse) {
      mDroplet = pDroplet;
      mRequest = pRequest;
      mResponse = pResponse;
    }

    /** Debugging toString. */
    public String toString() {
      StringBuilder strbuf = new StringBuilder(getClass().getName());
      strbuf.append("(droplet=").append(mDroplet);
      strbuf.append(", renderedParameters=").append(mRenderedParameters);
      strbuf.append(", nameToRenderedParameters=").append(
          mNameToRenderedParameters);
      strbuf.append(")");
      return strbuf.toString();
    }

    /**
     * Return the droplet that was invoked.
     * 
     * @return the droplet that was invoked.
     */
    public Servlet getDroplet() {
      return mDroplet;
    }

    /**
     * Add a rendered OPARAM to our list and map of OPARAMs.
     * 
     * @param pParameter
     *          the parameter to add.
     */
    public void addRenderedParameter(RenderedOutputParameter pParameter) {
      mRenderedParameters.add(pParameter);
      List<RenderedOutputParameter> listParams = mNameToRenderedParameters
          .get(pParameter.getName());
      if (listParams == null) {
        listParams = new ArrayList<RenderedOutputParameter>();
        mNameToRenderedParameters.put(pParameter.getName(), listParams);
      }
      listParams.add(pParameter);
    }

    /** Return the list of all rendered OPARAMs, in order. */
    public List<RenderedOutputParameter> getRenderedOutputParameters() {
      return mRenderedParameters;
    }

    /**
     * Return the list of all rendered OPARAMs with the specified name.
     * 
     * @param pName
     *          the name of the oparam to return.
     * @return the list of rendered OPARAMS with the specified name, or null if
     *         none exist.
     */
    public List<RenderedOutputParameter> getRenderedOutputParametersByName(
        String pName) {
      return mNameToRenderedParameters.get(pName);
    }

    /**
     * Return the OPARAM with the specified name.
     * 
     * @param pName
     *          the name of the OPARAM to return.
     * @return the list of rendered OPARAMS with the specified name, or null if
     *         none exist.
     * @exception IllegalStateException
     *              if multiple OPARAMS with the given name were rendered.
     */
    public RenderedOutputParameter getRenderedOutputParameter(String pName) {
      return getRenderedOutputParameter(pName, true);
    }

    /**
     * Return the first rendered OPARAM with the specified name.
     * 
     * @param pName
     *          the name of the OPARAM to return.
     * @param pEnforceSingle
     *          if there is more than one OPARAM with the specified name, throw
     *          an IllegalStateException.
     * @return the list of rendered OPARAMS with the specified name, or null if
     *         none exist.
     * @exception IllegalStateException
     *              if multiple OPARAMS with the given name were rendered and
     *              pEnforceSingle is true.
     */
    public RenderedOutputParameter getRenderedOutputParameter(String pName,
        boolean pEnforceSingle) {
      List<RenderedOutputParameter> listParams = getRenderedOutputParametersByName(pName);

      RenderedOutputParameter paramResult = null;

      if ((listParams != null) && !listParams.isEmpty()) {
        if (pEnforceSingle && (listParams.size() > 1)) {
          throw new IllegalStateException(
              "More than one rendered OPARAM found for " + pName);
        }
        paramResult = listParams.get(0);
      }
      return paramResult;
    }

    /**
     * Return the rendered OPARAM with the specified name and index.
     * 
     * @param pName
     *          the name of the OPARAM to return.
     * @param pIndex
     *          the index of the OPARAM to return. Can use a negative index to
     *          count from the end (-1 equals the last OPARAM rendered with that
     *          name).
     * @return the specified OPARAM, or null if an OPARAM with that name and
     *         index does not exist.
     */
    public RenderedOutputParameter getRenderedOutputParameter(String pName,
        int pIndex) {

      List<RenderedOutputParameter> listParams = getRenderedOutputParametersByName(pName);

      RenderedOutputParameter paramResult = null;

      if (listParams != null) {
        int index = pIndex;
        if (index < 0) {
          // offset from the end
          index = listParams.size() + index;
        }

        if ((index >= 0) && (index < listParams.size())) {
          paramResult = listParams.get(pIndex);
        }
      }
      return paramResult;
    }

    /**
     * A convenience method to get the specified parameter from the parameter
     * stack from of a recorded OPARAM invocation.
     * 
     * @param pParameterName
     *          the name of the parameter whose value should be returned (for
     *          example, "element")
     * @param pOutputParameterName
     *          the name of the output parameter whose parameter stack frame
     *          should be used (for example, "output" or "error")
     * @param pIndex
     *          the index of the Nth invocation of the output parameter.
     * @return the value of the parameter from the named OPARAM invocation, or
     *         null if either the OPARAM invocation or the parameter to be
     *         fetched were not found.
     */
    public Object getFrameParameterOfRenderedParameter(String pParameterName,
        String pOutputParameterName, int pIndex) {
      RenderedOutputParameter oparam = getRenderedOutputParameter(
          pOutputParameterName, pIndex);
      if (oparam != null) {
        return oparam.getFrameParameter(pParameterName);
      }
      return null;
    }

  } // end inner-class DropletResult

  // -------------------------------------------------------
  /** Our implementation of ServiceParameterCallback. */
  public class ServiceParameterCallbackImpl implements ServiceParameterCallback {
    /** The droplet result to which we will add RenderedOutputParameter. */
    DropletResult mDropletResult;

    /**
     * Create a new instance that will invoke addRenderedParameter to
     * DropletResult when didServiceParameter() is invoked.
     * 
     * @param pDropletResult
     *          to droplet result to record OPARAM rending one.
     */
    public ServiceParameterCallbackImpl(DropletResult pDropletResult) {
      mDropletResult = pDropletResult;
    }

    /**
     * In this case, create a new RenderedOutputParameter and add it to our
     * list.
     * 
     * @inheritDoc
     */
    public boolean didServiceParameter(String pParameterName,
        ServletRequest pRequest, ServletResponse pResponse, TagConverter pCvt,
        Properties pCvtArgs, boolean pResult, boolean pIsLocal) {

      mDropletResult.addRenderedParameter(createRenderedOutputParameter(
          pParameterName, (DynamoHttpServletRequest) pRequest,
          (DynamoHttpServletResponse) pResponse));

      boolean bResult = pResult;
      if (null != mOparamExistsOverrideMap.get(pParameterName)) {
        // override map always wins
        bResult = mOparamExistsOverrideMap.get(pParameterName).booleanValue();
      } else {
        if (!bResult && getOparamsExistByDefault()) {
          bResult = true;
        }
      }
      return bResult;
    }
  } // end inner-class ServiceParameterCallbackImpl

}
