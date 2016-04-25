package com.seankelly001.assassin;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RadioGroup;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Sean on 23/04/2016.
 */
public class AboutGameActivity extends Activity implements View.OnClickListener {

    SimpleExpandableListAdapter adapter;

    private final int[] CLICKABLES = {R.id.about_game_objective_heading,
            R.id.about_game_invite_friends_heading,
            R.id.about_game_quick_game_heading,
            R.id.about_game_lobby_heading,
            R.id.about_game_rules_heading};

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_game_layout);

        Log.e("#####", "ABOUT GAME STARTED");

        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {

        View vv;

        switch (v.getId()) {
            case R.id.about_game_objective_heading:
                vv = findViewById(R.id.about_game_objective_content);
                break;
            case R.id.about_game_invite_friends_heading:
                vv = findViewById(R.id.about_game_invite_friends_content);
                break;
            case R.id.about_game_quick_game_heading:
                vv = findViewById(R.id.about_game_quick_game_content);
                break;
            case  R.id.about_game_lobby_heading:
                vv = findViewById(R.id.about_game_lobby_content);
                break;
            case R.id.about_game_rules_heading:
                vv = findViewById(R.id.about_game_rules_content);
                break;
            default:
                vv = null;
        }

        if(vv != null) {
            if (vv.getVisibility() == View.GONE)
                expand(vv);
            else
                collapse(vv);
        }
    }

    public static void expand(final View v) {
        v.measure(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        Log.e("####", "EXPAND");
        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? RadioGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }


    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Log.e("####", "COLLAPSE");
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

}
