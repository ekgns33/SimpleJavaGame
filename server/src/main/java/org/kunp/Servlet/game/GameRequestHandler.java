package org.kunp.Servlet.game;

import org.kunp.Servlet.menu.WaitingRoomContext;
import org.kunp.Servlet.message.GameMessage;
import org.kunp.Servlet.session.Session;
import org.kunp.Servlet.session.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class GameRequestHandler {
    private static GameRequestHandler gameRequestHandler;

    private GameRequestHandler() {
    }

    public static GameRequestHandler getInstance() {
        if (gameRequestHandler == null) {
            gameRequestHandler = new GameRequestHandler();
        }
        return gameRequestHandler;
    }

    public int createGameContextAndJoinAll(WaitingRoomContext context) {
        int gameId =
        GameContextRegistry.getInstance()
            .createGameContext(
                context.getRoomName(), context.getHostId(), context.getUserLimit(), context.getTimeLimit());
        for(Map.Entry<String, OutputStream> entry : context.getParticipants().entrySet()) {
            Session user = SessionManager.getInstance().getSession(entry.getKey());
            GameContextRegistry.getInstance().subscribe(user, gameId);
        }
        GameContextRegistry.getInstance().startGameContext(gameId);
        return gameId;
    }

    public void handleGameRequest(Session session, GameMessage parsedMessage) throws IOException {

      GameContextRegistry.getInstance().subscribe(session, parsedMessage.getGameId());
      if (parsedMessage.getType() == 201) {
        GameContextRegistry.getInstance().updatePositionState(parsedMessage);
      } else if (parsedMessage.getType() == 202) {
        GameContextRegistry.getInstance().interact(parsedMessage);
      }
    }
}
