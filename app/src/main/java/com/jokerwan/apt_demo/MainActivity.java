package com.jokerwan.apt_demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.jokerwan.annotation.JRouter;

@JRouter(path = "/app/MainActivity")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
