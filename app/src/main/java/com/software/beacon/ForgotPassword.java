package com.software.beacon;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class ForgotPassword extends AppCompatActivity implements ValidationResponse {

    private PopupWindow mPopup;
    TextInputLayout newPassword;
    TextInputLayout confirmNewPassword;
    TextInputLayout mobile;
    Button submit;
    String mobileNumber;
    boolean once = true;
    boolean doubleBackPressed = false;
    TextView head;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mobile = (TextInputLayout) findViewById(R.id.mobile_num);
        submit = (Button) findViewById(R.id.submit_button);
        head = (TextView) findViewById(R.id.head);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (once) confirm();
                else reset();
            }
        });
    }

    TextInputLayout createTextInputLayout(String hint, int index) {
        LinearLayout forgotPasswordForm = (LinearLayout) findViewById(R.id.forgot_password_form);
        TextInputLayout textInputLayout = new TextInputLayout(ForgotPassword.this);
        LinearLayout.LayoutParams textInputLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textInputLayout.setLayoutParams(textInputLayoutParams);
        EditText editText = new EditText(this);
        LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(editTextParams);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editText.setHint(hint);
        textInputLayout.addView(editText, editTextParams);
        forgotPasswordForm.addView(textInputLayout, index);
        return textInputLayout;
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
            String response = null;
            try {
                response = jObject.getString("response");
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
                if (error_no != 0)
                    Snackbar.make(findViewById(R.id.forgot_password_form), Html.fromHtml("<b> Error !!</b>"), Snackbar.LENGTH_INDEFINITE).show();
            } else {
                boolean exists = false;
                try {
                    exists = jObject.getBoolean("exists");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (response.equals("find")) {
                    if (exists) {
                        if (once) {
                            mobile.setVisibility(View.GONE);
                            head.setText("Change Password");
                            newPassword = createTextInputLayout("New Password", 1);
                            confirmNewPassword = createTextInputLayout("Confirm Password", 2);
                            once = false;
                            doubleBackPressed = false;
                        }
                    } else
                        mobile.setError("The mobile number is not registered");
                } else {
                    setResult(RESULT_OK,null);
                    Intent i = new Intent(ForgotPassword.this, LoginActivity.class);
                    startActivity(i);
                    finish();
                    Snackbar.make(findViewById(R.id.forgot_password_form), Html.fromHtml("<b> Updated !!</b>"), Snackbar.LENGTH_INDEFINITE).show();
                }
            }
        }
    }

    void confirm() {
        if (!validate(1)) {
            return;
        }
        mobileNumber = mobile.getEditText().getText().toString();
        Get_Result conn = new Get_Result(this);
        conn.delegate = ForgotPassword.this;
        showProgress(true);
        conn.execute(URLS.SignUp_URL, "find=" + mobileNumber);
    }

    boolean validate(int howMany) {
        boolean valid = true;
        if (howMany == 1) {
            String mobileNum = mobile.getEditText().getText().toString();
            if (mobileNum.isEmpty() || mobileNum.length() != 10) {
                mobile.setError("Enter Valid Mobile Number");
                valid = false;
            } else {
                mobile.setError(null);
            }
        } else {
            String password = newPassword.getEditText().getText().toString();
            String reEnterPassword = confirmNewPassword.getEditText().getText().toString();
            if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
                newPassword.setError("Password must be between 4 and 10");
                valid = false;
            } else {
                newPassword.setError(null);
            }

            if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
                confirmNewPassword.setError("Password Do not match");
                valid = false;
            } else {
                confirmNewPassword.setError(null);
            }
        }
        return valid;
    }

    void reset() {
        if (!validate(2)) {
            return;
        }

        String password = newPassword.getEditText().getText().toString();
        String query = "update users set password='" + password + "' where mobile='" + mobileNumber + "';";
        Get_Result conn = new Get_Result(this);
        conn.delegate = ForgotPassword.this;
        showProgress(true);
        conn.execute(URLS.SignUp_URL, query);
    }

    public void showProgress(final boolean show) {
        if (show) {
            View popupView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.my_progress, null);
            Point size = new Point();
            this.getWindowManager().getDefaultDisplay().getSize(size);
            int width = size.x;
            int height = size.y;
            mPopup = new PopupWindow(popupView, width, height);
            mPopup.showAtLocation(findViewById(R.id.forgot_password_form), Gravity.CENTER, 0, 0);
            mPopup.setFocusable(true);
        } else
            mPopup.dismiss();
    }

    @Override
    public void onBackPressed() {
        if(!once) {
            if (doubleBackPressed) {
                super.onBackPressed();
                return;
            }
        }
        else{
            super.onBackPressed();
            return;
        }

        if(!once) {
            mobile.setVisibility(View.VISIBLE);
            head.setText("Forgot Password?");
            newPassword.setVisibility(View.GONE);
            confirmNewPassword.setVisibility(View.GONE);
            once = true;
        }

        doubleBackPressed = true;
    }
}
