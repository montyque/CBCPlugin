package neonique.cbcplugin_new.gamemodes.koth.tasks;

import neonique.cbcplugin_new.gamemodes.koth.KOTHGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;

public class KOTHStartTimer extends BaseStartGameTimer {

    private final KOTHGame game;

    public KOTHStartTimer(GameManager gameManager, KOTHGame game, int countdownTimer) {
        super(game, countdownTimer);
        this.game = game;
    }

    @Override
    public void startGame() {
        // Start the game
        game.startGame();
    }
}