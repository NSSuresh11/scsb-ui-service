package org.recap.security;

import org.recap.ScsbConstants;
import org.recap.util.HelperUtil;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import jakarta.servlet.http.HttpSessionEvent;

/**
 * Created by sheiks on 27/03/17.
 */
public class SCSBHttpSessionEventPublisher extends HttpSessionEventPublisher {
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Object attribute = event.getSession().getAttribute(ScsbConstants.USER_TOKEN);
        HelperUtil.logoutFromShiro(attribute);
        super.sessionDestroyed(event);
    }
}
