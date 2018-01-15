package com.software.beacon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SplashScreen extends Activity {
    private static int SPLASH_TIME_OUT = 1500;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        session = new SessionManager(getApplicationContext());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean login = session.checkLogin();
                Log.e("login",login+"");
                if(login){
                Intent i = new Intent(SplashScreen.this, Home.class);
                startActivity(i);}
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}