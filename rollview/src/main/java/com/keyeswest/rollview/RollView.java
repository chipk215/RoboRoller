package com.keyeswest.rollview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;


// Attribution: http://pygmalion.nitri.org/android-gauge-view-1039.html

public class RollView extends View {

    private static final String TAG = "RollView";

    // --------------------- View Constructors ----------------------------
    public RollView(Context context) {
        super(context);
        initialize();
    }

    public RollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public RollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }


    private void initialize(){
        //initValues();
        //initPaint();
    }
}
