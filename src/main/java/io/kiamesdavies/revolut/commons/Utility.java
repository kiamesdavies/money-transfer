package io.kiamesdavies.revolut.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class
 */
public class Utility {

    /**
     * Disable creation of utility class
     */
    private Utility(){

    }

    public static byte[] toBytes(Object obj) {
        try {
            return new ObjectMapper().writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            return new byte[0];
        }
    }


}
