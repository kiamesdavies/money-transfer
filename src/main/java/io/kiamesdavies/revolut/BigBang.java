package io.kiamesdavies.revolut;

import com.twitter.chill.KryoInstantiator;
import com.twitter.chill.KryoPool;
import io.kiamesdavies.revolut.models.Evt;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Hello world!
 */
public class BigBang {


    public static void main(String[] args) throws IOException {
        Bootstrap.getInstance();
        System.out.println("Hello World!");
        System.in.read(); // let it run until user presses return
    }


}
