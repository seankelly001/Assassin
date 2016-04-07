package com.seankelly001.assassin;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.prefs.Preferences;

/**
 * Created by Sean on 03/04/2016.
 */
public class SettingsActivity extends Activity implements View.OnClickListener {

    private static final int RC_SELECT_PHOTO = 8000;
    private static final int[] CLICKABLES = {R.id.select_photo, R.id.save, R.id.cancel};

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor preferences_editor;
    private static final String PREFERENCES_NAME = "com.seankelly001.assassin";
    private static final String IMAGE_PATH_KEY = "IMAGE_PATH_KEY";

    private Button hold_me;
    private ImageView selected_photo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);
        preferences_editor = preferences.edit();

        String image_path = preferences.getString(IMAGE_PATH_KEY, null);
        Log.e("#####", "IMAGE PATH IS: " + image_path);

        if(image_path != null) {
            setImage(image_path);
        }

        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }

        selected_photo = (ImageView) findViewById(R.id.selected_photo_view);

        hold_me = (Button) findViewById(R.id.hold_me);
        hold_me.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    //makeToast("DOWN");
                    selected_photo.setVisibility(View.VISIBLE);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                   // makeToast("UP");
                    selected_photo.setVisibility(View.GONE);
                }
                return false;
            }
        });

    }


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


    private void selectPhoto() {

        Intent select_photo_intent = new Intent(Intent.ACTION_PICK);
        select_photo_intent.setType("image/*");
        startActivityForResult(select_photo_intent, RC_SELECT_PHOTO);
    }


    private void photoSelected(Intent data) {

        Uri selected_photo = data.getData();
        String[] file_path = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selected_photo, file_path, null, null, null);
        cursor.moveToFirst();
        String image_path = cursor.getString(cursor.getColumnIndex(file_path[0]));
        cursor.close();

        preferences_editor.putString(IMAGE_PATH_KEY, image_path);
        Log.e("#####", "New image path: " + image_path);
        setImage(image_path);
    }


    private void setImage(String image_path) {

        BitmapFactory.Options options = new BitmapFactory.Options();
       // options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(image_path, options);

        ImageView selected_photo_view = (ImageView) findViewById(R.id.selected_photo_view);
        selected_photo_view.setImageBitmap(bitmap);
    }


    private void saveClick() {

        preferences_editor.commit();
        finish();
    }


    private void cancelClick() {

        finish();
    }


    @Override
    public void onClick(View v) {

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

    private void makeToast(String s) {

        Log.e("#####", s);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
