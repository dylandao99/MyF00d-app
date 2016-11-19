package com.x10host.httpmyf00d.myf00d;

//allFoodAdapter - Dictates the Food List Generation
//Dictates what information is displayed in the food list and how it is displayed

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class allFoodAdapter extends BaseAdapter {

    Context Context;
    JSONArray JsonArray;

    public allFoodAdapter (Context context, JSONArray jsonarray){
        Context = context;
        JsonArray = jsonarray;
    }

    @Override
    //get number of items in food database
    public int getCount() {
        return JsonArray.length();
    }

    @Override
    //create individual list entries
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater mInflater = (LayoutInflater) Context
                .getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);
        convertView = mInflater.inflate(R.layout.food_list_item, null);

        String name = "";
        String category= "";
        String expiryDate="";
        String unit = "";
        int currentStock = 0;

        try { //JSON array additions

            //get single row of local database based upon list position (alphabetical)
            JSONObject row = JsonArray.getJSONObject(position);

            //get fields stored in row
            name = row.getString("name");
            category = row.getString("category");
            expiryDate = row.getString("expiry_date");
            unit = row.getString("unit_of_measurement");
            currentStock = row.getInt("quantity_have");

            int realpos = JsonArray.getJSONObject(position).getInt("real_position");


            //set highlighted colour if checked
            if (AllFoodFragment.isChecked.size() > 0 && AllFoodFragment.isChecked.get(realpos))
                convertView.setBackground(convertView.getResources().getDrawable(R.drawable.selected_item));

            //get Views to display information
            TextView Name = (TextView) convertView.findViewById(R.id.listItem_name);
            TextView Category = (TextView) convertView.findViewById(R.id.listItem_category);
            TextView ExpiryDate = (TextView) convertView.findViewById(R.id.listItem_expiryDate);
            TextView CurrentStock = (TextView) convertView.findViewById(R.id.listItem_quantity);
            CurrentStock.setInputType(InputType.TYPE_CLASS_NUMBER);
            TextView Unit = (TextView) convertView.findViewById(R.id.listItem_unit);

            //set local db information to views
            Name.setText(name);

            //if name is long, reduce text size to fit it in the ListItem
            if (name.length() > 10){
                Name.setTextSize(20 - name.length()/4);
            }
            Category.setText(category);

            ExpiryDate.setText("Expiry Date: " + expiryDate);

            CurrentStock.setText("" + currentStock);
            if (!unit.equals("")) {
                Unit.setText(unit + "(s)");
            } else {
                Unit.setText("unit(s)");
            }

            //create confirm changes button
            Button Confirm = (Button) convertView.findViewById(R.id.listItem_confirm);
            //set id to position
            Confirm.setId(position);
            //set onclick to confirmChange function in MainActivity
            Confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {if (Context instanceof MainActivity) {
                            ((MainActivity) Context).confirmChange(v);
                        }
                    }
                });

            //set CurrentStock display id to "1*position*
            CurrentStock.setId(Integer.parseInt('1' + (position + "")));

            //set Plus1 Stock button display id to "2*position*
            Button Plus = (Button) convertView.findViewById(R.id.listItem_plus);
            Plus.setId(Integer.parseInt('2' + (position + "")));

            //set Minus1 Stock button display id to "2*position*
            Button Minus = (Button) convertView.findViewById(R.id.listItem_minus);
            Minus.setId(Integer.parseInt('3' + (position + "")));
        } catch (JSONException e){
            System.out.println("getView - JSONException");
        }

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }
}
