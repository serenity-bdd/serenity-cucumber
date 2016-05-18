package net.serenitybdd.cucumber.actors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.webdriver.WebdriverManager;

import java.util.List;
import java.util.Map;

import static net.thucydides.core.webdriver.WebDriverFactory.DEFAULT_DRIVER;

/**
 * Provide simple support for managing Screenplay actors in Cucumber-JVM
 */
public class Cast {

    private WebdriverManager manager = Injectors.getInjector().getInstance(WebdriverManager.class);

    private final String WITH_NO_SPECIFIED_DRIVER = "";

    Map<String, Actor> actors = Maps.newHashMap();

    public Actor actorNamed(String actorName) {
        return actorNamed(actorName, WITH_NO_SPECIFIED_DRIVER);
    }

    public Actor actorNamed(String actorName, String driver) {

        if (! actors.containsKey(actorName)) {
            Actor newActor = Actor.named(actorName);

            newActor.can(BrowseTheWeb.with(manager.getWebdriverByName(actorName, driver)));

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

    public BrowsingActorBuilder actorUsingBrowser(String driver) {
        return new BrowsingActorBuilder(this, driver);
    }

    public class BrowsingActorBuilder {

        private final Cast cast;
        private final String driver;

        public BrowsingActorBuilder(Cast cast, String driver) {
            this.cast = cast;
            this.driver = driver;
        }

        public Actor named(String actorName) {
            return cast.actorNamed(actorName, driver);
        }
    }
}
