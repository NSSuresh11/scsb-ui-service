package org.jasig.cas.client.authentication;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasig.cas.client.util.CommonUtils;

public final class FacesCompatibleAuthenticationRedirectStrategy implements AuthenticationRedirectStrategy {

    private static final String FACES_PARTIAL_AJAX_PARAMETER = "javax.faces.partial.ajax";

    @Override
    public void redirect(final HttpServletRequest request, final HttpServletResponse response,
                         final String potentialRedirectUrl) throws IOException {

        if (CommonUtils.isNotBlank(request.getParameter(FACES_PARTIAL_AJAX_PARAMETER))) {
            // this is an ajax request - redirect ajaxly
            response.setContentType("text/xml");
            response.setStatus(200);

            final PrintWriter writer = response.getWriter();
            writer.write("<?xml version='1.0' encoding='UTF-8'?>");
            writer.write(String.format("<partial-response><redirect url=\"%s\"></redirect></partial-response>",
                    potentialRedirectUrl));
        } else {
            response.sendRedirect(potentialRedirectUrl);
        }
    }
}
