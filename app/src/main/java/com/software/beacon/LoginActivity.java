package com.software.beacon;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import static com.software.beacon.R.id.mobile;
import static com.software.beacon.R.layout.activity_login;

public class LoginActivity extends AppCompatActivity implements ValidationResponse, TextWatcher {

    private TextInputLayout mMobileView;
    private TextInputLayout mPasswordView;
    private InputMethodManager inputManager;
    private PopupWindow mPopup;
    private SessionManager session;
    private TextView _signupLink;
    private TextView forgotPassword;
    private static final int REQUEST_RESET_PASSWORD = 0;
    private static final int REQUEST_SIGNUP = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_login);

        // Set up the login form.
        session = new SessionManager(getApplicationContext());
        mMobileView = (TextInputLayout) findViewById(mobile);
        mMobileView.clearFocus();
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        _signupLink = (TextView) findViewById(R.id.link_signup);
        mPasswordView = (TextInputLayout) findViewById(R.id.password);
        forgotPassword = (TextView) findViewById(R.id.forgot_password);

        mMobileView.getEditText().addTextChangedListener(this);
        mPasswordView.getEditText().addTextChangedListener(this);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                attemptLogin();
            }
        });

        _signupLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent,REQUEST_SIGNUP);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        forgotPassword.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ForgotPassword.class);
                startActivityForResult(intent, REQUEST_RESET_PASSWORD);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESET_PASSWORD) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

        private void attemptLogin () {

            mMobileView.setError(null);
            mPasswordView.setError(null);

            // Store values at the time of the login attempt.
            String mobile = mMobileView.getEditText().getText().toString();
            String password = mPasswordView.getEditText().getText().toString();

            boolean cancel = false;
            View focusView = null;

            if (TextUtils.isEmpty(mobile)) {
                mMobileView.setError("This field cannot be empty");
                focusView = mMobileView;
                cancel = true;
            } else if (!isMobileValid(mobile)) {
                mMobileView.setError("Enter Valid Mobile Number");
                focusView = mMobileView;
                cancel = true;
            }

            if (TextUtils.isEmpty(password)) {
                mPasswordView.setError("This field cannot be empty");
                focusView = mMobileView;
                cancel = true;
            }

            if (cancel) {
                focusView.requestFocus();
            } else {
                String query = "select * from users where mobile = '" + mobile + "' and password= '" + password + "';";
                Get_Result conn = new Get_Result(this);
                conn.delegate = LoginActivity.this;
                showProgress(true);
                conn.execute(URLS.Login_URL, query);
            }
        }

        private boolean isMobileValid (String mobile){
            return mobile.length() != 10 ? false : true;
        }

        public void showProgress ( final boolean show){
            View popupView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.my_progress, null);
            if (show) {
                Point size = new Point();
                this.getWindowManager().getDefaultDisplay().getSize(size);
                int width = size.x;
                int height = size.y;
                mPopup = new PopupWindow(popupView, width, height);
                mPopup.showAtLocation(findViewById(R.id.email_login_form), Gravity.CENTER, 0, 0);
            } else
                mPopup.dismiss();
        }

        @Override
        public void onBackPressed () {
            finish();
        }

        @Override
        public void response ( boolean result, String s){
            showProgress(false);
            if (result) {
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                boolean error = false;
                try {
                    error = jObject.getBoolean("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (error) {
                    Snackbar.make(findViewById(R.id.email_login_form), Html.fromHtml("<b> Error !!</b>"), Snackbar.LENGTH_INDEFINITE).show();
                } else {
                    boolean login = false;
                    try {
                        login = jObject.getBoolean("login");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (login) {
                        JSONObject user;
                        String name = null;
                        String mobile = null;
                        String pass = null;
                        try {
                            user = jObject.getJSONObject("user");
                            name = user.getString("name");
                            mobile = user.getString("mobile");
                            pass = user.getString("password");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        session.createLoginSession(name, mobile, pass);
                        Intent intent = new Intent(LoginActivity.this, Home.class);
                        startActivity(intent);
                        finish();
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
                        alertDialog.setTitle("Login failed..")
                                .setMessage("Username/Password is incorrect")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                        AlertDialog alert = alertDialog.create();
                        alert.show();
                    }
                }
            } else
                Snackbar.make(findViewById(R.id.email_login_form), Html.fromHtml("<b> "+s+" </b>"), Snackbar.LENGTH_INDEFINITE).show();
        }

        @Override
        public void beforeTextChanged (CharSequence charSequence,int i, int i1, int i2){

        }

        @Override
        public void onTextChanged (CharSequence charSequence,int i, int i1, int i2){

        }

        @Override
        public void afterTextChanged (Editable editable){
            validateText(editable);
        }

        private void validateText (Editable editable){
            String mobile = mMobileView.getEditText().getText().toString();
            String password = mPasswordView.getEditText().getText().toString();

            if (editable == mMobileView.getEditText().getEditableText()) {
                if (mobile.isEmpty() || mobile.length() != 10) {
                    mMobileView.setError("Enter Valid Mobile Number");
                } else {
                    mMobileView.setError(null);
                }
            } else if (editable == mPasswordView.getEditText().getEditableText()) {
                if (password.isEmpty()) {
                    mPasswordView.setError("This field cannot be empty");
                } else {
                    mPasswordView.setError(null);
                }
            }
        }
    }

