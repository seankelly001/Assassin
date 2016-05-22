/* This class is used for various data transformation methods */

package com.seankelly001.assassin;

import android.graphics.BitmapFactory;
import android.location.Location;
import com.google.android.gms.maps.model.LatLng;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DataTypeUtils {

    public static byte[] doubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }


    public static double byteArraytoDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }


    public static int byteToInt(byte[] array) {
        ByteBuffer wrapped = ByteBuffer.wrap(array);
        return wrapped.getInt();
    }


    public static LatLng locationToLatLng(Location l) {
        return new LatLng(l.getLatitude(), l.getLongitude());
    }


    //Check if all of the byte arrays in an arraylist contain something
    public static boolean isFull(ArrayList<byte[]> list) {

        if (list == null)
            return false;
        for (byte[] bx : list) {
            if (bx == null || bx.length == 0)
                return false;
        }
        return true;
    }


    //Check if all values in a hashmap are true
    public static boolean hashMapBool(HashMap<String, Boolean> map) {

        Iterator<HashMap.Entry<String,Boolean>> iter = map.entrySet().iterator();
        while(iter.hasNext()) {
            HashMap.Entry<String, Boolean> entry = iter.next();
            if(entry == null)
                iter.remove();
        }

        for(String s: map.keySet()) {
            if(!map.get(s))
                return false;
        }
        return true;
    }


    //Method obtained from stackoverflow.com
    //http://stackoverflow.com/questions/477572/strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object
    //This method helps to load an image into memory without running into out of memory issues
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        //Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
