package org.jasig.cas.client.authentication;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthenticationRedirectStrategy {

    /**
     * Method name is a bit of a misnomer.  This method handles "redirection" for a localized version of redirection (i.e. AJAX might mean an XML fragment that contains the url to go to).
     *
     * @param request the original HttpServletRequest.   MAY NOT BE NULL.
     * @param response the original HttpServletResponse.  MAY NOT BE NULL.
     * @param potentialRedirectUrl the url that might be used (there are no guarantees of course!)
     * @throws IOException the exception to throw if there is some type of error.  This will bubble up through the filter.
     */
    void redirect(HttpServletRequest request, HttpServletResponse response, String potentialRedirectUrl)
            throws IOException;

}
