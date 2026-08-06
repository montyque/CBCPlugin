package neonique.cbcplugin_new.gamemodes.ctf.tasks;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.core.tasks.BaseStartGameTimer;

public class CTFStartGameTimer extends BaseStartGameTimer {

    private final CTFGame game;

    public CTFStartGameTimer(GameManager gameManager, CTFGame game, int countdownTimer) {

        super(game, countdownTimer);
        this.game = game;

    }

    @Override
    public void startGame() {
        game.startGame();
    }
}
