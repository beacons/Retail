package com.software.beacon;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MyAccount extends AppCompatActivity implements ValidationResponse, TextWatcher {
    Toolbar mToolbar;
    SessionManager session;
    EditText name, password, mobile;
    boolean edit = true;
    MenuItem item;
    boolean changed = false;
    HashMap<String, String> user;
    PopupWindow mPopup;
    TextInputLayout mobileWrapper;
    boolean hasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        session = new SessionManager(getApplicationContext());
        name = (EditText) findViewById(R.id.print_name);
        mobile = (EditText) findViewById(R.id.print_mobile);
        password = (EditText) findViewById(R.id.print_password);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_my_account);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setText();
        disableEditText(name);
        disableEditText(mobile);
        disableEditText(password);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_account_action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                if (edit) {
                    edit = false;
                    item.setIcon(R.drawable.ic_done_white_24dp);
                    enableEditText(name);
                    name.requestFocus();
                    name.setSelection(name.getText().length());
                    enableEditText(mobile);
                    enableEditText(password);
                } else {
                    if (!changed) {
                        edit = true;
                        item.setIcon(R.mipmap.ic_mode_edit_white_24dp);
                        disableEditText(name);
                        disableEditText(mobile);
                        disableEditText(password);
                    } else
                        ask();
                }
                this.item = item;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void disableEditText(EditText editText) {
        editText.setFocusable(false);
        editText.setEnabled(false);
        //editText.setCursorVisible(false);
        //editText.setInputType(InputType.TYPE_NULL);
        //editText.setKeyListener(null);
        //editText.setBackgroundColor(Color.TRANSPARENT);
    }

    private void enableEditText(EditText editText) {
        editText.addTextChangedListener(this);
        editText.setFocusable(true);
        editText.setEnabled(true);
        editText.setFocusableInTouchMode(true);
        //editText.setCursorVisible(true);
        //editText.setKeyListener(new EditText(getApplicationContext()).getKeyListener());
        //editText.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onBackPressed() {
        if (!edit) {
            edit = true;
            if(hasError) {
                hasError = false;
                mobileWrapper.setError(null);
            }
            if(changed) {
                ask();
                changed = false;
            }
            else {
                item.setIcon(R.mipmap.ic_mode_edit_white_24dp);
                setText();
                disableEditText(name);
                disableEditText(mobile);
                disableEditText(password);
            }
        } else
            super.onBackPressed();
    }

    void ask() {
        if (changed) {
            new AlertDialog.Builder(this)
                    .setMessage("Do you really want to save changes?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            update();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (hasError) {
                                mobileWrapper.setError(null);
                                hasError = false;
                            }
                            item.setIcon(R.mipmap.ic_mode_edit_white_24dp);
                            setText();
                            disableEditText(name);
                            disableEditText(mobile);
                            disableEditText(password);
                            dialog.dismiss();
                            changed = false;
                        }
                    }).show();
        }
    }

    void update() {
        if (!validate()) {
            return;
        }
        String userName = name.getText().toString();
        userName=userName.substring(0,1).toUpperCase()+userName.substring(1);
        String userMobile = mobile.getText().toString();
        String userPassword = password.getText().toString();
        String query = "update users set name='" + userName + "',mobile='" + userMobile + "',password='" + userPassword + "' where mobile='" + user.get(session.KEY_MOB) + "';";
        Get_Result conn = new Get_Result(this);
        conn.delegate = MyAccount.this;
        showProgress(true);
        conn.execute(URLS.SignUp_URL, query);
    }

    private boolean validate() {
        boolean valid = true;
        TextInputLayout nameWrapper = (TextInputLayout) name.getParentForAccessibility();
        TextInputLayout mobileWrapper = (TextInputLayout) mobile.getParentForAccessibility();
        TextInputLayout passwordWrapper = (TextInputLayout) password.getParentForAccessibility();
        String userName = name.getText().toString();
        String userMobile = mobile.getText().toString();
        String userPassword = password.getText().toString();
        //String reEnterPassword = _reEnterPasswordText.getEditText().getText().toString();

        if (userName.isEmpty()) {
            nameWrapper.setError("Name can't be blank");
            valid = false;
        } else {
            nameWrapper.setError(null);
        }

        if (userMobile.isEmpty() || userMobile.length() != 10) {
            mobileWrapper.setError("Enter Valid Mobile Number");
            valid = false;
        } else {
            mobileWrapper.setError(null);
        }

        if (userPassword.isEmpty() || userPassword.length() < 4 || userPassword.length() > 10) {
            passwordWrapper.setError("Password must be between 4 and 10");
            valid = false;
        } else {
            passwordWrapper.setError(null);
        }

        /*if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(userPassword))) {
            _reEnterPasswordText.setError("Password Do not match");
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }*/

        return valid;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        changed = true;
    }

    void setText() {
        user = session.getUserDetails();
        name.setText(user.get(session.KEY_NAME));
        mobile.setText(user.get(session.KEY_MOB));
        password.setText(user.get(session.KEY_PASS));
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
                    mobileWrapper = (TextInputLayout) mobile.getParentForAccessibility();
                    mobileWrapper.setError("Mobile Number Already Registered ");
                    hasError = true;
                    enableEditText(mobile);
                    mobile.requestFocus();
                    changed = false;
                } else
                    Snackbar.make(findViewById(R.id.my_account), Html.fromHtml("<b> Error !!</b>"), Snackbar.LENGTH_INDEFINITE).show();
            } else
                onUpdateSuccess();
        } else
            Snackbar.make(findViewById(R.id.my_account), Html.fromHtml("<b> "+s+" </b>"), Snackbar.LENGTH_INDEFINITE).show();
    }

    public void showProgress(final boolean show) {
        View popupView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.my_progress, null);
        if (show) {
            Point size = new Point();
            this.getWindowManager().getDefaultDisplay().getSize(size);
            int width = size.x;
            int height = size.y;
            mPopup = new PopupWindow(popupView, width, height);
            mPopup.showAtLocation(findViewById(R.id.my_account), Gravity.CENTER, 0, 0);
        } else
            mPopup.dismiss();
    }


    public void onUpdateSuccess() {
        item.setIcon(R.mipmap.ic_mode_edit_white_24dp);
        edit = true;
        changed = false;
        disableEditText(name);
        disableEditText(mobile);
        disableEditText(password);
        String name = this.name.getText().toString();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        String mobile = this.mobile.getText().toString();
        String password = this.password.getText().toString();
        session.createLoginSession(name, mobile, password);
        Snackbar.make(findViewById(R.id.my_account), Html.fromHtml("<b> Updated !!</b>"), Snackbar.LENGTH_INDEFINITE).show();
    }
}
