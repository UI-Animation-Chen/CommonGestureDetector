package com.chenzhifei.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SketchView sketchView = findViewById(R.id.sketch_view);

        final SwitchCompat switchView = findViewById(R.id.operate_img);
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
        RadioGroup rg = findViewById(R.id.radio_group);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.curve:
                        sketchView.setLineMode(SketchView.LINE_MODE_CURVE);
                        break;
                    case R.id.straight_line:
                        sketchView.setLineMode(SketchView.LINE_MODE_STRAIGHT);
                        break;
                    case R.id.rubber:
                        sketchView.setLineMode(SketchView.LINE_MODE_RUBBER);
                        break;
                }
            }
        });
        RadioGroup colorRg = findViewById(R.id.color_rg);
        colorRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.green:
                        sketchView.setPenColor("#00ff00");
                        break;
                    case R.id.red:
                        sketchView.setPenColor("#ff0000");
                        break;
                }
            }
        });
    }

}
