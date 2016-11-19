package com.x10host.httpmyf00d.myf00d;

//Main Menu Screen - Central navigation menu
//Allows user to navigate to all other post-login screens and logout

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.io.IOException;

public class main_mainMenu extends ListFragment {

    SharedPreferences sharedPref;

    public main_mainMenu() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        sharedPref = getActivity().getSharedPreferences(getActivity().getPackageName() + ".saved_user", getActivity().MODE_PRIVATE);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle("Hello " + sharedPref.getString("fName", "User") + "!");

        //clear FragmentContainer
        container.removeAllViews();

        return inflater.inflate(R.layout.fragment_main_main_menu, container, false);
    }

    @Override
    public void onResume() {
        //set toolbar title to "Hello (User name)!"
        Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Hello " + sharedPref.getString("fName", "User") + "!");
        super.onResume();
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        //declare fragment
        Fragment fr = null;
        switch (position){ //depending on list choice, change to appropriate fragment
            case 0: //all food
                fr = new AllFoodFragment();
                break;
            case 1: //categories
                fr = new CategoriesFragment();
                break;
            case 2: //add food
                Intent i = new Intent(getActivity().getApplicationContext(), addFood.class);
                startActivity(i);
                break;
            case 3: //sync
                try {
                    ((MainActivity) getActivity()).onlineSync();
                } catch (IOException e){}
                ((MainActivity)getActivity()).updateDatabase();
                break;
            case 4: //logout
                //clear all saved files
                clearPrefs(".saved_user");
                clearPrefs(".food_database");
                clearPrefs(".database_additions");
                clearPrefs(".databaseChanges");
                clearPrefs(".databaseDeletions");
                Intent in = new Intent(getActivity().getApplicationContext(), login.class);
                startActivity(in);
                break;
            default:
                fr = new AllFoodFragment();
                break;
        }
        //pass database to fragment
        Bundle args = new Bundle();
        args.putString("database", getActivity().getSharedPreferences(getActivity().getPackageName() + ".food_database",
                getActivity().MODE_PRIVATE).getString("food_database", null));
        if (fr != null) {
            fr.setArguments(args);
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment_container, fr);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    //clear all locally saved data (logout)
    public void clearPrefs(String pref){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getActivity().getPackageName() + pref, getActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
    }
}
