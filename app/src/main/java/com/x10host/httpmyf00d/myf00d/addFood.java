package com.x10host.httpmyf00d.myf00d;

//Add Food Screen - Add food to the online database
//View for user to enter in food details

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

public class addFood extends AppCompatActivity {

    SharedPreferences sharedPref;
    SharedPreferences sharedPrefFood;
    SharedPreferences databaseAdditions;

    Bundle extras;

    NumberPicker CurrentStock;

    TextView tCurrentStock;

    EditText Name, Category, Unit, Price, Notes;

    int currentStock;

    static String name, category, unit, expiryDate, price, notes;

    AlertDialog alertDialog;

    AlertDialog stocksEditDialog;
    LayoutInflater layoutInflater;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //declare/find all of the editText fields in the form
        Name = (EditText)findViewById(R.id.addFood_et_name);
        Category = (EditText)findViewById(R.id.addFood_et_category);
        Unit = (EditText)findViewById(R.id.addFood_et_unit);
        Price = (EditText)findViewById(R.id.addFood_et_price);
        Notes = (EditText)findViewById(R.id.addFood_et_notes);
        tCurrentStock = (TextView)findViewById(R.id.addFood_txt_currentStockNum);

        //get saved data
        sharedPref = getSharedPreferences(getPackageName() + ".saved_user", MODE_PRIVATE);
        sharedPrefFood = getSharedPreferences(getPackageName() + ".food_database", MODE_PRIVATE);
        databaseAdditions = getSharedPreferences(getPackageName() + ".database_additions", MODE_PRIVATE);

        //create edit stocks dialog with wheel number picker
        initializeEditStocksDialog();

        //create universal alert dialog
        alertDialog = new AlertDialog.Builder(addFood.this).create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        //get food info to be edited
        extras = getIntent().getExtras();

        //if food info has been sent to this activity
        if (extras != null){
            importSelection();
        }
    }

    //put food information to be edited into addFood screen form
    protected void importSelection(){
        getSupportActionBar().setTitle("Edit Food");

        //put existing values into EditTexts
        JSONArray food_database;
        JSONObject row;

        try {
            food_database = new JSONArray(extras.getString("database"));
            row = food_database.getJSONObject(extras.getInt("selected"));

            Name.setText (row.getString("name"));
            Category.setText (row.getString("category"));
            Unit.setText(row.getString("unit_of_measurement"));
            Price.setText (row.getString("price"));
            Notes.setText(row.getString("notes"));

            currentStock = row.getInt("quantity_have");

            tCurrentStock.setText(currentStock + "");

        } catch (JSONException e){}
    }

    //create editstocks dialog number pickers
    protected void initializeNumberPickers(){
        CurrentStock = (NumberPicker)stocksEditDialog.findViewById(R.id.editStocks_np_currentStock);

        CurrentStock.setMinValue(0);
        CurrentStock.setMaxValue(999);

        CurrentStock.setValue(currentStock);
    }

    //create editstocks dialog box
    protected void initializeEditStocksDialog(){
        //create alert dialog with okay button
        layoutInflater = getLayoutInflater();

        View layout = layoutInflater.inflate(R.layout.dialog_stocks_edit, null);

        stocksEditDialog = new AlertDialog.Builder(addFood.this).create();

        stocksEditDialog.setView(layout);

        stocksEditDialog.setTitle("Edit Stocks");

        stocksEditDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "DONE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //when okay is pressed...
                        //set the Quantity display to the wheel display
                        setStockNums();
                        //close the dialog box
                        dialog.dismiss();
                    }
                });
    }

    //sets stocks numbers in main screen to values in number pickers in editstocks dialog
    public void setStockNums(){

        //get numberpicker stock values
        currentStock = CurrentStock.getValue();

        //get numberpicker stock value
        String sCurrentStock = Integer.toString(currentStock);

        //put numberpicker stock values into main foodaddition screen values
        tCurrentStock.setText(sCurrentStock);
    }

    //opens editstocks dialog and initializes the numberpickers
    public void editStocks (View view){
        stocksEditDialog.show();
        initializeNumberPickers();
    }

    //validate food addition entry
    protected boolean validateInput(){

        //get entered text
        name = Name.getText().toString();
        category = Category.getText().toString();
        unit = Unit.getText().toString();
        price = Price.getText().toString();
        notes = Notes.getText().toString();

        if (name.equals("")){ //no name
            alertDialog.setTitle("Empty Name Field");
            alertDialog.setMessage("No item name entered!");
            alertDialog.show();
            return false;
        }
        return true;
    }

    //creates datepicker to set expiry date
    public void setExpiryDate(View view){
        DatePickerFragment datePicker = new DatePickerFragment();
        datePicker.show(getFragmentManager(), "datePicker");
    }

    //creates query to add food to server
    public void addFoodConfirm(View view) throws IOException{
        if (validateInput()) { //verify input is valid

            //get number of offline databaseAdditions
            int i = databaseAdditions.getInt("number", 0)+1;

            SharedPreferences.Editor editor = databaseAdditions.edit();

            //adds food in JSON format to local database
            String addition = "{\"id\":" + null + ", " +
                    "\"name\":\"" + name + "\", " +
                    "\"category\":\"" + category + "\", " +
                    "\"quantity_have\":" + currentStock + ", " +
                    "\"unit_of_measurement\":\"" + unit + "\", " +
                    "\"expiry_date\":\"" + expiryDate + "\", " +
                    "\"price\":\"" + price + "\", " +
                    "\"notes\":\"" + notes + "\"}";

            editor.putString("addition_" + i, addition);

            editor.putInt("number", i);

            editor.apply();

            //get local db, if doesn't exist, create a new one
            JSONArray local_db_array;
            try {
                local_db_array = new JSONArray(sharedPrefFood.getString("food_database", null));

                //add addition to fetched local db
                JSONObject newRow = new JSONObject(addition);
                newRow.put("real_position", local_db_array.length());
                local_db_array.put(newRow);

                if (extras != null){
                    MainActivity.confirmDeletion(local_db_array.getJSONObject(extras.getInt("selected")).getInt("real_position"));
                }
            } catch (JSONException e){ //if no existing database, create new database
                local_db_array = new JSONArray();
            } catch (NullPointerException e){ //if no existing database, create new database
                local_db_array = new JSONArray();
            }

            //push changes to local db
            editor = sharedPrefFood.edit();
            editor.putString("food_database", local_db_array.toString());
            editor.apply();

            Intent I = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(I);
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        //set date picker button text to chosen date
        public void onDateSet(DatePicker view, int year, int month, int day) {
            Button ExpiryDate = (Button)getActivity(). findViewById(R.id.addFood_btn_expiryDate);
            expiryDate = year + "-" + (month+1) + "-" + day;
            String placeholder = "Expiry Date: " + expiryDate;
            ExpiryDate.setText(placeholder);
        }
    }
}
