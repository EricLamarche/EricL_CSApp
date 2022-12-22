package com.app.ericl_csapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.app.ericl_csapp.databinding.ActivityMainBinding;
import com.app.ericl_csapp.databinding.ActivitySplashScreenBinding;

import java.util.ArrayList;

public class SplashScreen extends AppCompatActivity {
    ActivitySplashScreenBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            ArrayList<Course> requiredCourses = extras.getParcelableArrayList("requiredCourses");
            boolean isWelcomeBack = extras.getBoolean("isWelcomeBack");
            String userName = extras.getString("username");

            if (isWelcomeBack){
                binding.txtWelcome.setText("Welcome Back " + userName);
            } else {
                binding.txtWelcome.setText("Welcome " + userName);
            }

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Write whatever to want to do after delay specified (1 sec)
                    //Log.d("Handler", "Running Handler");

                    Intent intent = new Intent(SplashScreen.this, Advisor.class);
                    intent.putParcelableArrayListExtra("requiredCourses", requiredCourses);
                    startActivity(intent);
                    finish();

                }
            }, 2000);

        }

    }
}