package com.seankelly001.assassin;

import com.seankelly001.assassin.MainActivity;

import org.junit.Test;

import java.lang.Exception;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class TestMain {

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void doubleToByteArrayTest() throws Exception {

        assertEquals(MainActivity.doubleToByteArray(3.5), );
    }
}