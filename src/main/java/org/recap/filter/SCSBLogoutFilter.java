package org.recap.filter;

import org.recap.ScsbConstants;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by sheiks on 03/03/17.
 */
public class SCSBLogoutFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest)request).getSession();
        Object attribute = session.getAttribute(ScsbConstants.USER_TOKEN);
        request.setAttribute(ScsbConstants.USER_TOKEN,attribute);
        chain.doFilter(request,response);
    }
}
