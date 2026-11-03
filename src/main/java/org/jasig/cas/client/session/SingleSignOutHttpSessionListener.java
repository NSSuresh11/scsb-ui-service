package org.jasig.cas.client.session;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public final class SingleSignOutHttpSessionListener implements HttpSessionListener {

    private SessionMappingStorage sessionMappingStorage;

    @Override
    public void sessionCreated(final HttpSessionEvent event) {
        // nothing to do at the moment
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent event) {
        if (sessionMappingStorage == null) {
            sessionMappingStorage = getSessionMappingStorage();
        }
        final HttpSession session = event.getSession();
        sessionMappingStorage.removeBySessionById(session.getId());
    }

    /**
     * Obtains a {@link SessionMappingStorage} object. Assumes this method will always return the same
     * instance of the object.  It assumes this because it generally lazily calls the method.
     *
     * @return the SessionMappingStorage
     */
    protected static SessionMappingStorage getSessionMappingStorage() {
        return SingleSignOutFilter.getSingleSignOutHandler().getSessionMappingStorage();
    }
}
