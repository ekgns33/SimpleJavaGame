package org.kunp.Servlet.session;

import static org.kunp.Servlet.session.Session.createEmptySession;

public class SessionManager {

  private final SessionStorage sessionStorage;
  private final ISessionIdGenerator sessionIdGenerator;
  private static final Map<String, Session> clientSessions = new ConcurrentHashMap<>();

  public SessionManager(SessionStorage sessionStorage, ISessionIdGenerator sessionIdGenerator) {
    this.sessionStorage = sessionStorage;
    this.sessionIdGenerator = sessionIdGenerator;
  }

  public Session createSession() {
    Session session = createEmptySession();
    String sessionId = sessionIdGenerator.generateSessionId();
    session.setSessionId(sessionId);
    sessionStorage.save(sessionId, session);
    return session;
  }

  public Session getSession(String sessionId) {
    return (Session) sessionStorage.get(sessionId);
  }

  public void removeSession(String sessionId) {
    sessionStorage.remove(sessionId);
  }

  public void clear() {
    sessionStorage.clear();
  }
}
