package com.example.karthik.quotes;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class SelectionActivity extends AppCompatActivity {

    TextView love, fear, faith, humour, poetry, science, happiness, inspiration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        //setup category titles
        love = (TextView)findViewById(R.id.tvLoveCat);
        fear = (TextView)findViewById(R.id.tvFearCat);
        faith = (TextView)findViewById(R.id.tvFaithCat);
        humour = (TextView)findViewById(R.id.tvHumourCat);
        poetry = (TextView)findViewById(R.id.tvPoetryCat);
        science = (TextView)findViewById(R.id.tvScienceCat);
        happiness = (TextView)findViewById(R.id.tvHappinessCat);
        inspiration = (TextView)findViewById(R.id.tvInspirationCat);

        //setting up font
        TextView[] tvs = {love, fear, faith, humour, poetry, science, happiness, inspiration};
        Typeface face= Typeface.createFromAsset(getAssets(), "fonts/sun.ttf");
        for(TextView tv : tvs){
            tv.setTypeface(face);
        }
    }

    public void chooseCategory(View view){
        //selecting the chosen category ans sending over to next Activity
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        TextView catTv = (TextView)view;
        String category = catTv.getText().toString();
        i.putExtra("category", category);
        startActivity(i);
    }
}
