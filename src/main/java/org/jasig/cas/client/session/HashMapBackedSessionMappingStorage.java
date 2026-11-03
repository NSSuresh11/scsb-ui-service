package org.jasig.cas.client.session;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HashMapBackedSessionMappingStorage implements SessionMappingStorage {

    /**
     * Maps the ID from the CAS server to the Session.
     */
    private final Map<String, HttpSession> MANAGED_SESSIONS = new HashMap<String, HttpSession>();

    /**
     * Maps the Session ID to the key from the CAS Server.
     */
    private final Map<String, String> ID_TO_SESSION_KEY_MAPPING = new HashMap<String, String>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public synchronized void addSessionById(final String mappingId, final HttpSession session) {
        ID_TO_SESSION_KEY_MAPPING.put(session.getId(), mappingId);
        MANAGED_SESSIONS.put(mappingId, session);

    }

    @Override
    public synchronized void removeBySessionById(final String sessionId) {
        logger.debug("Attempting to remove Session=[{}]", sessionId);

        final String key = ID_TO_SESSION_KEY_MAPPING.get(sessionId);

        if (logger.isDebugEnabled()) {
            if (key != null) {
                logger.debug("Found mapping for session.  Session Removed.");
            } else {
                logger.debug("No mapping for session found.  Ignoring.");
            }
        }
        MANAGED_SESSIONS.remove(key);
        ID_TO_SESSION_KEY_MAPPING.remove(sessionId);
    }

    @Override
    public synchronized HttpSession removeSessionByMappingId(final String mappingId) {
        final HttpSession session = MANAGED_SESSIONS.get(mappingId);

        if (session != null) {
            removeBySessionById(session.getId());
        }

        return session;
    }
}
