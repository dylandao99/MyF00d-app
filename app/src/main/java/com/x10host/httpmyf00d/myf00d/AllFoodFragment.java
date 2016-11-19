package com.x10host.httpmyf00d.myf00d;

//All Food Fragment - Displays All/Filtered Food List in a alphabetically ordered, interactive list
//users can select food to delete, edit food, or view food details

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class AllFoodFragment extends ListFragment implements AbsListView.MultiChoiceModeListener {

    SharedPreferences sharedPrefFood;

    static JSONArray newDatabase;

    JSONArray food_database;

    allFoodAdapter allfoodadapter;

    Toolbar toolbar;

    public static ArrayList<Boolean> isChecked;

    public AllFoodFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        //get local food database
        sharedPrefFood = this.getActivity().getSharedPreferences(this.getActivity().getPackageName() + ".food_database", this.getActivity().MODE_PRIVATE);

        resetList();

        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_all_food, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemLongClickListener(longClick);
    }

    //reset the food list
    public void resetList(){

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        //get database
        try {
            food_database = new JSONArray(sharedPrefFood.getString("food_database", null));

        } catch (JSONException e){
        } catch (NullPointerException e){ //create new food database and show No Food message
            food_database = new JSONArray();
            noFood();
        }

        //if filtered food list selected from Categories Fragment
        if (getArguments().getString("category") != null) {

            newDatabase = new JSONArray();

            toolbar.setTitle(getArguments().getString("category"));

            //add all food with a certain category to a new array/database
            try {
                for (int i = 0; i < food_database.length(); i++)
                    if (food_database.getJSONObject(i).getString("category").equalsIgnoreCase(getArguments().getString("category")))
                        newDatabase.put(food_database.getJSONObject(i));

            } catch (JSONException e) {}

            //if newdatabase is empty, return to the categories fragment
            if (newDatabase.toString().equals("[]")){
                Fragment fr = new CategoriesFragment();

                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                ft.replace(R.id.fragment_container, fr, "category").commit();
            }
        } else {
            //disply all food, set new database to all food database
            toolbar.setTitle("All Food");
            newDatabase = food_database;
        }

        //create list with new database
        allfoodadapter = new allFoodAdapter(getActivity(), newDatabase){};

        //uncheck all food items
        isChecked = new ArrayList<>();

        for (int i = 0; i < food_database.length();i++)
            isChecked.add(i, false);

        setListAdapter(allfoodadapter);

        //if no food in database, show No Food message
        if (food_database.toString().equals("[]")) {
            LinearLayout fragCont = (LinearLayout)getActivity().findViewById(R.id.fragment_container);
            fragCont.removeAllViews();
            noFood();
        }
    }

    //show No Food message
    public void noFood(){
        LinearLayout lLayout = (LinearLayout)getActivity().findViewById(R.id.fragment_container);
        TextView noFood = new TextView(getActivity().getApplicationContext());
        noFood.setText(R.string.empty_food);
        noFood.setTextColor(Color.BLACK);
        noFood.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lLayout.addView(noFood);
    }

    //Edit food item upon clicking food item in list
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        try {
            if (!newDatabase.getJSONObject(position).getString("id").equals("null")) {//addition has been synced online
                //send selected food information to Add Food
                Intent i = new Intent(getActivity().getApplicationContext(), addFood.class);
                i.putExtra("selected", position);
                i.putExtra("database", newDatabase.toString());
                startActivity(i);
            } else { //addition has not been synced online, show error message
                MainActivity.alertDialog.setTitle("Sync Failed");
                MainActivity.alertDialog.setMessage("Can't make changes to offline additions!");
                MainActivity.alertDialog.show();
            }
        } catch (JSONException e){}
    }

    //enable Delete Context Menu upon long clicking a food item
    AdapterView.OnItemLongClickListener longClick = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

            // Start the CAB using the ActionMode.Callback defined above
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            getListView().setMultiChoiceModeListener(AllFoodFragment.this);
            getListView().setItemChecked(position, true);
            return true;
        }
    };

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        //inflate bar with delete button
        MenuInflater mi = mode.getMenuInflater();
        mi.inflate(R.menu.food_long_click, menu);
        return true;
    }

    //delete selected food items
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        //call deletion function in Main Activity
        food_database = MainActivity.confirmDeletion(-1);

        //update the food list
        resetList();

        //return to list viewing interaction mode
        mode.finish();
        return true;
    }


    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        int i = 0;
        try {
            //if in filtered food list, get main local database position
            i = newDatabase.getJSONObject(position).getInt("real_position");
            if (!newDatabase.getJSONObject(position).getString("id").equals("null")) //if selected food synced online
                isChecked.set(i, checked);
            else { //if selected food not synced online
                MainActivity.alertDialog.setTitle("Sync Failed");
                MainActivity.alertDialog.setMessage("Can't delete offline additions!");
                MainActivity.alertDialog.show();
            }
        } catch (JSONException e){}

        //reset food list
        allfoodadapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        //uncheck all food
        for (int i = 0; i < isChecked.size();i++)
            isChecked.set(i, false);
        //reset food list
        allfoodadapter.notifyDataSetChanged();
    }
}
