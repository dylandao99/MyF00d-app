package com.x10host.httpmyf00d.myf00d;

//Register/Join Household Screen - user registers or joins a household
//sends household info to online PHP script which registers/joins user to household in online DB
//a household is a central hub in which users can join
//food is associated to households, users associated to a household can access all foods associated to said household

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

public class regJoinHousehold extends AppCompatActivity {

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    EditText Code, Password;

    String code, password;

    ProgressBar spinner;

    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg_join_household);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get saved user data
        sharedPref = getSharedPreferences(getPackageName() + ".saved_user", MODE_PRIVATE);

        //create loading spinner
        spinner = (ProgressBar) findViewById(R.id.login_progressSpinner);
        spinner.isIndeterminate();
        spinner.setVisibility(View.GONE);

        //create universal alert dialog
        alertDialog = new AlertDialog.Builder(regJoinHousehold.this).create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

    }

    @Override
    public void onBackPressed(){
        //do nothing
    }

    protected boolean validateInput (){ //validate household registration info

        //get EditTexts
        Code = (EditText)findViewById(R.id.regJoinHousehold_et_code);
        Password = (EditText)findViewById(R.id.regJoinHousehold_et_password);

        //put user input to string
        code = Code.getText().toString();
        password = Password.getText().toString();

        //VALIDATE REGISTRATION INFO
        if (code.equals("") ||
                password.equals("")){ //missing fields
            alertDialog.setTitle("Missing Fields");
            alertDialog.setMessage("One or more required fields are empty!");
            alertDialog.show();
            return false;
        }
        if (password.length()<6){ //password less than 6 characters
            Password.setText("");
            alertDialog.setTitle("Password Invalid");
            alertDialog.setMessage("Password must be at least 6 characters!");
            alertDialog.show();
            return false;
        }

        return true;
    }

    //prepares query to register user and tries connection to server in another thread
    public void regJoinHouseholdTask (View view) throws IOException{

        if (validateInput()) { //if information is valid

            //show loading spinner
            spinner.setVisibility(View.VISIBLE);

            //disable buttons
            Button reg = (Button)findViewById(R.id.regJoinHousehold_btn_reg);
            Button join = (Button)findViewById(R.id.regJoinHousehold_btn_join);
            reg.setEnabled(false);
            join.setEnabled(false);

            //prepare query
            String Url = "http://www.myf00d.x10host.com/join_household.php";
            String charset = "UTF-8";

            //tell PHP script whether to register or join user to an existing household
            String regJoin = (view.getId() == R.id.regJoinHousehold_btn_reg) ? "true" : "false";

            String query = String.format("code=%s&password=%s&email=%s&register=%s&appPackage=%s",
                    URLEncoder.encode(code, charset),
                    URLEncoder.encode(password, charset),
                    URLEncoder.encode(sharedPref.getString("email", "null"), charset),
                    URLEncoder.encode(regJoin, charset),
                    getPackageName());

            new regJoinTask().execute(Url, charset, query);
        }
    }

    //query sent to server, user registers/joins household
    private class regJoinTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... data){
            String response = "";
            try {
                //prepare connection
                URL url = new URL(data[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Accept-Charset", data[1]);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + data[1]);

                //returns connection failure after 5000ms
                conn.setConnectTimeout(5000);

                try (OutputStream output = conn.getOutputStream()) {
                    output.write(data[2].getBytes(data[1]));
                }

                //get response from server
                Scanner inStream = new Scanner(conn.getInputStream());

                while(inStream.hasNextLine())
                    response+=(inStream.nextLine());

                return response;

            } catch (IOException e){
                return "connFailed";
            }
        }

        //get response from regJoinTask method and do something based upon it
        @Override
        protected void onPostExecute(String s) {
            if (s.equals ("400")) { //household code already registered
                Code.setText("");
                Password.setText("");
                alertDialog.setTitle("Household Registration Failed");
                alertDialog.setMessage("Household code is already in use. Try another one!");
                alertDialog.show();
            } else if (s.equals ("401")) { //household does not exist, cannot join
                Code.setText("");
                Password.setText("");
                alertDialog.setTitle("Household Join Failed");
                alertDialog.setMessage("Household does not exist. Try another one!");
                alertDialog.show();
            } else if (s.equals("connFailed")) { //conneciton failed
                alertDialog.setTitle("Registration/Join Failed");
                alertDialog.setMessage("Connection Timeout. Verify internet connection!");
                alertDialog.show();
            } else { //go to main screen
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }

            //hide loading spinner
            spinner.setVisibility(View.GONE);

            //re-enable buttons
            Button reg = (Button)findViewById(R.id.regJoinHousehold_btn_reg);
            Button join = (Button)findViewById(R.id.regJoinHousehold_btn_join);
            reg.setEnabled(true);
            join.setEnabled(true);
        }
    }
}
