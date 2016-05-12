package net.serenitybdd.cucumber.actors;

import net.serenitybdd.screenplay.Actor;

public class OnStage {

    private static final ThreadLocal<Stage> stage = new ThreadLocal<>();
    static {
        stage.set(new Stage(new Cast()));
    }

    public static Actor theActorCalled(String requiredActor) {
        return stage().shineSpotlightOn(requiredActor);
    }

    public static Actor theActorInTheSpotlight() {
        return stage().theActorInTheSpotlight();
    }

    private static Stage stage() { return stage.get(); }

    public static void drawTheCurtain() {
        stage().drawTheCurtain();
    }
}
