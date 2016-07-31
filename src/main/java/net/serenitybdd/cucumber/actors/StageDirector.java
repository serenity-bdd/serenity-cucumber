package net.serenitybdd.cucumber.actors;

import cucumber.api.java.After;
import net.serenitybdd.screenplay.actors.OnStage;

public class StageDirector {
    @After
    public void endTheAct() {
        OnStage.drawTheCurtain();
    }
}
