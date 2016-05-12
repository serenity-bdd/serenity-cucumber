package net.serenitybdd.cucumber.actors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.webdriver.WebdriverManager;

import java.util.List;
import java.util.Map;

/**
 * Provide simple support for managing Screenplay actors in Cucumber-JVM
 */
public class Cast {

    private WebdriverManager manager = Injectors.getInjector().getInstance(WebdriverManager.class);

    Map<String, Actor> actors = Maps.newHashMap();

    public Actor actorNamed(String actorName) {

        if (! actors.containsKey(actorName)) {
            Actor newActor = Actor.named(actorName);

            newActor.can(BrowseTheWeb.with(manager.getWebdriverByName(actorName)));

            actors.put(actorName, newActor);
        }
        return actors.get(actorName);
    }

    public List<Actor> getActors() {
        return ImmutableList.copyOf(actors.values());
    }

    public void dismissAll() {
        actors.clear();
    }
}
