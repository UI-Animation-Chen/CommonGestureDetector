package com.chenzhifei.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SketchView sketchView = findViewById(R.id.sketch_view);

        SwitchCompat switchView = findViewById(R.id.operate_img);
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwitchCompat)v).setChecked(sketchView.setOperatingImage());
            }
        });
        switchView.setChecked(true);

        findViewById(R.id.add_a_screen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sketchView.addScreenNum();
            }
        });
        findViewById(R.id.save_to_jpg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sketchView.saveToJPG();
            }
        });
    }

}
