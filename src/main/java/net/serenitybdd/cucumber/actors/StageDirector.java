package net.serenitybdd.cucumber.actors;

import cucumber.api.java.After;

public class StageDirector {
    @After
    public void endTheAct() {
        OnStage.drawTheCurtain();
    }
}
