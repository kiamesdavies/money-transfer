package io.kiamesdavies.revolut;

import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;

import java.util.concurrent.ExecutionException;

/**
 * Creation of this universe
 */
public class BigBang extends HttpApp {

    private final Bootstrap instance;

    public BigBang() {
        instance = Bootstrap.getInstance();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        BigBang bigBang = new BigBang();
        bigBang.startServer("0.0.0.0", 8080, bigBang.instance.actorSystem);
    }


    /**
     * Override to implement the route that will be served by this http server.
     */
    @Override
    protected Route routes() {
        return instance.route;
    }
}
