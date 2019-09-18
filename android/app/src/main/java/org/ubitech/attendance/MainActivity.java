package org.ubitech.attendance;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

import io.flutter.Log;
import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import android.os.Bundle;
import io.flutter.app.FlutterActivity;
import io.flutter.plugins.GeneratedPluginRegistrant;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import android.hardware.Camera;

import androidx.core.app.ActivityCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import io.flutter.Log;
import io.flutter.app.FlutterActivity;
import io.flutter.plugins.GeneratedPluginRegistrant;
public class MainActivity extends FlutterActivity implements LocationAssistant.Listener{

  private LocationAssistant assistant;
  private static final String CHANNEL = "location.spoofing.check";
  private static final String CAMERA_CHANNEL = "update.camera.status";
  private boolean cameraOpened=false;


  private SettingsClient mSettingsClient;
  private LocationSettingsRequest mLocationSettingsRequest;
  private static final int REQUEST_CHECK_SETTINGS = 214;
  private static final int REQUEST_ENABLE_GPS = 516;


  MethodChannel channel;
  LocationListenerExecuter listenerExecuter;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i("Dialog","hdghdgjdgjdgdjgdjgdjggggggg");
  //showLocationDialog();



    if (android.os.Build.VERSION.SDK_INT > 9)
    {
      StrictMode.ThreadPolicy policy = new
              StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
    }
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

    channel=new MethodChannel(getFlutterView(), CHANNEL);
    GeneratedPluginRegistrant.registerWith(this);
    listenerExecuter=new LocationListenerExecuter(channel,this);
    listenerExecuter.execute("");
      Timer timer = new Timer();

      new MethodChannel(getFlutterView(), CAMERA_CHANNEL).setMethodCallHandler(
            new MethodCallHandler() {
              @Override
              public void onMethodCall(MethodCall call, Result result) {
                if (call.method.equals("cameraOpened")) {
                  cameraOpened=true;
                  Log.i("camera","camera opened true");
                  if(listenerExecuter!=null)
                 listenerExecuter.updateCameraStatus(true);
                }
                else
                if (call.method.equals("cameraClosed")) {
                  Log.i("camera","camera opened false");
                  cameraOpened=false;
                  if(listenerExecuter!=null)
                  listenerExecuter.updateCameraStatus(false);
                }
                else
                if (call.method.equals("startAssistant")) {
                  Log.i("Assistant","Assistant Start Called");

                  manuallyStartAssistant();
                }
                if (call.method.equals("startTimeOutNotificationWorker")) {
                  // Log.i("Assistant","Assistant Start Called");
                  WorkManager.getInstance().cancelAllWorkByTag("TimeInWork");// Cancel time in work if scheduled previously
                  String ShiftTimeOut = call.argument("ShiftTimeOut");
                  Log.i("ShiftTimeout",ShiftTimeOut);
                  startTimeOutNotificationWorker(ShiftTimeOut);
                }
                if (call.method.equals("startTimeInNotificationWorker")) {
                  // Log.i("Assistant","Assistant Start Called");
                  WorkManager.getInstance().cancelAllWorkByTag("TimeOutWork");// Cancel time out work if scheduled previously
                  String ShiftTimeIn = call.argument("ShiftTimeIn");
                    String nextWorkingDay = call.argument("nextWorkingDay");
                  Log.i("nextWorkingDay",nextWorkingDay);
                  startTimeInNotificationWorker(ShiftTimeIn,nextWorkingDay);

                }
                if (call.method.equals("openLocationDialog")) {
                  openLocationDialog();

                }

              }
            });



  }


  public void openLocationDialog(){


    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY));
    builder.setAlwaysShow(true);
    mLocationSettingsRequest = builder.build();

    mSettingsClient = LocationServices.getSettingsClient(MainActivity.this);

    mSettingsClient
            .checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
              @Override
              public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Success Perform Task Here
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                  case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    try {
                      ResolvableApiException rae = (ResolvableApiException) e;
                      rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sie) {
                      Log.e("GPS","Unable to execute request.");
                    }
                    break;
                  case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    Log.e("GPS","Location settings are inadequate, and cannot be fixed here. Fix in Settings.");
                }
              }
            })
            .addOnCanceledListener(new OnCanceledListener() {
              @Override
              public void onCanceled() {
                Log.e("GPS","checkLocationSettings -> onCanceled");
              }
            });



  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CHECK_SETTINGS) {
      switch (resultCode) {
        case Activity.RESULT_OK:
          //Success Perform Task Here
          manuallyStartAssistant();
          break;
        case Activity.RESULT_CANCELED:
          Log.e("GPS","User denied to access location");
          openLocationDialog();
          break;
      }
    } else if (requestCode == REQUEST_ENABLE_GPS) {
      LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

      if (!isGpsEnabled) {
        openLocationDialog();
      } else {
        //navigateToUser();
       // manuallyStartAssistant();
      }
    }
  }

  private void openGpsEnableSetting() {
    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    startActivityForResult(intent, REQUEST_ENABLE_GPS);
  }



  public void startTimeOutNotificationWorker(String ShiftTimeOut){
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    String currentTime=sdf.format(cal.getTime());
    Log.i("DateShashank",currentTime+"");

    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    Date date1 = null,date2=null;
    long minutes=0;
    try {
      date1 = format.parse(ShiftTimeOut);
      date2 = format.parse(currentTime);
      long differenceinMilli = date1.getTime()- date2.getTime();
      minutes = TimeUnit.MILLISECONDS.toMinutes(differenceinMilli);
      Log.i("differenceinMilli",differenceinMilli+"");
      Log.i("minutes",minutes+"");
      if(minutes<0){
        minutes=0;
      }
      else{
        minutes=minutes+5;
      }
    } catch (ParseException e) {
      Log.i("TimeError","Time not correct when calculating worker interval");
      e.printStackTrace();
    }


Log.i("WorkerMinutesForTimeOut",minutes+"");
  final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TimeOutNotificationWork.class)
          .setInitialDelay(minutes, TimeUnit.MINUTES)
          .addTag("TimeOutWork")
          .build();
  WorkManager.getInstance().enqueue(workRequest);
}


  public void startTimeInNotificationWorker(String ShiftTimeIn,String nextWorkingDay){
    Calendar cal = Calendar.getInstance();
    Log.i("nextWorkingday",nextWorkingDay);
      String dateStart = nextWorkingDay+" "+ShiftTimeIn;
      String dateStop = "01/15/2012 10:31:48";

      //HH converts hour in 24 hours format (0-23), day calculation
      SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

      Date d1 = null;
      Date d2 = null;
      long diffMinutes=0;
      try {
          d1 = format.parse(dateStart);
          d2 =  new Date(System.currentTimeMillis());

          //in milliseconds
          long diff =  d1.getTime()-d2.getTime() ;
           Log.i("diff",diff+"");
          long diffSeconds = diff / 1000 % 60;
          diffMinutes = diff / (60 * 1000) % 60;
          long diffHours = diff / (60 * 60 * 1000) % 24;
          long diffDays = diff / (24 * 60 * 60 * 1000);
         diffMinutes = diffMinutes+diffDays*1440+diffHours*60;
          if(diffMinutes<0){
            diffMinutes=0;
          }
          else{
            diffMinutes=diffMinutes+5;
          }

      } catch (Exception e) {
          e.printStackTrace();
      }














    Log.i("WorkerMinutesForTimeIn",diffMinutes+"");
    final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TimeInNotificationWork.class)
            .setInitialDelay(diffMinutes, TimeUnit.MINUTES)
            .addTag("TimeInWork")
            .build()
            ;



      WorkManager w=WorkManager.getInstance();
      w.enqueueUniqueWork("TimeInNotificationWork", ExistingWorkPolicy.KEEP,workRequest);

  }


  public void manuallyStartAssistant(){
    if(listenerExecuter!=null)
    listenerExecuter.manuallyStartAssistant();
  }
  @Override
  public void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
    //  tvLocation.setText(getString(R.string.mockLocationMessage));
    //  tvLocation.setOnClickListener(fromView);
   // channel.invokeMethod("message", "Location is mocked");
  }
  @Override
  public void onDestroy() {
    if(listenerExecuter!=null)
    listenerExecuter.onDestroy();

    super.onDestroy();

  }
  @Override
  protected void onResume() {
    super.onResume();
   if(!cameraOpened)
     if(listenerExecuter!=null)
    listenerExecuter.startAssistant();
   // assistant.start();
  }

  @Override
  protected void onPause() {
   // assistant.stop();
  //if(!cameraOpened)
    if(listenerExecuter!=null)
    listenerExecuter.stopAssistant();
    super.onPause();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if(permissions!=null&&permissions.length>0) {
      for (int i = 0; i < permissions.length; i++) {


        if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)&&listenerExecuter!=null) {
          Log.i("Peeeerrrr", requestCode + "detected");
          if (listenerExecuter.onPermissionsUpdated(requestCode, grantResults)) ;

        }


      }
    }

  // Log.i("Perrrrr",permissions[1]+grantResults);

  }
