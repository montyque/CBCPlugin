package neonique.cbcplugin_new.gamemodes.tdm.tasks;

import neonique.cbcplugin_new.gamemodes.tdm.TDMGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;

public class TDMStartGameTimer extends BaseStartGameTimer {

    private final TDMGame game;

    public TDMStartGameTimer(GameManager gameManager, TDMGame game, int countdownTimer) {

        super(gameManager, game, countdownTimer);
        this.game = game;

    }

    @Override
    public void startGame() {
        game.startGame();
    }
}
