package com.example.meloch.ui.activity;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meloch.R;

/**
 * This is a test activity to verify the app icon.
 * It's not meant to be used in the final app.
 */
public class IconTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_test);

        ImageView iconView = findViewById(R.id.appIconView);
        TextView iconSourceText = findViewById(R.id.iconSourceText);

        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            Drawable icon = getPackageManager().getApplicationIcon(applicationInfo);
            iconView.setImageDrawable(icon);
            iconSourceText.setText("Icon loaded successfully");
        } catch (PackageManager.NameNotFoundException e) {
            iconSourceText.setText("Error loading icon: " + e.getMessage());
        }
    }
}
