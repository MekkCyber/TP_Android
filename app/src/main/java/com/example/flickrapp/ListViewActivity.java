package com.example.flickrapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
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
import java.util.Vector;

public class ListViewActivity extends AppCompatActivity {
    Vector<String> vector = new Vector<>();
    MyAdapter adapter = new MyAdapter(vector, this);
    Double longitude = Double.valueOf(0);
    Double latitude = Double.valueOf(0);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // geolocalisation part, verify the permissions
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 123);

        }
        LocationListener locationListener = new LocationListener() {
            @Override
            // Event listner sur la géolocation
            public void onLocationChanged(@NonNull Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();

                Log.i("logitude : ",longitude.toString());
                Log.i("latitude : ",latitude.toString());
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);

        // list view pour les images
        ListView list = findViewById(R.id.list);
        // Adaptateur de la ListView
        list.setAdapter(adapter);
        AsyncFlickrJSONDataForList list_of_imgs = new AsyncFlickrJSONDataForList();
        list_of_imgs.execute("https://www.flickr.com/services/feeds/photos_public.gne?tags=cats&format=json",false);


    }

    /**********************************************************************************************************************************************/

    class MyAdapter extends BaseAdapter {
        Context context;
        Vector<String> vector = new Vector<>();

        public MyAdapter(Vector<String> vector, Context context) {
            this.vector = vector;
            this.context = context;
        }

        void dd(String url) {
            vector.add(url);
        }


        @Override
        public int getCount() {
            return vector.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            /*if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.textlayout, viewGroup, false);
            }
            TextView text = view.findViewById(R.id.textView);
            text.setText(vector.elementAt(i));*/
            if (view==null) {
                // inflate the bitmaplayout and get the imageView id to set the image
                view = LayoutInflater.from(context).inflate(R.layout.bitmaplayout, viewGroup, false);
            }

            RequestQueue queue = MySingleton.getInstance(view.getContext()).getRequestQueue();

            ImageView imageView = (ImageView) view.findViewById(R.id.imageView);

            ImageRequest imageRequest=new ImageRequest (vector.elementAt(i),new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    imageView.setImageBitmap(response);

                }
            },0,0, ImageView.ScaleType.CENTER_CROP,null, new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("Error", "getView()");
                    error.printStackTrace();

                }
            }
            );
            // Ajouter la requête à la queue des requêtes pour qu'elle soit traitée de façon asychrone
            queue.add(imageRequest);
            return view;
        }
    }

    /**********************************************************************************************************************************************/

    public class AsyncFlickrJSONDataForList extends AsyncTask<Object, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Object... strings) {
            URL url = null;
            JSONObject result = null;
            try {
                // si vrai, on prendra un url prenant compte de la logitude et latitude
                if ((Boolean)strings[1]){
                    url = new URL(((String)strings[0])+"&lat="+latitude.toString() +"&lon=" + longitude.toString() + "&per_page=1&format=json");
                    Log.i("the geolocation url", url.toString());
                }
                // sinon c'est juste l'url classique
                else {
                    url = new URL((String)strings[0]);
                }
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
            try {
                JSONArray items = jsonObject.getJSONArray("items");
                // pour chaque élément dans les items on construit un url et on l'ajoute à l'adaptateur
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String url = item.getJSONObject("media").getString("m");
                    adapter.dd(url);
                    Log.i("Mekk", "URL ajouté: " + url);
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        private String readStream(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            is.close();
            return sb.toString();
        }
    }
}
/*****************************************************************************************************************************************************/
// permettra de créer la file pour gérer les requêtes
class MySingleton {
    private static MySingleton instance;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private static Context ctx;

    private MySingleton(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();

        imageLoader = new ImageLoader(requestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized MySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new MySingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }
}