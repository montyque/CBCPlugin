package neonique.cbcplugin_new.tasks.gamemodetasks.assassin;

import neonique.cbcplugin_new.gamemodes.assassin.AssassinGame;
import neonique.cbcplugin_new.gamemodes.kmation.KMationGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;

public class AssassinStartGameTimer extends BaseStartGameTimer {

    private final AssassinGame game;

    public AssassinStartGameTimer(GameManager gameManager, AssassinGame game, int countdownTimer) {

        super(gameManager, game, countdownTimer);
        this.game = game;

    }

    @Override
    public void startGame() {
        game.startGame();
    }

}
