package neonique.cbcplugin_new.tasks.gamemodetasks.holdthegold;

import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;

public class HTGStartGameTimer extends BaseStartGameTimer {

    private final HTGGame game;

    public HTGStartGameTimer(GameManager gameManager, HTGGame game, int countdownTimer) {

        super(gameManager, game, countdownTimer);
        this.game = game;

    }

    @Override
    public void startGame() {
        game.startGame();
    }
}
