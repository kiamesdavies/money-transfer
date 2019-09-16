package io.kiamesdavies.revolut;

import akka.actor.ActorRef;
import akka.http.javadsl.ServerBinding;

import java.io.IOException;

/**
 * Hello world!
 */
public class BigBang {


    public static void main(String[] args) throws IOException {

        Bootstrap instance = Bootstrap.getInstance();




        System.in.read(); // let it run until user presses return

        instance.binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> instance.actorSystem.terminate()); // and shutdown when done
    }


}
