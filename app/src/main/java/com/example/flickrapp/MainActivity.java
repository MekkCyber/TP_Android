package com.example.flickrapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button img = (Button) findViewById(R.id.get_img);
        img.setOnClickListener(new GetImageOnClickListener());

        Button get_img_butt = findViewById(R.id.get_img);
        get_img_butt.setOnClickListener(new GetImageOnClickListener());

        Button list_view = findViewById(R.id.go_list);
        list_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ListViewActivity.class);
                startActivity(intent);
            }
        });
    }
/**************************************************************************************************************************************/
    public class GetImageOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AsyncFlickrJSONData data = new AsyncFlickrJSONData();
            data.execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=trees&format=json");
        }
    }
/****************************************************************************************************************************************/
    public class AsyncFlickrJSONData extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... strings) {
            URL url = null;
            JSONObject result = null;
            try {
                url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String s = readStream(in);
                    in.close();
                    int msgLen = s.length();
                    // On essai de ne garder que le sous chaine de caractères après jsonFlickrFeed(, et de longueur msgLen
                    String msgJson = s.substring("jsonFlickrFeed(".length(), msgLen - 1);
                    result = new JSONObject(msgJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            Log.i("json :  ", jsonObject.toString());

            try {
                // D'après l'API ça devient clair pourquoi cet enchainement
                String url = jsonObject.getJSONArray("items").getJSONObject(1).getJSONObject("media").getString("m");

                AsyncBitmapDownloader bitmap = new AsyncBitmapDownloader();
                bitmap.execute(url);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        private String readStream(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
            for (String line = r.readLine(); line != null; line =r.readLine()){
                sb.append(line);
            }
            is.close();
            return sb.toString();
        }
    }
/**************************************************************************************************************************************/
    public class AsyncBitmapDownloader extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap img = null;
            URL url = null;
            try {
                url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream input = new BufferedInputStream(urlConnection.getInputStream());
                    img = BitmapFactory.decodeStream(input);
                    input.close();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return img;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView img = (ImageView) findViewById(R.id.listview_img);
            img.setImageBitmap(bitmap);
        }
    }
}