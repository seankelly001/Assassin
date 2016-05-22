/* Activity for the About Game screen */

package com.seankelly001.assassin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RadioGroup;


public class AboutGameActivity extends Activity implements View.OnClickListener {

    private final int[] CLICKABLES = {R.id.about_game_objective_heading,
            R.id.about_game_invite_friends_heading,
            R.id.about_game_quick_game_heading,
            R.id.about_game_lobby_heading,
            R.id.about_game_rules_heading};

    //Called when activity is started
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_game_layout);

        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
    }


    //Expand or collapse section depending on what's clicked
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


    //Expand a section
    public static void expand(final View v) {

        v.measure(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

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

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }


    //Collapse a section
    public static void collapse(final View v) {

        final int initialHeight = v.getMeasuredHeight();
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
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
}
