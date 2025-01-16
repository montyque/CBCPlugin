package neonique.cbcplugin_new.tasks.gamemodetasks.kmation;

import neonique.cbcplugin_new.gamemodes.kmation.KMationGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;

public class KMationStartGameTimer extends BaseStartGameTimer {

    private final KMationGame game;

    public KMationStartGameTimer(GameManager gameManager, KMationGame game, int countdownTimer) {

        super(gameManager, game, countdownTimer);
        this.game = game;

    }

    @Override
    public void startGame() {
        game.startGame();
    }
}