/*
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    assistant.onActivityResult(requestCode, resultCode);
  }
*/
  @Override
  public void onNeedLocationPermission() {
    //    tvLocation.setText("Need\nPermission");
    /*    tvLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assistant.requestLocationPermission();
        });*/
    if(listenerExecuter!=null)
   listenerExecuter.requestLocationPermission();
    if(listenerExecuter!=null)
    listenerExecuter.requestAndPossiblyExplainLocationPermission();
  }

  @Override
  public void onExplainLocationPermission() {
/*
    new AlertDialog.Builder(this)
            .setMessage(R.string.permissionExplanation)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                assistant.requestLocationPermission();
              }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                       /* tvLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                assistant.requestLocationPermission();
                            }
                        });*/
             /* }
            })
            .show();*/
  }

  @Override
  public void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView,
                                                      DialogInterface.OnClickListener fromDialog) {
   /* new AlertDialog.Builder(this)
            .setMessage(R.string.permissionPermanentlyDeclined)
            .setPositiveButton(R.string.ok, fromDialog)
            .show();*/
  }

  @Override
  public void onNeedLocationSettingsChange() {
    /*
    new AlertDialog.Builder(this)
            .setMessage(R.string.switchOnLocationShort)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                assistant.changeLocationSettings();
              }
            })
            .show();
            */
  }

  @Override
  public void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
    /*
    new AlertDialog.Builder(this)
            .setMessage(R.string.switchOnLocationLong)
            .setPositiveButton(R.string.ok, fromDialog)
            .show();
            */
  }

  @Override
  public void onNewLocationAvailable(Location location) {
    //    if (location == null) return;
    //    tvLocation.setOnClickListener(null);
    //    tvLocation.setText(location.getLongitude() + "\n" + location.getLatitude());
    // tvLocation.setAlpha(1.0f);
    // tvLocation.animate().alpha(0.5f).setDuration(400);
  }



  @Override
  public void onError(LocationAssistant.ErrorType type, String message) {
    // tvLocation.setText(getString(R.string.error));
  }

}
