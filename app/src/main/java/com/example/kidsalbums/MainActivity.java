package com.example.kidsalbums;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import android.animation.Animator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.kidsalbums.LoginAPIConnector.LoginAPIRequestAsyncTask;
import com.example.kidsalbums.Modules.Kindergarten;
import com.example.kidsalbums.Modules.User;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int REQUEST_NEW_PASSWORD = 0;
    private ImageView bookIconImageView;
    private TextView bookITextView;
    private ProgressBar loadingProgressBar;
    private RelativeLayout rootView, afterAnimationView;
    private android.support.design.widget.TextInputEditText _emailText;
    private android.support.design.widget.TextInputEditText _passwordText;
    private Button _loginButton;
    private Button _forgotPasswordButton;
    public static User current_user;
    HashMap<String,String> userInfo;
    SharedPreferences sp;
    //SharedPreferences spUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        initViews();

        //sp.edit().putBoolean("logged",false).apply();

        //this code block enables to keep a user logged in
        if(sp.getBoolean("logged",false)){
            if(current_user == null){
                current_user = new User();

            }
            startUserActivity();
        }

        new CountDownTimer(5000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                bookITextView.setVisibility(GONE);
                loadingProgressBar.setVisibility(GONE);
                rootView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorSplashText));
                bookIconImageView.setImageResource(R.drawable.background_color_book);
                startAnimation();
            }

            @Override
            public void onFinish() {

            }
        }.start();

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        // login authentication logic
        if (authenticateUser(email, password)){
            //when login was successful we save "true" value into SharedPreferences instance (sp)
            //this allows us to implement 'keep user logged in' logic
            sp.edit().putBoolean("logged",true).apply();
            startUserActivity();

        }
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onLoginSuccess or onLoginFailed
                        onLoginSuccess();
                        // onLoginFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);
    }

    public void startUserActivity(){
        //we send the data from this activity (login) to user activity using intent
        Intent intent = initIntentForUserActivity();
        //successfull login -> go to user activity
        startActivity(intent);
    }



    /* This method sends a request with the email and password from user input into Ruslan's API (Node.js server)
     * If the email and password exists in the system, the API will send a response with status "true" and user data
     * If the email and password doesn't exist in the system, the API will send a response with status "false" */

    public boolean authenticateUser(String email, String password)  {
        //get user from Ruslan's API
        try{
            /* initialized email and password of a specific parent for debugging purposes */
            //String email = "aba.yonatan@gmail.com";
            //String password = "Wnxaukd9Zx";

            userInfo = new LoginAPIRequestAsyncTask().execute(email,password).get();
            setUserSharedPreferences();
            return true;
        }catch (Exception e){
            System.out.print(e.getMessage());
        }
        return false;
    }


    /* This method saves data of current user into a SharedPreferences instance (spUser)
     * It allows to save the user data for future launches, when the user exits the app and opens it again*/

    public void setUserSharedPreferences(){
        sp.edit().putString("id",userInfo.get("id")).apply();
        sp.edit().putString("name",userInfo.get("name")).apply();
        sp.edit().putString("email",userInfo.get("email")).apply();
        sp.edit().putString("phone",userInfo.get("phone")).apply();
        sp.edit().putString("childId",userInfo.get("childId")).apply();
        sp.edit().putString("childName",userInfo.get("childName")).apply();
        sp.edit().putString("childTag",userInfo.get("childTag")).apply();
        sp.edit().putString("childBirthday",userInfo.get("childBirthday")).apply();
        sp.edit().putString("kindergartenId",userInfo.get("kindergartenId")).apply();
        sp.edit().putString("kindergartenName",userInfo.get("kindergartenName")).apply();
        sp.edit().putString("kindergartenAddress",userInfo.get("kindergartenAddress")).apply();
        sp.edit().putString("kindergartenEmail",userInfo.get("kindergartenEmail")).apply();
        sp.edit().putString("kindergartenLatitude",userInfo.get("kindergartenLatitude")).apply();
        sp.edit().putString("kindergartenLongitude",userInfo.get("kindergartenLongitude")).apply();
//        spUser.edit().putString("id",userInfo.get("id")).apply();
//        spUser.edit().putString("name",userInfo.get("name")).apply();
//        spUser.edit().putString("email",userInfo.get("email")).apply();
//        spUser.edit().putString("phone",userInfo.get("phone")).apply();
//        spUser.edit().putString("childId",userInfo.get("childId")).apply();
//        spUser.edit().putString("childName",userInfo.get("childName")).apply();
//        spUser.edit().putString("childTag",userInfo.get("childTag")).apply();
//        spUser.edit().putString("childBirthday",userInfo.get("childBirthday")).apply();
//        spUser.edit().putString("kindergartenId",userInfo.get("kindergartenId")).apply();
//        spUser.edit().putString("kindergartenName",userInfo.get("kindergartenName")).apply();
//        spUser.edit().putString("kindergartenAddress",userInfo.get("kindergartenAddress")).apply();
//        spUser.edit().putString("kindergartenEmail",userInfo.get("kindergartenEmail")).apply();
//        spUser.edit().putString("kindergartenLatitude",userInfo.get("kindergartenLatitude")).apply();
//        spUser.edit().putString("kindergartenLongitude",userInfo.get("kindergartenLongitude")).apply();

    }

    public Intent initIntentForUserActivity(){
        Intent intent = new Intent(this, UserActivity.class);
        intent.putExtra("id",sp.getString("id",""));
        intent.putExtra("name",sp.getString("name",""));
        intent.putExtra("email",sp.getString("email",""));
        intent.putExtra("phone",sp.getString("phone",""));
        intent.putExtra("childId",sp.getString("childId",""));
        intent.putExtra("childName",sp.getString("childName",""));
        intent.putExtra("childTag",sp.getString("childTag",""));
        intent.putExtra("childBirthday",sp.getString("childBirthday",""));
        intent.putExtra("kindergartenId",sp.getString("kindergartenId",""));
        intent.putExtra("kindergartenName",sp.getString("kindergartenName",""));
        intent.putExtra("kindergartenAddress",sp.getString("kindergartenAddress",""));
        intent.putExtra("kindergartenEmail",sp.getString("kindergartenEmail",""));
        intent.putExtra("kindergartenLatitude",sp.getString("kindergartenLatitude",""));
        intent.putExtra("kindergartenLongitude",sp.getString("kindergartenLongitude",""));
//        intent.putExtra("id",spUser.getString("id",""));
//        intent.putExtra("name",spUser.getString("name",""));
//        intent.putExtra("email",spUser.getString("email",""));
//        intent.putExtra("phone",spUser.getString("phone",""));
//        intent.putExtra("childId",spUser.getString("childId",""));
//        intent.putExtra("childName",spUser.getString("childName",""));
//        intent.putExtra("childTag",spUser.getString("childTag",""));
//        intent.putExtra("childBirthday",spUser.getString("childBirthday",""));
//        intent.putExtra("kindergartenId",spUser.getString("kindergartenId",""));
//        intent.putExtra("kindergartenName",spUser.getString("kindergartenName",""));
//        intent.putExtra("kindergartenAddress",spUser.getString("kindergartenAddress",""));
//        intent.putExtra("kindergartenEmail",spUser.getString("kindergartenEmail",""));
//        intent.putExtra("kindergartenLatitude",spUser.getString("kindergartenLatitude",""));
//        intent.putExtra("kindergartenLongitude",spUser.getString("kindergartenLongitude",""));
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NEW_PASSWORD) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful name update logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }


    /* This method only validates that the email and password that the user input have a valid format */

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid id address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4) {
            _passwordText.setError("invalid input");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private void initViews() {
        bookIconImageView = findViewById(R.id.bookIconImageView);
        bookITextView = findViewById(R.id.bookITextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        rootView = findViewById(R.id.rootView);
        afterAnimationView = findViewById(R.id.afterAnimationView);
        _emailText = findViewById(R.id.emailEditText);
        _passwordText = findViewById(R.id.passwordEditText);
        _loginButton = findViewById(R.id.loginButton);
        _forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        sp = getSharedPreferences("CurrentSession",MODE_PRIVATE);
        //spUser = getSharedPreferences("user",MODE_PRIVATE);
    }

    private void startAnimation() {
        ViewPropertyAnimator viewPropertyAnimator = bookIconImageView.animate();
        viewPropertyAnimator.x(50f);
        viewPropertyAnimator.y(100f);
        viewPropertyAnimator.setDuration(1000);
        viewPropertyAnimator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                afterAnimationView.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}
