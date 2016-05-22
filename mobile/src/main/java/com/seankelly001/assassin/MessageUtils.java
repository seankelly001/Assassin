/* Helper class to create and decode message sent between players */

package com.seankelly001.assassin;

import com.google.android.gms.maps.model.LatLng;
import org.apache.commons.lang3.ArrayUtils;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MessageUtils {

    //Create a message from a string
    public static byte[] createStringMessage(final char flag, String s) {

        byte[] iden_bytes = {(byte) flag};
        byte[] s_bytes = s.getBytes(Charset.forName("UTF-8"));
        return ArrayUtils.addAll(iden_bytes, s_bytes);
    }


    //Decode a message that will give a string
    public static String decodeStringMessage(byte[] buf) {

        byte[] message_bytes = ArrayUtils.subarray(buf, 1, buf.length);
        return new String(message_bytes, Charset.forName("UTF-8"));
    }


    //Create a coordinate message
    public static byte[] createCoordinateMessage(char flag, LatLng lat_lng) {

        if(lat_lng != null) {

            double lat = lat_lng.latitude;
            double lng = lat_lng.longitude;
            byte[] iden_bytes = {(byte) flag};
            byte[] lat_bytes = DataTypeUtils.doubleToByteArray(lat);
            byte[] lng_bytes = DataTypeUtils.doubleToByteArray(lng);
            return ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(lat_bytes, lng_bytes));
        }
        else {
            return null;
        }
    }


    //Decode a message to get a latlng
    public static LatLng decodeCoordinateMessage(byte[] message_bytes) {

        byte[] lat_bytes = ArrayUtils.subarray(message_bytes, 1, 9);
        byte[] lng_bytes = ArrayUtils.subarray(message_bytes, 9, 17);
        double lat = DataTypeUtils.byteArraytoDouble(lat_bytes);
        double lng = DataTypeUtils.byteArraytoDouble(lng_bytes);
        return new LatLng(lat, lng);
    }


    //Create a sub image message
    public static byte[] createSubImageMessage(final char FLAG, byte[] message_bytes, int current_sub_array_count, int total_count, int start_pos, int sub_array_size) {

        //Set up the identifier bytes
        byte[] iden_bytes = {(byte) FLAG};
        byte[] current_sub_array_count_array = ByteBuffer.allocate(4).putInt(current_sub_array_count).array();
        byte[] total_sub_array_count_array = ByteBuffer.allocate(4).putInt(total_count).array();
        byte[] iden_bytes_array = ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(current_sub_array_count_array, total_sub_array_count_array));
        byte[] sub_message_bytes = ArrayUtils.subarray(message_bytes, start_pos, start_pos + sub_array_size);
        byte[] message = ArrayUtils.addAll(iden_bytes_array, sub_message_bytes);
        return message;
    }


    public static void receivedSubPictureMessage(byte[] message, ArrayList<byte[]> picture_array_list) throws Exception{

        int current_byte_pos = DataTypeUtils.byteToInt(ArrayUtils.subarray(message, 1, 5));
        byte[] bitmap_data = ArrayUtils.subarray(message, 9, message.length);
        picture_array_list.set(current_byte_pos, bitmap_data);
    }
}
