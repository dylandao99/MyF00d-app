package com.x10host.httpmyf00d.myf00d;

//Register User Screen - user enters information and registers information in database

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class registerUser extends AppCompatActivity {

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    String email, password, confPassword, fName;

    EditText Email, Password, ConfPassword, FName;

    AlertDialog alertDialog;

    ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get saved user credentials
        sharedPref = getSharedPreferences(getPackageName() + ".saved_user", MODE_PRIVATE);

        //create loading spinner
        spinner = (ProgressBar) findViewById(R.id.login_progressSpinner);
        spinner.isIndeterminate();
        spinner.setVisibility(View.GONE);

        //create universal alert dialog
        alertDialog = new AlertDialog.Builder(registerUser.this).create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

    }

    //ensure user registration info is valid
    protected boolean validateInput (){

        //get EditText Views
        Email = (EditText)findViewById(R.id.registerUser_et_email);
        Password = (EditText)findViewById(R.id.registerUser_et_password);
        ConfPassword = (EditText)findViewById(R.id.registerUser_et_confPassword);
        FName = (EditText)findViewById(R.id.registerUser_et_fName);

        //get user input fron EditTexts
        email = Email.getText().toString();
        password = Password.getText().toString();
        confPassword = ConfPassword.getText().toString();
        fName = FName.getText().toString();

        if (email.equals("") ||
                password.equals("") ||
                confPassword.equals("") ||
                fName.equals("")) { //missing fields
            alertDialog.setTitle("Missing Fields");
            alertDialog.setMessage("One or more required fields are empty!");
            alertDialog.show();
            return false;
        }
        if (password.length()<6){ //password is less than six characters
            Password.setText("");
            ConfPassword.setText("");
            alertDialog.setTitle("Password Invalid");
            alertDialog.setMessage("Password must be at least 6 characters!");
            alertDialog.show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){ //email is valid
            Email.setText("");
            Password.setText("");
            ConfPassword.setText("");
            alertDialog.setTitle("Email Invalid");
            alertDialog.setMessage("Email is not valid. Did you enter it correctly?");
            alertDialog.show();
            return false;
        }
        if (!password.equals(confPassword)){//confirmation password does not match
            Password.setText("");
            ConfPassword.setText("");
            alertDialog.setTitle("Passwords do not match");
            alertDialog.setMessage("The password and confirmation password entries do not match!");
            alertDialog.show();
            return false;
        }

        return true;
    }

    public void register (View view) throws IOException{
        if (validateInput()){ //if user credentials entered are valid

            //show loading spinner
            spinner.setVisibility(View.VISIBLE);

            //disable register button
            Button reg = (Button)findViewById(R.id.registerUser_btn_registerUser);
            reg.setEnabled(false);

            //prepare query to send to server
            String Url = "http://www.myf00d.x10host.com/add_user.php";
            String charset = "UTF-8";

            String query = String.format("email=%s&password=%s&first_name=%s&appPackage=%s",
                    URLEncoder.encode(email, charset),
                    URLEncoder.encode(password, charset),
                    URLEncoder.encode(fName, charset),
                    getPackageName());

            //try executing query on server
            new registerTask().execute (Url, charset, query);
        }
    }

    //uses query from register method, connects to online server, and registers user
    private class registerTask extends AsyncTask <String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
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

                while (inStream.hasNextLine())
                    response += (inStream.nextLine());

                return response;

            } catch (IOException e){
                return "connFailed";
            }
        }

        protected void onPostExecute(String s) {
            if (s.equals("400")) { //email already registered
                Email.setText("");
                Password.setText("");
                ConfPassword.setText("");
                alertDialog.setTitle("Registration Failed");
                alertDialog.setMessage("Entered email is already registered!");
                alertDialog.show();
            } else if (s.equals("connFailed")) { //connection failed
                alertDialog.setTitle("Registration Failed");
                alertDialog.setMessage("Connection Timeout. Verify internet connection!");
                alertDialog.show();
            } else { //save user details locally
                    editor = sharedPref.edit();
                    editor.putString("email", email);
                    editor.putString("password", password);
                    editor.putString("fName", fName);
                    editor.apply();
                    Intent i = new Intent (getApplicationContext(), regJoinHousehold.class);
                    startActivity(i);
            }

            //turn off loading spinner
            spinner.setVisibility(View.GONE);

            //re-enable button
            Button reg = (Button) findViewById(R.id.registerUser_btn_registerUser);
            reg.setEnabled(true);
        }
    }
}