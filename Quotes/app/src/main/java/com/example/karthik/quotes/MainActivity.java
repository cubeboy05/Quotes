package com.example.karthik.quotes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.appevents.AppEventsLogger;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    static String entireCode = "";
    static String preAuthor = "";
    //extract out quotes. Need to remove <br> and <br><br>
    static Pattern p = Pattern.compile("dquo;(.*?)&rdq");
    //extract out author pattern. Needs further extracting
    static Pattern p1 = Pattern.compile("<a class=\"authorOrTitle(.*?)a>");
    //author perfectly extracted pattern after apply P1 first.
    static Pattern p2 = Pattern.compile(">(.*?)</");
    static Matcher m;
    public static ArrayList<String> quotes = new ArrayList<>();
    public static ArrayList<String> authors = new ArrayList<>();

    TextView tvQuote, toolbarTitle;
    Toolbar toolbar;
    ImageView heartImgView;
    String category, tableName;
    SharedPreferences preferences;
    DBHelper dbHelper;
    long rowCount;
    boolean isChecked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get the chosen category
        Intent i = getIntent();
        category = i.getStringExtra("category").toLowerCase();
        tableName = category+"Table";
        preferences = getSharedPreferences("com.example.karthik.quotes", Context.MODE_PRIVATE);

        //setting up toolbar
        toolbar = (Toolbar)findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarTitle = (TextView)findViewById(R.id.toolbar_title);
        toolbarTitle.setText(i.getStringExtra("category"));

        //setting up font
        tvQuote = (TextView)findViewById(R.id.tvQuote);
        Typeface face= Typeface.createFromAsset(getAssets(), "fonts/georgiabelle.ttf");
        tvQuote.setTypeface(face);
        tvQuote.setMovementMethod(new ScrollingMovementMethod());
        heartImgView = (ImageView)findViewById(R.id.heartImgView);

        //getting the database stored in assets folder
        try {
            dbHelper = new DBHelper(this);
            dbHelper.getReadableDatabase();
            rowCount = DatabaseUtils.queryNumEntries(dbHelper.getReadableDatabase(), tableName, null);
        } catch (SQLiteAssetHelper.SQLiteAssetException e) {
            e.printStackTrace();
        }

        nextQuote(null);

        SetupData setupData = new SetupData();
        setupData.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.social_share){
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "This is the title probably"); //This is long content
            i.putExtra(android.content.Intent.EXTRA_SUBJECT, "A beautiful quote"); // This is title
            startActivity(i.createChooser(i, "Share"));
        }
        return true;
    }

    public void selectHeart(View view){
        String[] inserts = {category, preferences.getString(category, "1")};
        if(!isChecked){
            heartImgView.setImageResource(R.drawable.ic_filled_heart);
            dbHelper.getWritableDatabase().execSQL("UPDATE " + tableName + " SET isFavorite=1 WHERE category=? AND rowid=?", inserts);
            isChecked = true;
            Toast.makeText(MainActivity.this, "Added to Favourite", Toast.LENGTH_SHORT).show();
        }
        else{
            heartImgView.setImageResource(R.drawable.ic_empty_heart);
            dbHelper.getWritableDatabase().execSQL("UPDATE " + tableName + " SET isFavorite=0 WHERE category=? AND rowid=?", inserts);
            isChecked = false;
            Toast.makeText(MainActivity.this, "Removed from Favourite", Toast.LENGTH_SHORT).show();
        }
    }

    public void previousQuote(View view){
        tvQuote.scrollTo(0,0);
        String favorite;
        //store and increment rowid to not repeat quotes.
        int decrementBy1 = Integer.valueOf(preferences.getString(category, "1")) - 1;
        if(decrementBy1 > 0) {
            preferences.edit().putString(category, String.valueOf(decrementBy1)).apply();
        }
        else {
            preferences.edit().putString(category, String.valueOf(rowCount)).apply();
        }

        try {
            String[] inserts = {category, preferences.getString(category, "1")};
            Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT quote, author, isFavorite FROM " +  tableName + " WHERE category=? AND rowid=? LIMIT 1", inserts);

            if(c.moveToFirst() && c.getCount() >= 1){
                do {
                    String quote = c.getString(c.getColumnIndex("quote"));
                    String author = c.getString(c.getColumnIndex("author"));
                    favorite = c.getString(c.getColumnIndex("isFavorite"));
                    tvQuote.setText(quote + "\n\n" + author);

                    Log.i("zx", favorite);
                    if(favorite.equals("1") || favorite == "1"){
                        heartImgView.setImageResource(R.drawable.ic_filled_heart);
                        isChecked = true;
                    }
                    else {
                        heartImgView.setImageResource(R.drawable.ic_empty_heart);
                        isChecked = false;
                    }
                }while (c.moveToNext());
            }
            else{
                //reset to 1 and start over when finished all quotes in a category
                preferences.edit().putString(category, String.valueOf(1)).apply();
                inserts[1] = preferences.getString(category, "1");

                Cursor c1 = dbHelper.getReadableDatabase().rawQuery("SELECT quote, author, isFavorite FROM " +  tableName + " WHERE category=? AND rowid=? LIMIT 1", inserts);

                if(c1.moveToFirst() && c.getCount() >= 1){
                    do {
                        String quote = c1.getString(c.getColumnIndex("quote"));
                        String author = c1.getString(c.getColumnIndex("author"));
                        favorite = c.getString(c.getColumnIndex("isFavorite"));
                        tvQuote.setText(quote + "\n\n" + author);

                        if(favorite.equals("1") || favorite == "1"){
                            heartImgView.setImageResource(R.drawable.ic_filled_heart);
                            isChecked = true;
                        }
                        else {
                            heartImgView.setImageResource(R.drawable.ic_empty_heart);
                            isChecked = false;
                        }
                    }while (c1.moveToNext());
                }
            }
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void nextQuote(View view){
        tvQuote.scrollTo(0,0);
        String favorite;
        //store and increment rowid to not repeat quotes.
        int incrementBy1 = Integer.valueOf(preferences.getString(category, "1")) + 1;
        preferences.edit().putString(category, String.valueOf(incrementBy1)).apply();


        try {
            String[] inserts = {category, preferences.getString(category, "1")};

            Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT quote, author, isFavorite FROM " +  tableName + " WHERE category=? AND rowid=? LIMIT 1", inserts);

            if(c.moveToFirst() && c.getCount() >= 1){
                do {
                    String quote = c.getString(c.getColumnIndex("quote"));
                    String author = c.getString(c.getColumnIndex("author"));
                    favorite = c.getString(c.getColumnIndex("isFavorite"));
                    tvQuote.setText(quote + "\n\n" + author);

                    Log.i("zx", favorite);
                    if(favorite.equals("1") || favorite == "1"){
                        heartImgView.setImageResource(R.drawable.ic_filled_heart);
                        isChecked = true;
                    }
                    else {
                        heartImgView.setImageResource(R.drawable.ic_empty_heart);
                        isChecked = false;
                    }
                }while (c.moveToNext());
            }
            else{
                //reset to 1 and start over when finished all quotes in a category
                preferences.edit().putString(category, String.valueOf(1)).apply();
                inserts[1] = preferences.getString(category, "1");

                Cursor c1 = dbHelper.getReadableDatabase().rawQuery("SELECT quote, author FROM "  +  tableName + " WHERE category=? AND rowid=? LIMIT 1", inserts);

                if(c1.moveToFirst() && c.getCount() >= 1){
                    do {
                        String quote = c1.getString(c.getColumnIndex("quote"));
                        String author = c1.getString(c.getColumnIndex("author"));
                        favorite = c.getString(c.getColumnIndex("isFavorite"));
                        tvQuote.setText(quote + "\n\n" + author);

                        if(favorite.equals("1") || favorite == "1"){
                            heartImgView.setImageResource(R.drawable.ic_filled_heart);
                            isChecked = true;
                        }
                        else {
                            heartImgView.setImageResource(R.drawable.ic_empty_heart);
                            isChecked = false;
                        }

                    }while (c1.moveToNext());
                }
            }
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void setupDB(){
        String tName = "loveTable";
        String tCategory = "love";
        int falseValue = 0;
        try {
            SQLiteDatabase db = this.openOrCreateDatabase("QuoteDb", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS " + tName + " (quote VARCHAR, author VARCHAR, category VARCHAR, isFavorite VARCHAR)");

            int count=0;
            while(count < quotes.size()) {
                String[] inserts = {"\"" + quotes.get(count) + "\"", authors.get(count), tCategory, String.valueOf(falseValue)};

                db.execSQL("INSERT INTO " + tName + " (quote, author, category, isFavorite) VALUES (?, ?, ?, ?)", inserts);
                count++;
            }
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class SetupData extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            //getQuote();
            //setupDB();

            Log.i("zxx", "All Done!");
            return null;
        }

        private void getQuote(){
            URL url;
            int pageNumber = 0;

            try {
                //Loop and grab all the web page content first
                while (pageNumber < 5) {
                    pageNumber++;
                    String a="http://www.goodreads.com/quotes/tag/love?page=" + pageNumber;
                    //String a="https://www.goodreads.com/quotes/tag/inspirational?page=" + pageNumber;
                    url = new URL(a);
                    URLConnection conn = url.openConnection();

                    // open the stream and put it into BufferedReader
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));

                    String inputLine;
                    while ((inputLine = br.readLine()) != null) {
                        //adding up the entire code of the each page
                        entireCode += inputLine + "\n";
                    }
                    br.close();
                }

                //removing the <br>, <br><br> and replacing with spacing
                String revisedStr1 =  entireCode.replaceAll("<br><br>", " ");
                String revisedStr2 =  revisedStr1.replaceAll("<br>", " ");

                //Quotes extracted perfectly
                m = p.matcher(revisedStr2);
                while (m.find()){
                    quotes.add(m.group(1));
                }

                //author extracted. Need to further extract
                m = p1.matcher(revisedStr2);
                while (m.find()){
                    preAuthor += m.group(1) + "\n";
                }

                //author extracted perfectly
                m = p2.matcher(preAuthor);
                while (m.find()){
                    authors.add("- " + m.group(1));
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class DBHelper extends SQLiteAssetHelper{
        private static final String DATABASE_NAME = "QuoteDb.db";
        private static final int DATABASE_VERSION = 1;

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
    }
}
