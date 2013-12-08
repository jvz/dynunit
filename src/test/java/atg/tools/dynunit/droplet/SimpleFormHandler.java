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

package atg.tools.dynunit.droplet;

import atg.droplet.GenericFormHandler;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A form handler implementation to be used as example code for testing.
 *
 * @author adamb
 */
public class SimpleFormHandler
        extends GenericFormHandler {

    public static final String REDIRECT_URL_PARAM_NAME = "redirectURL";

    private String errorURL = "/error.jsp";

    /**
     * Sets the URL to which this formhandler redirects on any error.
     *
     * @param errorURL
     */
    public void setErrorURL(String errorURL) {
        this.errorURL = errorURL;
    }

    /**
     * Returns the URL to which this formhandler redirects on any error
     *
     * @return
     */
    public String getErrorURL() {
        return errorURL;
    }

    /**
     * This is a contrived example form handler use to demonstrate testing a very
     * basic formhandler with DUST. A Simple "handler" method implementation. It
     * calls redirect given a value in a request parameter.
     *
     * @param request
     * @param response
     *
     * @return
     *
     * @throws IOException
     */
    public boolean handleRedirect(@NotNull DynamoHttpServletRequest request,
                                  DynamoHttpServletResponse response)
            throws IOException {
        redirected = false;
        String redirectURL = request.getParameter(REDIRECT_URL_PARAM_NAME);
        if (redirectURL == null) {
            response.sendLocalRedirect(errorURL, request);
        }
        else {
            response.sendLocalRedirect(redirectURL, request);
            redirected = true;
        }
        return false;
    }

    private boolean redirected = false;

    /**
     * Set to true if the last request resulted in a successful redirect. False
     * otherwise
     *
     * @return
     */
    public boolean isRedirected() {
        return redirected;
    }

}
