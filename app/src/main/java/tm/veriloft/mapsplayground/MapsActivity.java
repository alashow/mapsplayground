/*
 * Copyright 2015. Alashov Berkeli
 *
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tm.veriloft.mapsplayground;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private ArrayList<Polyline> drawnRoutes = new ArrayList<>(); //all drawn routes in the map, for removing after
    private ArrayList<Marker> addedMarkers = new ArrayList<>(); //all markers in the map, for removing after

    private final String SERVER = "http://router.project-osrm.org/viaroute";
    private final int SERVER_PORT = 80;

    private final String[] drawOptionsStrings = {"Focus to start point after draw", "Add end of point marker", "Add requested point marker", "Markers for returned points"};
    private final boolean[] drawOptionsValues = {false, true, true, false};

    private boolean focusAfterRouteDraw = drawOptionsValues[0];
    private boolean endMarkerRouteDraw = drawOptionsValues[1];
    private boolean requestedPointMarkerRouteDraw = drawOptionsValues[2];
    private boolean returnedPointsMarkers = drawOptionsValues[3];

    private final String[] mapTypeStrings = {"Normal", "Hybrid", "Satellite", "Terrain"};
    private int lastMapType = 1;

    private final int ROUTE_WIDTH = 5;
    private final int ROUTE_COLOR = Color.RED;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {

        switch (item.getItemId()) {
            case R.id.map_routes_clear:

                //clear them from map
                for(Polyline drawnRoute : drawnRoutes)
                    drawnRoute.remove();
                for(Marker marker : addedMarkers)
                    marker.remove();

                //clear array
                drawnRoutes.clear();
                addedMarkers.clear();

                break;
            case R.id.map_type:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                alertDialogBuilder.setTitle("Choose map type");

                alertDialogBuilder.setSingleChoiceItems(mapTypeStrings, lastMapType, new DialogInterface.OnClickListener() {
                    @Override public void onClick( DialogInterface dialog, int which ) {
                        dialog.dismiss();

                        if (which == lastMapType)
                            return;

                        lastMapType = which;

                        switch (which) {
                            case 0:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                break;
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 3:
                                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                break;
                        }
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", null);
                alertDialogBuilder.show();
                break;
        }
        return true;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setMyLocationEnabled(true);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override public void onMapLongClick( final LatLng latLng ) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                alertDialogBuilder.setTitle("Confirm Route Drawing");

                alertDialogBuilder.setMultiChoiceItems(drawOptionsStrings, drawOptionsValues, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int which, boolean isChecked ) {
                        drawOptionsValues[which] = isChecked;
                        setDrawOptionsValues();
                    }
                });

                alertDialogBuilder.setNegativeButton("Nope", null);
                alertDialogBuilder.setPositiveButton("Yes, of coz", new DialogInterface.OnClickListener() {
                    @Override public void onClick( DialogInterface dialog, int which ) {
                        Location location = mMap.getMyLocation();

                        if (location != null) {
                            fetchAndDrawRoute(new LatLng(location.getLatitude(), location.getLongitude()), latLng);
                        } else {
                            showCenteredToast("Your current location currently not available, please wait.");
                        }
                    }
                });
                alertDialogBuilder.show();
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override public void onMapClick( LatLng latLng ) {
                l("onMapClick, " + latLng.toString());
            }
        });

        focusTo(new LatLng(37.945958662069174, 58.38305085897446), 13); // focus to ashgabat
    }

    private void focusTo( LatLng latLng, float zoomLevel ) {
        CameraPosition cameraPosition = CameraPosition.builder().zoom(zoomLevel).target(latLng).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void setDrawOptionsValues() {
        focusAfterRouteDraw = drawOptionsValues[0];
        endMarkerRouteDraw = drawOptionsValues[1];
        requestedPointMarkerRouteDraw = drawOptionsValues[2];
        returnedPointsMarkers = drawOptionsValues[3];
    }

    private void fetchAndDrawRoute( final LatLng... latLngs ) {
        final AsyncHttpClient asyncHttpClient = new AsyncHttpClient(SERVER_PORT);

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override public void onClick( DialogInterface dialog, int which ) {
                dialog.cancel();
            }
        });

        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel( DialogInterface dialog ) {
                asyncHttpClient.cancelRequests(MapsActivity.this, true);
                showCenteredToast("Drawing route got cancelled");
            }
        });

        final RequestParams requestParams = new RequestParams();

        for(LatLng latLng : latLngs) {
            requestParams.add("loc", latLng.latitude + "," + latLng.longitude);
            l("Added latLng to request, " + latLng);
        }

        asyncHttpClient.get(this, SERVER, requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onSuccess( int statusCode, Header[] headers, JSONObject response ) {
                try {
                    ArrayList<LatLng> route = MapUtils.decodePoly(response.getString("route_geometry"));
                    PolylineOptions routeOptions = new PolylineOptions();

                    for(int i = 0; i < route.size(); i++) {
                        routeOptions.add(route.get(i));
                        l("Returned point " + route.get(i));
                        if (returnedPointsMarkers)
                            addedMarkers.add(mMap.addMarker(new MarkerOptions().position(route.get(i))
                                .title("#" + i + " point of #" + drawnRoutes.size())));
                    }

                    routeOptions.width(ROUTE_WIDTH).color(ROUTE_COLOR);
                    drawnRoutes.add(mMap.addPolyline(routeOptions));

                    List<LatLng> points = routeOptions.getPoints();

                    if (focusAfterRouteDraw)
                        focusTo(points.get(0), 13);

                    if (endMarkerRouteDraw) {
                        LatLng trueEndPoint = MapUtils.findTrueEndPoint(latLngs[latLngs.length - 1], route.get(0), route.get(route.size() - 1));
                        addedMarkers.add(mMap.addMarker(new MarkerOptions().position(trueEndPoint)
                            .title("End of #" + drawnRoutes.size())));
                    }

                    if (requestedPointMarkerRouteDraw)
                        addedMarkers.add(mMap.addMarker(new MarkerOptions().position(latLngs[latLngs.length - 1])
                            .title("Requested point of #" + drawnRoutes.size())));

                } catch (JSONException exception) {
                    exception.printStackTrace();
                    showCenteredToast("Exception while parsing! Error: " + exception.getLocalizedMessage());
                }
            }

            @Override
            public void onFailure( int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse ) {
                showCenteredToast("Network error! Error: " + throwable.getLocalizedMessage());
            }

            @Override
            public void onFailure( int statusCode, Header[] headers, String responseString, Throwable throwable ) {
                showCenteredToast("Network error! Server returned non-json response, response: " + responseString + ", Error: " + throwable.getLocalizedMessage());
            }

            @Override
            public void onFailure( int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse ) {
                showCenteredToast("Network error! Error: " + throwable.getLocalizedMessage());
            }

            @Override public void onFinish() {
                progressDialog.dismiss();
            }
        });
    }

    public void showCenteredToast( String string ) {
        if (string == null || string.equals(""))
            string = "null";
        Toast toast = Toast.makeText(this.getApplicationContext(), string, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    void l( String l ) {
        Log.d("MapPlayground", l);
    }
}
