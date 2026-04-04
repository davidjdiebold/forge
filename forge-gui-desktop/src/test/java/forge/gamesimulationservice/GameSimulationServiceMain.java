package forge.gamesimulationservice;

import com.sun.net.httpserver.HttpServer;
import forge.GuiDesktop;
import forge.deck.DeckFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.gamesimulationservice.runner.Rules;
import forge.gamesimulationservice.service.*;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.gamesimulationservice.model.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * cd forge
 * mvn clean install -DskipTests
 * cd forge-desktop-gui
 * mvn clean package assembly:single@make-uber-jar-with-tests
 * cd target
 *
 */
public class GameSimulationServiceMain {
    public static void main(String[] args) {
        try {
            String strPort = System.getenv("PORT");
            int port = Integer.parseInt(strPort!=null ? strPort : "8001");

            GuiBase.setInterface(new GuiDesktop());
            FModel.initialize(null, preferences -> {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                return null;
            });

            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
            GameRules gameRules = new GameRules(GameType.Constructed);
            gameRules.setManaBurn(true);
            Rules rules = new Rules(
                    gameRules,
                    FModel.getFormats().get9394French(),
                    DeckFormat.Constructed
            );
            GameRunnerController controller = new GameRunnerController(rules);

            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 16);

            server.createContext("/decks", new HttpHandler(new DecksHandler(controller), GetDecksRequest.class));
            server.createContext("/game", new HttpHandler(new GameHandler(controller), PostGamesRequest.class));
            server.createContext("/clear", new HttpHandler(new ClearGamesHandler(controller), ClearGamesRequest.class));
            server.createContext("/gamestat", new HttpHandler(new GameStatHandler(controller), GetGameStatsRequest.class));
            server.createContext("/gameoutcome", new HttpHandler(new GameOutcomesHandler(controller), GetGameOutcomesRequest.class));

            server.setExecutor(threadPoolExecutor);
            server.start();
            System.out.println("        _            ___                      __                          ");
            System.out.println("  /\\/\\ | |_ __ _    / __\\__  _ __ __ _  ___  / _\\ ___ _ ____   _____ _ __ ");
            System.out.println(" /    \\| __/ _` |  / _\\/ _ \\| '__/ _` |/ _ \\ \\ \\ / _ \\ '__\\ \\ / / _ \\ '__|");
            System.out.println("/ /\\/\\ \\ || (_| | / / | (_) | | | (_| |  __/ _\\ \\  __/ |   \\ V /  __/ |   ");
            System.out.println("\\/    \\/\\__\\__, | \\/   \\___/|_|  \\__, |\\___| \\__/\\___|_|    \\_/ \\___|_|   ");
            System.out.println("           |___/                 |___/                                    ");
            System.out.println("http://0.0.0.0:8001/game");
            System.out.println("http://0.0.0.0:8001/gamestat");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
