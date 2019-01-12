package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

public class DescompressaoActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DescompressaoActivity";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    public static Handler mHandler;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private TextView timeTextView;
    //    private long mTimeInMillis = 60 * 10000;
    private long mTimeInMillis = 10000;
    private CountDownTimer2 m2;
    private boolean pauseCounterOnce;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean bCheck = true;
    private boolean bFaceDown;
    private boolean bRain;
    private Weather weather;
    private String plText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descompressao);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        timeTextView = findViewById(R.id.tvTime);

        m2 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (ActivityCompat.checkSelfPermission(DescompressaoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
                Awareness.getSnapshotClient(DescompressaoActivity.this).getWeather()
                        .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
                            @Override
                            public void onSuccess(WeatherResponse weatherResponse) {
                                weather = weatherResponse.getWeather();
                                for (int condition : weather.getConditions()) {
                                    switch (condition) {
                                        case Weather.CONDITION_RAINY:
                                            bRain = true;
                                            Log.e(TAG, "weatherSnap: Rainning");
                                            break;
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "weatherSnap: Could not get Weather: " + e);
                                Toast.makeText(DescompressaoActivity.this, "Could not get Weather: " + e, Toast.LENGTH_SHORT).show();
                            }
                        });
                if (!bRain && bFaceDown && Singleton.getInstance().isFenceBool() && bCheck) {
                    if (m2.ismPaused()) m2.resume();
                    mTimeInMillis = millisUntilFinished;
                    pauseCounterOnce = false;
                } else if (!pauseCounterOnce) {
                    if (!bRain) printNearbyPlaces();
                    m2.pause();
                    pauseCounterOnce = true;
                }
                timeTextView.setText(updateCountDownText());
            }

            @Override
            public void onFinish() {
                timeTextView.setText("Finish");
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(500);
                Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
                Singleton.getInstance().setActivityKey("corridaKey");
                finish();
                startActivity(new Intent(DescompressaoActivity.this, PreambuloActivity.class));
            }
        }.start();

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String feedback = msg.getData().getString("FEEDBACK");
                if (feedback != null) Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
            }
        };
    }

    public void onCLickShowPreamb(MenuItem item) {
        bCheck = false;
        showDescription();
    }

    public void showDescription() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Descompressão");
        alert.setMessage(": O caloiro tem de estar sentado durante 10 minutos, no pátio do ed. A, com o telemóvel pousado no colo com o ecrã virado para baixo (é para relaxar!).");
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bCheck = true;
            }
        });
        alert.create().show();
    }

    public void showDialogNearbyLocation() {
        new AlertDialog.Builder(this)
                .setTitle("Atenção!")
                .setMessage("Foi detectado que não estao reunidas as melhores condições para terminares esta atividade no patio.\n" +
                        "Por favor dirija-se a um destes locais de interesse se assim quiser." +
                        plText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    private void printNearbyPlaces() {
        if (ContextCompat.checkSelfPermission(DescompressaoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(DescompressaoActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) Toast.makeText(this, "Error: high accuracy location mode must be enabled in the device.", Toast.LENGTH_LONG).show();
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(this, "Error: could not access location mode.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        Awareness.getSnapshotClient(this).getPlaces()
                .addOnSuccessListener(new OnSuccessListener<PlacesResponse>() {
                    @Override
                    public void onSuccess(PlacesResponse placesResponse) {
                        List<PlaceLikelihood> pll = placesResponse.getPlaceLikelihoods();
                        plText = "";
                        for (int i = 0; i < (pll.size() < 3 ? pll.size() : 3); i++) {
                            PlaceLikelihood pl = pll.get(i);
                            plText += "\t#" + i + ": " + pl.getPlace().toString() + "\t" + printPlaceTypes(pl.getPlace().getPlaceTypes()) + "\n";
//                            plText += "\t#" + i + ": " + pl.getPlace().getName().toString()
//                                    + "\n\tlikelihood: " + pl.getLikelihood()
//                                    + "\n\taddress: " + pl.getPlace().getAddress()
//                                    + "\n\tlocation: " + pl.getPlace().getLatLng()
//                                    + "\n\twebsite: " + pl.getPlace().getWebsiteUri()
//                                    + "\n\tplaceTypes: " + pl.getPlace().getPlaceTypes()
//                                    + "\t" + printPlaceTypes(pl.getPlace().getPlaceTypes()) + "\n";
                        }
                        showDialogNearbyLocation();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Could not get Places: " + e);
                        Toast.makeText(DescompressaoActivity.this, "Could not get Places: " + e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String printPlaceTypes(List<Integer> placeTypes) {
        String res = "";
        for (int placeType : placeTypes) {
            switch (placeType) {
                case Place.TYPE_ACCOUNTING  :   res += "TYPE_ACCOUNTING"; break;
                case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_1 : res += "TYPE_ADMINISTRATIVE_AREA_LEVEL_1"; break;
                case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_2 : res += "TYPE_ADMINISTRATIVE_AREA_LEVEL_2"; break;
                case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_3 : res += "TYPE_ADMINISTRATIVE_AREA_LEVEL_3"; break;
                case Place.TYPE_AIRPORT : res += "TYPE_AIRPORT"; break;
                case Place.TYPE_AMUSEMENT_PARK  : res += "TYPE_AMUSEMENT_PARK"; break;
                case Place.TYPE_AQUARIUM  : res += "TYPE_AQUARIUM"; break;
                case Place.TYPE_ART_GALLERY : res += "TYPE_ART_GALLERY"; break;
                case Place.TYPE_ATM : res += "TYPE_ATM"; break;
                case Place.TYPE_BAKERY  : res += "TYPE_BAKERY"; break;
                case Place.TYPE_BANK  : res += "TYPE_BANK"; break;
                case Place.TYPE_BAR : res += "TYPE_BAR"; break;
                case Place.TYPE_BEAUTY_SALON  : res += "TYPE_BEAUTY_SALON"; break;
                case Place.TYPE_BICYCLE_STORE : res += "TYPE_BICYCLE_STORE"; break;
                case Place.TYPE_BOOK_STORE  : res += "TYPE_BOOK_STORE"; break;
                case Place.TYPE_BOWLING_ALLEY : res += "TYPE_BOWLING_ALLEY"; break;
                case Place.TYPE_BUS_STATION : res += "TYPE_BUS_STATION"; break;
                case Place.TYPE_CAFE  : res += "TYPE_CAFE"; break;
                case Place.TYPE_CAMPGROUND  : res += "TYPE_CAMPGROUND"; break;
                case Place.TYPE_CAR_DEALER  : res += "TYPE_CAR_DEALER"; break;
                case Place.TYPE_CAR_RENTAL  : res += "TYPE_CAR_RENTAL"; break;
                case Place.TYPE_CAR_REPAIR  : res += "TYPE_CAR_REPAIR"; break;
                case Place.TYPE_CAR_WASH  : res += "TYPE_CAR_WASH"; break;
                case Place.TYPE_CASINO  : res += "TYPE_CASINO"; break;
                case Place.TYPE_CEMETERY  : res += "TYPE_CEMETERY"; break;
                case Place.TYPE_CHURCH  : res += "TYPE_CHURCH"; break;
                case Place.TYPE_CITY_HALL : res += "TYPE_CITY_HALL"; break;
                case Place.TYPE_CLOTHING_STORE  : res += "TYPE_CLOTHING_STORE"; break;
                case Place.TYPE_COLLOQUIAL_AREA : res += "TYPE_COLLOQUIAL_AREA"; break;
                case Place.TYPE_CONVENIENCE_STORE : res += "TYPE_CONVENIENCE_STORE"; break;
                case Place.TYPE_COUNTRY : res += "TYPE_COUNTRY"; break;
                case Place.TYPE_COURTHOUSE  : res += "TYPE_COURTHOUSE"; break;
                case Place.TYPE_DENTIST : res += "TYPE_DENTIST"; break;
                case Place.TYPE_DEPARTMENT_STORE  : res += "TYPE_DEPARTMENT_STORE"; break;
                case Place.TYPE_DOCTOR  : res += "TYPE_DOCTOR"; break;
                case Place.TYPE_ELECTRICIAN : res += "TYPE_ELECTRICIAN"; break;
                case Place.TYPE_ELECTRONICS_STORE : res += "TYPE_ELECTRONICS_STORE"; break;
                case Place.TYPE_EMBASSY : res += "TYPE_EMBASSY"; break;
                case Place.TYPE_ESTABLISHMENT : res += "TYPE_ESTABLISHMENT"; break;
                case Place.TYPE_FINANCE : res += "TYPE_FINANCE"; break;
                case Place.TYPE_FIRE_STATION  : res += "TYPE_FIRE_STATION"; break;
                case Place.TYPE_FLOOR : res += "TYPE_FLOOR"; break;
                case Place.TYPE_FLORIST : res += "TYPE_FLORIST"; break;
                case Place.TYPE_FOOD  : res += "TYPE_FOOD"; break;
                case Place.TYPE_FUNERAL_HOME  : res += "TYPE_FUNERAL_HOME"; break;
                case Place.TYPE_FURNITURE_STORE : res += "TYPE_FURNITURE_STORE"; break;
                case Place.TYPE_GAS_STATION : res += "TYPE_GAS_STATION"; break;
                case Place.TYPE_GENERAL_CONTRACTOR  : res += "TYPE_GENERAL_CONTRACTOR"; break;
                case Place.TYPE_GEOCODE : res += "TYPE_GEOCODE"; break;
                case Place.TYPE_GROCERY_OR_SUPERMARKET  : res += "TYPE_GROCERY_OR_SUPERMARKET"; break;
                case Place.TYPE_GYM : res += "TYPE_GYM"; break;
                case Place.TYPE_HAIR_CARE : res += "TYPE_HAIR_CARE"; break;
                case Place.TYPE_HARDWARE_STORE  : res += "TYPE_HARDWARE_STORE"; break;
                case Place.TYPE_HEALTH  : res += "TYPE_HEALTH"; break;
                case Place.TYPE_HINDU_TEMPLE  : res += "TYPE_HINDU_TEMPLE"; break;
                case Place.TYPE_HOME_GOODS_STORE  : res += "TYPE_HOME_GOODS_STORE"; break;
                case Place.TYPE_HOSPITAL  : res += "TYPE_HOSPITAL"; break;
                case Place.TYPE_INSURANCE_AGENCY  : res += "TYPE_INSURANCE_AGENCY"; break;
                case Place.TYPE_INTERSECTION  : res += "TYPE_INTERSECTION"; break;
                case Place.TYPE_JEWELRY_STORE : res += "TYPE_JEWELRY_STORE"; break;
                case Place.TYPE_LAUNDRY : res += "TYPE_LAUNDRY"; break;
                case Place.TYPE_LAWYER  : res += "TYPE_LAWYER"; break;
                case Place.TYPE_LIBRARY : res += "TYPE_LIBRARY"; break;
                case Place.TYPE_LIQUOR_STORE  : res += "TYPE_LIQUOR_STORE"; break;
                case Place.TYPE_LOCALITY  : res += "TYPE_LOCALITY"; break;
                case Place.TYPE_LOCAL_GOVERNMENT_OFFICE : res += "TYPE_LOCAL_GOVERNMENT_OFFICE"; break;
                case Place.TYPE_LOCKSMITH : res += "TYPE_LOCKSMITH"; break;
                case Place.TYPE_LODGING : res += "TYPE_LODGING"; break;
                case Place.TYPE_MEAL_DELIVERY : res += "TYPE_MEAL_DELIVERY"; break;
                case Place.TYPE_MEAL_TAKEAWAY : res += "TYPE_MEAL_TAKEAWAY"; break;
                case Place.TYPE_MOSQUE  : res += "TYPE_MOSQUE"; break;
                case Place.TYPE_MOVIE_RENTAL  : res += "TYPE_MOVIE_RENTAL"; break;
                case Place.TYPE_MOVIE_THEATER : res += "TYPE_MOVIE_THEATER"; break;
                case Place.TYPE_MOVING_COMPANY  : res += "TYPE_MOVING_COMPANY"; break;
                case Place.TYPE_MUSEUM  : res += "TYPE_MUSEUM"; break;
                case Place.TYPE_NATURAL_FEATURE : res += "TYPE_NATURAL_FEATURE"; break;
                case Place.TYPE_NEIGHBORHOOD  : res += "TYPE_NEIGHBORHOOD"; break;
                case Place.TYPE_NIGHT_CLUB  : res += "TYPE_NIGHT_CLUB"; break;
                case Place.TYPE_OTHER : res += "TYPE_OTHER"; break;
                case Place.TYPE_PAINTER : res += "TYPE_PAINTER"; break;
                case Place.TYPE_PARK  : res += "TYPE_PARK"; break;
                case Place.TYPE_PARKING : res += "TYPE_PARKING"; break;
                case Place.TYPE_PET_STORE : res += "TYPE_PET_STORE"; break;
                case Place.TYPE_PHARMACY  : res += "TYPE_PHARMACY"; break;
                case Place.TYPE_PHYSIOTHERAPIST : res += "TYPE_PHYSIOTHERAPIST"; break;
                case Place.TYPE_PLACE_OF_WORSHIP  : res += "TYPE_PLACE_OF_WORSHIP"; break;
                case Place.TYPE_PLUMBER : res += "TYPE_PLUMBER"; break;
                case Place.TYPE_POINT_OF_INTEREST : res += "TYPE_POINT_OF_INTEREST"; break;
                case Place.TYPE_POLICE  : res += "TYPE_POLICE"; break;
                case Place.TYPE_POLITICAL : res += "TYPE_POLITICAL"; break;
                case Place.TYPE_POSTAL_CODE : res += "TYPE_POSTAL_CODE"; break;
                case Place.TYPE_POSTAL_CODE_PREFIX  : res += "TYPE_POSTAL_CODE_PREFIX"; break;
                case Place.TYPE_POSTAL_TOWN : res += "TYPE_POSTAL_TOWN"; break;
                case Place.TYPE_POST_BOX  : res += "TYPE_POST_BOX"; break;
                case Place.TYPE_POST_OFFICE : res += "TYPE_POST_OFFICE"; break;
                case Place.TYPE_PREMISE : res += "TYPE_PREMISE"; break;
                case Place.TYPE_REAL_ESTATE_AGENCY  : res += "TYPE_REAL_ESTATE_AGENCY"; break;
                case Place.TYPE_RESTAURANT  : res += "TYPE_RESTAURANT"; break;
                case Place.TYPE_ROOFING_CONTRACTOR  : res += "TYPE_ROOFING_CONTRACTOR"; break;
                case Place.TYPE_ROOM  : res += "TYPE_ROOM"; break;
                case Place.TYPE_ROUTE : res += "TYPE_ROUTE"; break;
                case Place.TYPE_RV_PARK : res += "TYPE_RV_PARK"; break;
                case Place.TYPE_SCHOOL  : res += "TYPE_SCHOOL"; break;
                case Place.TYPE_SHOE_STORE  : res += "TYPE_SHOE_STORE"; break;
                case Place.TYPE_SHOPPING_MALL : res += "TYPE_SHOPPING_MALL"; break;
                case Place.TYPE_SPA : res += "TYPE_SPA"; break;
                case Place.TYPE_STADIUM : res += "TYPE_STADIUM"; break;
                case Place.TYPE_STORAGE : res += "TYPE_STORAGE"; break;
                case Place.TYPE_STORE : res += "TYPE_STORE"; break;
                case Place.TYPE_STREET_ADDRESS  : res += "TYPE_STREET_ADDRESS"; break;
                case Place.TYPE_SUBLOCALITY : res += "TYPE_SUBLOCALITY"; break;
                case Place.TYPE_SUBLOCALITY_LEVEL_1 : res += "TYPE_SUBLOCALITY_LEVEL_1"; break;
                case Place.TYPE_SUBLOCALITY_LEVEL_2 : res += "TYPE_SUBLOCALITY_LEVEL_2"; break;
                case Place.TYPE_SUBLOCALITY_LEVEL_3 : res += "TYPE_SUBLOCALITY_LEVEL_3"; break;
                case Place.TYPE_SUBLOCALITY_LEVEL_4 : res += "TYPE_SUBLOCALITY_LEVEL_4"; break;
                case Place.TYPE_SUBLOCALITY_LEVEL_5 : res += "TYPE_SUBLOCALITY_LEVEL_5"; break;
                case Place.TYPE_SUBPREMISE  : res += "TYPE_SUBPREMISE"; break;
                case Place.TYPE_SUBWAY_STATION  : res += "TYPE_SUBWAY_STATION"; break;
                case Place.TYPE_SYNAGOGUE : res += "TYPE_SYNAGOGUE"; break;
                case Place.TYPE_SYNTHETIC_GEOCODE : res += "TYPE_SYNTHETIC_GEOCODE"; break;
                case Place.TYPE_TAXI_STAND  : res += "TYPE_TAXI_STAND"; break;
                case Place.TYPE_TRAIN_STATION : res += "TYPE_TRAIN_STATION"; break;
                case Place.TYPE_TRANSIT_STATION : res += "TYPE_TRANSIT_STATION"; break;
                case Place.TYPE_TRAVEL_AGENCY : res += "TYPE_TRAVEL_AGENCY"; break;
                case Place.TYPE_UNIVERSITY  : res += "TYPE_UNIVERSITY"; break;
                case Place.TYPE_VETERINARY_CARE : res += "TYPE_VETERINARY_CARE"; break;
                case Place.TYPE_ZOO : res += "TYPE_ZOO"; break;
            }
            res += "\t";
        }
        return res;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        if (hours > 0) timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        else timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        return timeLeftFormatted;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        bFaceDown = sensorEvent.values[2] < 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Toast.makeText(DescompressaoActivity.this, sensor.getName() + "accuracy changed to " + accuracy, Toast.LENGTH_SHORT).show();
    }

    protected void removeFences(String unique_key) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(unique_key)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\nFences were successfully removed.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\nFences could not be removed: " + e.getMessage());
                    }
                });
    }

    protected void queryFences() {
        Awareness.getFenceClient(this).queryFences(FenceQueryRequest.all())
                .addOnSuccessListener(new OnSuccessListener<FenceQueryResponse>() {
                    @Override
                    public void onSuccess(FenceQueryResponse fenceQueryResponse) {
                        String fenceInfo = "";
                        FenceStateMap fenceStateMap = fenceQueryResponse.getFenceStateMap();
                        for (String fenceKey : fenceStateMap.getFenceKeys()) {
                            int state = fenceStateMap.getFenceState(fenceKey).getCurrentState();
                            fenceInfo += fenceKey + ": " + (state == FenceState.TRUE ? "TRUE" : state == FenceState.FALSE ? "FALSE" : "UNKNOWN") + "\n";
                            if (fenceKey.equals("locationFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setFenceBool(true);
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n" + "> Fences' states:\n" + (fenceInfo.equals("") ? "No registered fences." : fenceInfo);
                        Log.d(TAG, text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n" + "Fences could not be queried: " + e.getMessage();
                        Log.d(TAG, text);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back button pressed");
        showDialogWaring();
    }

    public void showDialogWaring() {
        new AlertDialog.Builder(this)
                .setTitle("Sair Tarefa")
                .setMessage("Caloiro tem a certeza que pretende sair!\n Qualquer progresso que tenha feito ira ser perdido")
                .setPositiveButton("Terminar Tarefa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m2.cancel();
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        queryFences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mSensorManager.unregisterListener(this);
        queryFences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        queryFences();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }
}
