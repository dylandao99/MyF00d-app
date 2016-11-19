package com.x10host.httpmyf00d.myf00d;

//Login User Screen - User enters credentials to login
//Verifies login info, checks login info online
//if there is a match, login user

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class login extends AppCompatActivity {

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    String email, password;
    EditText Email, Password;

    ProgressBar spinner;

    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get saved user details
        sharedPref = getSharedPreferences(getPackageName() + ".saved_user", MODE_PRIVATE);
        //get saved food database
        SharedPreferences sharedPrefFood = getSharedPreferences(getPackageName() + ".food_database", MODE_PRIVATE);

        //if food database is not empty, redirect to main activity
        if (sharedPrefFood.getString("food_database",null) != null){
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }

        //create login wait spinner
        spinner = (ProgressBar) findViewById(R.id.login_progressSpinner);
        spinner.isIndeterminate();
        spinner.setVisibility(View.GONE);

       //create login error dialog
        alertDialog = new AlertDialog.Builder(login.this).create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        //re-enable buttons
        Button login = (Button) findViewById(R.id.login_btn_login);
        Button reg = (Button) findViewById(R.id.login_btn_register);
        login.setEnabled(true);
        reg.setEnabled(true);
    }

    protected boolean validateInput (){

       //get email and password EditText Views
        Email = (EditText)findViewById(R.id.login_et_email);
        Password = (EditText)findViewById(R.id.login_et_password);

        //get user input and convert to string
        email = Email.getText().toString();
        password = Password.getText().toString();

        //VALIDATE LOGIN CREDENTIALS
        //check if one of the fields are empty
        if (email.equals("") ||
                password.equals("")){
            alertDialog.setTitle("Missing Fields");
            alertDialog.setMessage("One or more required fields are empty!");
            alertDialog.show();
            return false;
        }
        //check if password is at least 6 characters
        if (password.length()<6){
            Password.setText("");
            alertDialog.setTitle("Password Invalid");
            alertDialog.setMessage("Password must be at least 6 characters!");
            alertDialog.show();
            return false;
        }
        //check if email is a valid email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Email.setText("");
            Password.setText("");
            alertDialog.setTitle("Email Invalid");
            alertDialog.setMessage("Email is not valid. Did you enter it correctly?");
            alertDialog.show();
            return false;
        }

        return true;
    }

    public void userLogin (View view) throws IOException {

        //if login credentials are valid
        if (validateInput()) {
            //turn on loading spinner
            spinner.setVisibility(View.VISIBLE);

            //disable buttons
            Button login = (Button) findViewById(R.id.login_btn_login);
            Button reg = (Button) findViewById(R.id.login_btn_register);
            login.setEnabled(false);
            reg.setEnabled(false);

            //prepare variables to send to server
            String Url = "http://www.myf00d.x10host.com/login_user.php";
            String charset = "UTF-8";

            String query = String.format("email=%s&password=%s&appPackage=%s",
                    URLEncoder.encode(email, charset),
                    URLEncoder.encode(password, charset),
                    (getPackageName()));

            //send login query to server
            new loginTask().execute(Url, charset, query);
        }
    }

    //on Register User button pressed, goes to register user screen
    public void goto_registerUser (View view){
        Button login = (Button) findViewById(R.id.login_btn_login);
        Button reg = (Button) findViewById(R.id.login_btn_register);
        login.setEnabled(false);
        reg.setEnabled(false);
        Intent i = new Intent(getApplicationContext(), registerUser.class);
        startActivity(i);
    }

    private class loginTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... data){

            //hold server response
            String response = "";

            try {
                //prepare connection
                URL url = new URL(data[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Accept-Charset", data[1]);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + data[1]);

                conn.setConnectTimeout(5000);

                //get response from server
                try (OutputStream output = conn.getOutputStream()) {
                    output.write(data[2].getBytes(data[1]));
                }

                Scanner inStream = new Scanner(conn.getInputStream());

                while(inStream.hasNextLine())
                    response+=(inStream.nextLine());

                //save login details locally
                try {
                    JSONObject user_data = new JSONObject(response);
                    editor = sharedPref.edit();
                    editor.putString("email", user_data.getString("email"));
                    editor.putString ("password", user_data.getString("password"));
                    editor.putString ("fName", user_data.getString("fName"));
                    editor.putString ("hID", user_data.getString("hID"));
                    editor.apply();
                } catch (Exception e){
                    System.out.println ("Error parsing json array");
                }

                //pass server response to postExecute
                return response;

            } catch (IOException e) {
                return "connFailed";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (s.equals("400")) { //login credentials not in user database
                Email.setText("");
                Password.setText("");
                alertDialog.setTitle("Login Failed");
                alertDialog.setMessage("Username/Password invalid. Are you sure you entered them correctly?");
                alertDialog.show();
            } else if (s.equals("connFailed")) { //connection to server failed
                alertDialog.setTitle("Login Failed");
                alertDialog.setMessage("Connection Timeout. Verify internet connection!");
                alertDialog.show();
            } else if (sharedPref.getString("hID", "0").equals("null")){ //if no registered household, redirect user to make/join one
                Intent i = new Intent(getApplicationContext(), regJoinHousehold.class);
                startActivity(i);
            } else { //go to main food screen
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }
            //turn off loading spinner
            spinner.setVisibility(View.GONE);

            //re-enable buttons
            Button login = (Button) findViewById(R.id.login_btn_login);
            Button reg = (Button) findViewById(R.id.login_btn_register);
            login.setEnabled(true);
            reg.setEnabled(true);
        }
    }
}
