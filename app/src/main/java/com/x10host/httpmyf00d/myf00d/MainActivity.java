package com.x10host.httpmyf00d.myf00d;

//Main Activity
//holds all connectivity methods for all post-login fragments and other misc. methods

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class MainActivity extends FragmentActivity {

    static SharedPreferences sharedPref;
    static SharedPreferences sharedPrefFood;
    SharedPreferences databaseAdditions;
    SharedPreferences databaseChanges;
    static SharedPreferences databaseDeletions;
    static SharedPreferences.Editor editor;

    static AlertDialog alertDialog;

    FragmentManager fm;

    static String Url, charset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get all local data
        sharedPref = getSharedPreferences(getPackageName() + ".saved_user", MODE_PRIVATE);
        sharedPrefFood = getSharedPreferences(getPackageName() + ".food_database", MODE_PRIVATE);
        databaseAdditions = getSharedPreferences(getPackageName() + ".database_additions", MODE_PRIVATE);
        databaseChanges = getSharedPreferences(getPackageName() + ".databaseChanges", MODE_PRIVATE);
        databaseDeletions = getSharedPreferences(getPackageName() + ".databaseDeletions", MODE_PRIVATE);

        //create universal alert dialog
        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        fm = getFragmentManager();

        //sync database changes
        try {
            onlineSync();
        } catch (IOException e){}

        //update local database
        updateDatabase();
        //show main menu
        displayMainMenu();
    }

    //prepares query with email and password to get updated database from server
    public void updateDatabase() {
        //prepare variables to send to server
        String Url = "http://www.myf00d.x10host.com/get_list.php";
        String charset = "UTF-8";
        String query = null;

        String email = sharedPref.getString("email", null);
        String password = sharedPref.getString("password", null);

        if (email != null && password != null) {
            try {
                query = String.format("email=%s&password=%s&appPackage=%s",
                        URLEncoder.encode(email, charset),
                        URLEncoder.encode(password, charset),
                        (getPackageName()));
            } catch (IOException e) {
                System.out.println("Failed to Encode Query");
            }
        }

        //try connecting on seperate thread
        new getDataTask().execute(Url, charset, query);
    }

    //show main menu list
    public void displayMainMenu(){

        main_mainMenu mainMenu = new main_mainMenu();

        fm.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .add(R.id.fragment_container, mainMenu, "mainmenu")
                .commit();
    }

    //addfood button onclick,goes to addfood screen
    public void goto_addFood(View view){
        Intent i = new Intent(getApplicationContext(), addFood.class);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {

        Fragment mainmenu = fm.findFragmentByTag("mainmenu");

        Fragment current = fm.findFragmentByTag("category");

        if (mainmenu.isVisible()){//if main menu is the current fragment
            //do nothing
        } else if (current != null && current.isVisible()){//if category was returned to from AllFood Screen
            //return to main menu on back pressed
            Fragment mm = new main_mainMenu();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.disallowAddToBackStack();
            ft.replace(R.id.fragment_container, mm, "mainmenu");
            ft.commit();
        } else {
            getFragmentManager().popBackStack();
        }

    }

    //functionality for + and - buttons on each list item
    public void quickQuantityChange(View view){

        String iden = view.getId() + "";
        //get the Quantity TextView display
        TextView Quantity = (TextView)findViewById(Integer.parseInt('1' + ("" + iden.substring(1))));

        int value = 0;
        if (iden.charAt(0) == '2'){ //if button is plus,add 1
            if (Integer.parseInt(Quantity.getText().toString()) < 998) {
                value = 1;
            }
        } else { //if button is minus, subtract 1
            if (Integer.parseInt(Quantity.getText().toString()) > 0) {
                value = -1;
            }
        }
        //set Stock display to changed value
        value += Integer.parseInt(Quantity.getText().toString());
        Quantity.setText("" + value);

        //change colour to red to show that the change has not been confirmed/added to the local database
        Quantity.setTextColor(Color.RED);
    }

    //adds quickchanges to local database
    public void confirmChange(View view){
        //gets the textview associated with the appropriate item
        TextView Quantity = (TextView)findViewById(Integer.parseInt('1' + (view.getId() + "")));

        //get the changed value
        int value = Integer.parseInt(Quantity.getText().toString());

        //get the number of offline changes
        int i = databaseChanges.getInt("number", 0);

        try {
            JSONArray JsonArray = new JSONArray(sharedPrefFood.getString("food_database", null));
            JSONObject row = JsonArray.getJSONObject(AllFoodFragment.newDatabase.getJSONObject(view.getId()).getInt("real_position"));

            //ADD TO OFFLINE CHANGES LIST
            editor = databaseChanges.edit();

            //find change (final - initial)
            int change = Integer.parseInt(row.getString("quantity_have")) - value;

            if (!row.getString("id").equals("null")) {
                i++;
                editor.putString("change_" + i, "{\"id\":" + row.getInt("real_position") + ", \"change\":" + change + "}");
                editor.putInt("number", i);
                editor.apply();

                //update local db
                row.put("quantity_have",Quantity.getText().toString());
                JsonArray.put(view.getId(), row);
                editor = sharedPrefFood.edit();
                editor.putString("food_database", JsonArray.toString());
                editor.apply();

                try { //attempt to make changes online
                    syncChanges();
                } catch (IOException e){}
            } else {
                Quantity.setText(row.getString("quantity_have"));
                alertDialog.setTitle("Sync Failed");
                alertDialog.setMessage("Cannot make changes to offline additions!");
                alertDialog.show();
            }

        } catch (JSONException e){
            System.out.println("Failed to change item details");
        }
        //set stock display text to black to show user that change has been confirmed
        Quantity.setTextColor(Color.BLACK);
    }

    public static JSONArray confirmDeletion (int pos) {

        if (pos > -1){
            AllFoodFragment.isChecked.set(pos, true);
        }

        //get local database
        JSONArray food_database_array;
        try {
            food_database_array = new JSONArray(sharedPrefFood.getString("food_database", null));
        } catch (JSONException e) {
            food_database_array = new JSONArray();
        }

        JSONArray new_database = new JSONArray();
        JSONArray removals = new JSONArray();

       //put deletions into removals list, put non-deletions into new database
        for (int i = 0; i < AllFoodFragment.isChecked.size(); i++) {
            if (!AllFoodFragment.isChecked.get(i)) {
                try {
                    new_database.put(food_database_array.getJSONObject(i));
                } catch (JSONException e) {
                }
            } else {
                try {
                    removals.put(food_database_array.getJSONObject(i));
                } catch (JSONException e) {
                }
            }
        }

        //change the real positions of food in fitered food database
        for (int i = 0; i < AllFoodFragment.isChecked.size(); i++)
            try {
                new_database.getJSONObject(i).put("real_position", i);
            } catch (JSONException e) {
            }

        //put new database (without the ones deleted) as new local database
        editor = sharedPrefFood.edit();
        editor.putString("food_database", new_database.toString());
        editor.apply();

        //ADD TO OFFLINE DELETIONS LIST

        editor = databaseDeletions.edit();

        int count = databaseDeletions.getInt("number", 0) + 1;

        for (int i = 0; i < removals.length(); i++){
            try {
                editor.putInt("deletion_" + count, removals.getJSONObject(i).getInt("id"));
            } catch (JSONException e) {}
            count++;
        }

        editor.putInt("number", count);
        editor.apply();

        try { //attempt to make changes online
            syncDeletions();
        } catch (IOException e){

        }
        return new_database;
    }

    public void onlineSync() throws IOException{

        //gets the number of additions
        int i = databaseAdditions.getInt("number", 0);

        //get the latest addition number via index
            try { //prepare query to add food
                Url = "http://www.myf00d.x10host.com/add_food.php";
                charset = "UTF-8";

                String email = sharedPref.getString("email", null);
                String password = sharedPref.getString("password", null);

                JSONObject row = new JSONObject(databaseAdditions.getString("addition_" + i, null));
                String name = row.getString("name");
                String category = row.getString("category");
                int quantity_have = row.getInt("quantity_have");
                String unit = row.getString("unit_of_measurement");
                String expiry_date = row.getString("expiry_date");
                String price = row.getString("price");
                String notes = row.getString("notes");

                String query = String.format("email=%s" +
                                "&password=%s" +
                                "&name=%s" +
                                "&category=%s" +
                                "&quantity_have=%s" +
                                "&unit=%s" +
                                "&expiry_date=%s" +
                                "&price=%s" +
                                "&notes=%s" +
                                "&appPackage=%s",
                        URLEncoder.encode(email, charset),
                        URLEncoder.encode(password, charset),
                        URLEncoder.encode(name, charset),
                        URLEncoder.encode(category, charset),
                        URLEncoder.encode(Integer.toString(quantity_have), charset),
                        URLEncoder.encode(unit, charset),
                        URLEncoder.encode(expiry_date, charset),
                        URLEncoder.encode(price, charset),
                        URLEncoder.encode(notes, charset),
                        (getPackageName()));

                if (i > 0) { //if there are any unsynced additions
                    //try to sync addition online
                    try {
                        new addFoodTask().execute(Url, charset, query);
                    } catch (RuntimeException e) {
                        System.out.println("JAVA RUNTIME EXCEPTION");
                    }
                }

            } catch (JSONException e){
                System.out.println("onlineSync - no offline additions");
            } catch (NullPointerException e){
                syncChanges();
            }
        //all other syncs are done in postExecute so all sync changes occur one at a time
    }

    //prepares quickchange query
    public void syncChanges() throws IOException{

        //get number of offline changes
        int i = databaseChanges.getInt("number", 0);

        //if there are any offline changes
        if (i > 0) {
            //prepare query
            try {

                Url = "http://www.myf00d.x10host.com/quick_change.php";
                charset = "UTF-8";

                String email = sharedPref.getString("email", null);
                String password = sharedPref.getString("password", null);

                JSONObject row = new JSONObject(databaseChanges.getString("change_" + i, null));

                JSONArray food_database_array = new JSONArray(sharedPrefFood.getString("food_database", null));

                //get food_id
                int local_id = Integer.parseInt(row.getString("id"));
                JSONObject local_row = food_database_array.getJSONObject(local_id);
                String id = Integer.toString(local_row.getInt("id"));

                int quantity_change = row.getInt("change");

                String query = String.format("change=%s&id=%s&email=%s&password=%s",
                        URLEncoder.encode(Integer.toString(quantity_change), charset),
                        URLEncoder.encode(id, charset),
                        URLEncoder.encode(email, charset),
                        URLEncoder.encode(password, charset));

                new quickChangeTask().execute(Url, charset, query);

            } catch (JSONException e) {
                System.out.println("syncChanges - failed to get JSON object");
            }
        } else {
            syncDeletions();
        }
    }

    public static void syncDeletions() throws IOException{

        //get number of offline deletions
        int i = databaseDeletions.getInt("number", 0);

        //if there are any offline deletions
        if (i > 0) {
            //prepare query
            Url = "http://www.myf00d.x10host.com/delete.php";
            charset = "UTF-8";

            String email = sharedPref.getString("email", null);
            String password = sharedPref.getString("password", null);

            String id = Integer.toString((databaseDeletions.getInt("deletion_" + i, 0)));

            String query = String.format("id=%s&email=%s&password=%s",
                    URLEncoder.encode(id, charset),
                    URLEncoder.encode(email, charset),
                    URLEncoder.encode(password, charset));

                new deletionTask().execute(Url, charset, query);
        }
    }

    //update local database background task
    private class getDataTask extends AsyncTask<String, Void, String> {
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


                try (OutputStream output = conn.getOutputStream()) {
                    output.write(data[2].getBytes(data[1]));
                }

                Scanner inStream = new Scanner(conn.getInputStream());

                while (inStream.hasNextLine())
                    response += (inStream.nextLine());



                if (response.equals("{\"food\":null" + "}")){
                    return "";
                }

                //return JSONArray of online foods associated with user family, pass to postExecute
                return response;

            } catch (IOException e) {
                return "connFailed";
            }
        }

        @Override
        //process return from server
        protected void onPostExecute(String s) {
            editor = sharedPrefFood.edit();
            if (s.equals("")) { //nothing added online
                //clear local database
                editor.putString("food_database", null);
            } else if (s.equals("connFailed")) { //connection failed
                alertDialog.setTitle("Sync Failed");
                alertDialog.setMessage("Failed to retrieve online database! Verify internet connection!");
                alertDialog.show();
            } else { //got JSONArray/food database from server
                try {
                    //change local database to server response
                    JSONObject titled_food = new JSONObject(s);
                    JSONArray not_titled_food = titled_food.getJSONArray("food");
                    for(int i = 0; i < not_titled_food.length();i++)
                        not_titled_food.getJSONObject(i).put("real_position", i);
                    editor.putString("food_database", not_titled_food.toString());
                } catch (JSONException e){
                    System.out.println("getDataTask - onPostExecute JSON Error");
                }
            }
            editor.apply();
        }
    }

    //adds food to online server
    private class addFoodTask extends AsyncTask <String, Void, String> {

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
            if (s.equals("connFailed")){
                alertDialog.setTitle("Sync Failed");
                alertDialog.setMessage("Failed to sync changes! Changes will be synced upon next sync.");
                alertDialog.show();
            } else if (s.equals("repeat_id")) {
                System.out.println("MainActivity - failed to put food into database, this should never happen!");
            } else {
                //get number of additions
                int i = databaseAdditions.getInt("number", 0);

                //take away one and put back into saved data
                if (i > 0){
                    editor = databaseAdditions.edit();
                    editor.putInt("number", i-1);
                    editor.apply();

                    //wait a bit before sending another query to the server
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e){

                    }
                    //do another database addition query
                    try {
                        onlineSync();
                    } catch (IOException e){

                    }

                } else {
                    //reset addition number to 0 after all additions have been added
                    editor = databaseAdditions.edit();
                    editor.putInt("number", 0);
                    editor.apply();
                    //sync changes
                    try {
                        syncChanges();
                    }catch (IOException e){}
                }
            }
        }
    }

    //adds quickchanges to online db in background
    private class quickChangeTask extends AsyncTask <String, Void, String> {

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
            if (s.equals("connFailed")){ //connection failed
                alertDialog.setTitle("Sync Failed");
                alertDialog.setMessage("Failed to sync changes! Changes will be synced upon next sync.");
                alertDialog.show();
            } else if (s.equals("400")) {
                System.out.println("MainActivity - failed to QUICK CHANGE database, this should never happen!");
            } else {
                //get number of database changes
                int i = databaseChanges.getInt("number", 0);

                if (i > 0){
                    editor = databaseChanges.edit();
                    editor.putInt("number", i-1);
                    editor.apply();
                    //subtract 1 from database changes number

                    //wait a bit before pushing another change to the server
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e){

                    }

                    //sync another change if there is one
                    try{
                        syncChanges();
                    }catch (IOException e){

                    }
                } else { //no more changes to push to server
                    //reset number of changes
                    editor = databaseChanges.edit();
                    editor.putInt("number", 0);
                    editor.apply();

                    updateDatabase();
                    try {
                        syncDeletions();
                    } catch (IOException e){}
                }
            }
        }
    }

    //adds deletions to online db in background
    private static class deletionTask extends AsyncTask <String, Void, String> {

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
            if (s.equals("connFailed")){ //connection failed
                alertDialog.setTitle("Sync Failed");
                alertDialog.setMessage("Failed to sync changes! Changes will be synced upon next sync.");
                alertDialog.show();
            } else if (s.equals("400")) {
                System.out.println("MainActivity - failed to delete from database, this should never happen!");
            } else {
                //get number of database deletions
                int i = databaseDeletions.getInt("number", 0);

                if (i > 0){
                    //subtract 1 from database deletions number
                    editor = databaseDeletions.edit();
                    editor.putInt("number", i-1);
                    editor.apply();

                    //wait a bit before pushing another deletion to the server
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e){

                    }

                    //sync another deletion if there is one
                    try{
                        syncDeletions();
                    }catch (IOException e){

                    }
                } else { //no more deletions to push to server
                    //reset number of deletions
                    editor = databaseDeletions.edit();
                    editor.putInt("number", 0);
                    editor.apply();
                }
            }
        }
    }
}
