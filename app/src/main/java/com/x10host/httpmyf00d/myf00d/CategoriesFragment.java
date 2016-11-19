package com.x10host.httpmyf00d.myf00d;

//Categories Screen - Generate and display list of categories in food list
//When clicked, displays a Filtered Food List for the selected category

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class CategoriesFragment extends ListFragment {

    SharedPreferences sharedPrefFood;

    String categories[];

    ArrayAdapter<String> arrayAdapter;

    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //get local database
        sharedPrefFood = getActivity().getSharedPreferences(getActivity().getPackageName() + ".food_database", getActivity().MODE_PRIVATE);

        //set Categories fragment to fragment container
        view = inflater.inflate(R.layout.fragment_categories, container, false);

        resetList();

        // Inflate the layout for this fragment
        return view;
    }

    //regenerates list of categories
    public void resetList(){
        ArrayList<String> list = new ArrayList<>();

        //get categories
        try {
            JSONArray JsonArray = new JSONArray(sharedPrefFood.getString("food_database", null));

            //go through all food categories
            for (int i = 0; i<JsonArray.length(); i++){
                JSONObject row = JsonArray.getJSONObject(i);
                String category = row.getString("category");
                boolean exists = false;
                //if category is blank, do not add it to the list
                if (category.equals(""))
                    exists = true;
                //if category already exists, do not add it to the list
                for (int j = 0; (j < list.size()); j++){
                    if (category.equalsIgnoreCase(list.get(j)))
                        exists = true;
                }
                //if category does not exist, add category to the list
                if (!exists)
                    list.add(category);
            }

            //sort the list into alphabetical order
            Collections.sort(list);

            //if list is empty, display "No Categories" message
            if (list.isEmpty()){
                noCatagories();
            }

            //convert list to array
            categories = list.toArray(new String[list.size()]);

        } catch (JSONException e){
        } catch (NullPointerException e){
           noCatagories();
        }

        //generate interactive list with categories array
        arrayAdapter = new ArrayAdapter<>
                (getActivity(), android.R.layout.simple_list_item_1, list);

        ListView lv = (ListView)view.findViewById(android.R.id.list);
        lv.setAdapter(arrayAdapter);
    }

    //display message if there are no categories added
    public void noCatagories(){
        LinearLayout lLayout = (LinearLayout)getActivity().findViewById(R.id.fragment_container);
        TextView noFood = new TextView(getActivity().getApplicationContext());
        noFood.setText(R.string.empty_category);
        noFood.setTextColor(Color.BLACK);
        noFood.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lLayout.addView(noFood);
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Categories");
        //recreate categories list
        resetList();
        arrayAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        //declare fragment
        Fragment fr;
        fr = new AllFoodFragment();

        //send selected category to AllFoodFragment and switch Fragments
        Bundle args = new Bundle();
        args.putString("category", categories[position]);
        fr.setArguments(args);

        //switch fragments
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fr);
        ft.addToBackStack(null);
        ft.commit();
    }
}
