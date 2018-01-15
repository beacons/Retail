package com.software.beacon;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class SignupActivity extends AppCompatActivity implements ValidationResponse, TextWatcher {
    private static final String TAG = "SignupActivity";

    TextInputLayout _nameText;
    TextInputLayout _mobileText;
    TextInputLayout _passwordText;
    TextInputLayout _reEnterPasswordText;
    Button _signupButton;
    TextView _loginLink;
    private PopupWindow mPopup;
    private LinearLayout _signup_form;
    private SessionManager session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        session = new SessionManager(getApplicationContext());

        _nameText = (TextInputLayout) findViewById(R.id.input_name);
        _mobileText = (TextInputLayout) findViewById(R.id.input_mobile);
        _passwordText = (TextInputLayout) findViewById(R.id.input_password);
        _reEnterPasswordText = (TextInputLayout) findViewById(R.id.input_reEnterPassword);
        _signupButton = (Button) findViewById(R.id.btn_signup);
        _loginLink = (TextView) findViewById(R.id.link_login);
        _signup_form = (LinearLayout) findViewById(R.id.signup_form);

        _nameText.getEditText().addTextChangedListener(this);
        _mobileText.getEditText().addTextChangedListener(this);
        _passwordText.getEditText().addTextChangedListener(this);
        _reEnterPasswordText.getEditText().addTextChangedListener(this);

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });
        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");
        if (!validate()) {
            return;
        }
        String name = _nameText.getEditText().getText().toString();
        String mobile = _mobileText.getEditText().getText().toString();
        String password = _passwordText.getEditText().getText().toString();
        name=name.substring(0,1).toUpperCase()+name.substring(1);
        Log.e("name",name);
        String query = "insert into users values('" + name + "','" + mobile + "','" + password + "');";

        Get_Result conn = new Get_Result(this);
        conn.delegate = SignupActivity.this;
        showProgress(true);
        conn.execute(URLS.SignUp_URL, query);
    }

    @Override
    public void response(boolean result, String s) {
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
                int error_no = 0;
                try {
                    error_no = jObject.getInt("error_no");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (error_no == 1062) {
                    _mobileText.setError("Mobile Number Already Registered ");
                } else
                    Snackbar.make(_signup_form, Html.fromHtml("<b> Error !!</b>"), Snackbar.LENGTH_INDEFINITE).show();
            } else
                onSignupSuccess();
        }
        else
            Snackbar.make(_signup_form, Html.fromHtml("<b> Connection Error. Please Try Again! </b>"), Snackbar.LENGTH_INDEFINITE).show();
    }

    public void onSignupSuccess() {
        String name = _nameText.getEditText().getText().toString();
        name=name.substring(0,1).toUpperCase()+name.substring(1);
        String mobile = _mobileText.getEditText().getText().toString();
        String password = _passwordText.getEditText().getText().toString();
        _signupButton.setEnabled(true);
        setResult(RESULT_OK, null);
        session.createLoginSession(name,mobile,password);
        setResult(RESULT_OK,null);
        Intent intent = new Intent(SignupActivity.this, Home.class);
        startActivity(intent);
        finish();
    }

    public boolean validate() {
        boolean valid = true;
        String name = _nameText.getEditText().getText().toString();
        String mobile = _mobileText.getEditText().getText().toString();
        String password = _passwordText.getEditText().getText().toString();
        String reEnterPassword = _reEnterPasswordText.getEditText().getText().toString();

        if (name.isEmpty()) {
            _nameText.setError("Name can't be blank");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (mobile.isEmpty() || mobile.length() != 10) {
            _mobileText.setError("Enter Valid Mobile Number");
            valid = false;
        } else {
            _mobileText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("Password must be between 4 and 10");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError("Password Do not match");
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }

        return valid;
    }

    public void showProgress(final boolean show) {
        if (show) {
            View popupView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.my_progress, null);
            Point size = new Point();
            this.getWindowManager().getDefaultDisplay().getSize(size);
            int width = size.x;
            int height = size.y;
            mPopup = new PopupWindow(popupView, width, height);
            mPopup.showAtLocation(_signup_form, Gravity.CENTER, 0, 0);
            mPopup.setFocusable(true);
        } else
            mPopup.dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPopup != null) {
            mPopup.dismiss();
            mPopup = null;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        validateText(editable);
    }

    private void validateText(Editable editable) {
        String name = _nameText.getEditText().getText().toString();
        String mobile = _mobileText.getEditText().getText().toString();
        String password = _passwordText.getEditText().getText().toString();
        String reEnterPassword = _reEnterPasswordText.getEditText().getText().toString();

        if (editable == _nameText.getEditText().getEditableText()) {
            if (name.isEmpty()) {
                _nameText.setError("Name can't be blank");
            } else {
                _nameText.setError(null);
            }
        } else if (editable == _mobileText.getEditText().getEditableText()) {
            if (mobile.isEmpty() || mobile.length() != 10) {
                _mobileText.setError("Enter Valid Mobile Number");

            } else {
                _mobileText.setError(null);
            }
        } else if (editable == _passwordText.getEditText().getEditableText()) {
            if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
                _passwordText.setError("Password must be between 4 and 10");
            } else {
                _passwordText.setError(null);
            }
        } else if (editable == _reEnterPasswordText.getEditText().getEditableText()) {
            if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
                _reEnterPasswordText.setError("Password Do not match");
            } else {
                _reEnterPasswordText.setError(null);
            }
        }
    }
}