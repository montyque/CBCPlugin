package neonique.cbcplugin_new.tasks.gamemodetasks.rendezvous;

import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;

public class RendezvousStartTimer extends BaseStartGameTimer {

    private final RendezvousGame game;

    public RendezvousStartTimer(GameManager gameManager, RendezvousGame game, int countdownTimer) {

        super(gameManager, game, countdownTimer);
        this.game = game;

    }

    @Override
    public void startGame() {
        game.startGame();
    }
}