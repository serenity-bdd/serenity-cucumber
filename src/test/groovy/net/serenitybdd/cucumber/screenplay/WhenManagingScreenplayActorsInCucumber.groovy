package net.serenitybdd.cucumber.screenplay

import net.serenitybdd.cucumber.actors.Cast
import spock.lang.Specification

public class WhenManagingScreenplayActorsInCucumber extends Specification {

    def "Actors are identified by their names"() {
        given:
            Cast cast = new Cast();
        when:
            def jamesDean = cast.actorNamed("James Dean")
        then:
            cast.actorNamed("James Dean") == jamesDean
    }

    def "Actors can be assigned a webdriver type"() {
        given:
            Cast cast = new Cast();
        when:
            def jamesDean = cast.actorUsingBrowser("chrome").named("James Dean")
        then:
            cast.actorNamed("James Dean") == jamesDean
    }


    def "An actor is only cast once"() {
        given:
            Cast cast = new Cast();
        when:
            def jamesDean = cast.actorNamed("James Dean")
        and:
            cast.actorNamed("James Dean")
        then:
            cast.actorNamed("James Dean") == jamesDean
    }

    def "Cast can be dismissed"() {
        given:
            Cast cast = new Cast();
            cast.actorNamed("James Dean")
        when:
            cast.dismissAll()
        then:
            cast.actors.isEmpty()
    }



}
