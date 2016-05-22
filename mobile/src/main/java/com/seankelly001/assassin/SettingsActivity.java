/* This class is used for the Settings page */

package com.seankelly001.assassin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;

public class SettingsActivity extends Activity implements View.OnClickListener {

    final static String TAG = "ASSASSIN";

    private static final int RC_SELECT_PHOTO = 8000;
    private static final int[] CLICKABLES = {R.id.select_photo, R.id.save, R.id.cancel};
    private ImageView selected_photo;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor preferences_editor;
    private static final String PREFERENCES_NAME = "com.seankelly001.assassin";
    private static final String IMAGE_PATH_KEY = "IMAGE_PATH_KEY";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        //Get user preferences
        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);
        preferences_editor = preferences.edit();

        selected_photo = (ImageView) findViewById(R.id.selected_photo_view);

        //Attempt to load image
        String image_path = preferences.getString(IMAGE_PATH_KEY, null);
        if(image_path != null)
            setImage(image_path);

        for (int id : CLICKABLES)
            findViewById(id).setOnClickListener(this);
    }


    //When returning from select photo screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case RC_SELECT_PHOTO:
                    photoSelected(data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    //Select a photo from gallery etc
    private void selectPhoto() {

        Intent select_photo_intent = new Intent(Intent.ACTION_PICK);
        select_photo_intent.setType("image/*");
        startActivityForResult(select_photo_intent, RC_SELECT_PHOTO);
    }


    //Photo has been selected
    private void photoSelected(Intent data) {

        //Get the photo from the data
        Uri selected_photo = data.getData();
        String[] file_path = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selected_photo, file_path, null, null, null);
        cursor.moveToFirst();
        String image_path = cursor.getString(cursor.getColumnIndex(file_path[0]));
        cursor.close();

        //Store the image path
        preferences_editor.putString(IMAGE_PATH_KEY, image_path);
        Log.e(TAG, "Image path: " + image_path);
        //Set the image on screen
        setImage(image_path);
    }


    //Set image on screen
    private void setImage(String image_path) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image_path, options);
        options.inSampleSize = DataTypeUtils.calculateInSampleSize(options, 300, 300);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(image_path, options);
        ImageView selected_photo_view = (ImageView) findViewById(R.id.selected_photo_view);
        selected_photo_view.setImageBitmap(bitmap);
    }


    //User saves changes made
    private void saveClick() {

        preferences_editor.commit();
        finish();
    }


    //User discards changes made
    private void cancelClick() {

        finish();
    }


    //When user clicks a button
    @Override
    public void onClick(View v) {

        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        switch (v.getId()) {
            case R.id.select_photo:
                selectPhoto();
                break;
            case R.id.save:
                saveClick();
                break;
            case R.id.cancel:
                cancelClick();
                break;
        }
    }

}
