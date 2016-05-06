package com.seankelly001.assassin;

import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.ant.shaded.IOUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;


public class TestMain {

    @Test
    public void pictureMessageTest() throws Exception {

        MainActivity activity = new MainActivity();

        String image_path  = "E:\\Users\\Sean\\Documents\\College\\Year 4\\CA400 - Project\\Testing\\UnitTesting\\test.jpg";
        File f = new File(image_path);
        FileInputStream stream = new FileInputStream(f);
        byte[] message_bytes = IOUtil.toByteArray(stream);

        final int image_bytes_size = Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 9;
        int current_sub_array_count = 0;
        int total_sub_array_count = (int) Math.ceil(((double) message_bytes.length) / ((double) image_bytes_size));
        ArrayList<byte[]> picture_byte_array_list = new ArrayList<>(Collections.nCopies(total_sub_array_count, new byte[]{}));

        for(int i = 0; i < message_bytes.length; i += (image_bytes_size)) {
            byte[] image_byte_message = activity.createSubImageMessage(message_bytes, current_sub_array_count, total_sub_array_count, i, image_bytes_size);
            activity.receivedSubPictureMessage(image_byte_message, picture_byte_array_list);
            current_sub_array_count++;
        }

        byte[] input_image = {};
        for (byte[] bx : picture_byte_array_list)
            input_image = ArrayUtils.addAll(input_image, bx);

        assertTrue(Arrays.equals(message_bytes, input_image));
    }


    @Test
    public void textMessageTestPass() throws Exception {

        MainActivity activity = new MainActivity();
        String inital_string = "This is a test string";
        byte[] test_bytes = activity.createStringMessage('t', inital_string);
        String test_string = activity.decodeStringMessage(test_bytes);
        assertTrue("Testing test message pass", inital_string.equals(test_string));
    }

    @Test
    public void textMessageTestFail() throws Exception {

        MainActivity activity = new MainActivity();
        String inital_string = "This is a test string";
        byte[] test_bytes = activity.createStringMessage('t', inital_string);
        String test_string = activity.decodeStringMessage(test_bytes);
        String false_string = inital_string + " ";
        assertFalse("Testing test message fail", false_string.equals(test_string));
    }



    @Test
    public void coordinateMessageTestPass() throws Exception {

        MainActivity activity = new MainActivity();

        LatLng lat_lng = new LatLng(53.3858, 6.2589);
        byte[] test_bytes = activity.createCoordinateMessage(lat_lng);
        LatLng test_lat_lng = activity.decodeCoordinateMessage(test_bytes);
        assertTrue("Testing coordinate message pass", lat_lng.equals(test_lat_lng));
    }



    @Test
    public void coordinateMessageTestFail() throws Exception {

        MainActivity activity = new MainActivity();

        LatLng lat_lng = new LatLng(53.3858, 6.2589);
        byte[] test_bytes = activity.createCoordinateMessage(lat_lng);
        LatLng test_lat_lng = activity.decodeCoordinateMessage(test_bytes);
        LatLng false_lat_lng = new LatLng(53,6);
        assertFalse("Testing coordinate message fail", test_lat_lng.equals(false_lat_lng));
    }
}