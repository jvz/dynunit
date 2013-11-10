/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test;

import atg.droplet.GenericFormHandler;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import java.io.IOException;

/**
 * A form handler implemention to be used as example code for testing.
 *
 * @author adamb
 */
public class SimpleFormHandler
        extends GenericFormHandler {

    public static final String REDIRECT_URL_PARAM_NAME = "redirectURL";

    public String mErrorURL = "/error.jsp";

    /**
     * Sets the URL to which this formhandler redirects on any error.
     *
     * @param pURL
     */
    public void setErrorURL(String pURL) {
        mErrorURL = pURL;
    }

    /**
     * Returns the URL to which this formhandler redirects on any error
     *
     * @return
     */
    public String getErrorURL() {
        return mErrorURL;
    }

    /**
     * This is a contrived example form handler use to demonstrate testing a very
     * basic formhandler with DUST. A Simple "handler" method implementation. It
     * calls redirect given a value in a request parameter.
     *
     * @param pRequest
     * @param pResponse
     *
     * @return
     * @throws IOException
     */
    public boolean handleRedirect(DynamoHttpServletRequest pRequest,
                                  DynamoHttpServletResponse pResponse)
            throws IOException {
        mDidRedirect = false;
        String redirectURL = pRequest.getParameter("redirectURL");
        if ( redirectURL == null ) {
            pResponse.sendLocalRedirect(mErrorURL, pRequest);
        } else {
            pResponse.sendLocalRedirect(redirectURL, pRequest);
            mDidRedirect = true;
        }
        return false;
    }

    private boolean mDidRedirect = false;

    /**
     * Set to true if the last request resulted in a successful redirect. False
     * otherwise
     *
     * @return
     */
    public boolean didRedirect() {
        return mDidRedirect;
    }

}
