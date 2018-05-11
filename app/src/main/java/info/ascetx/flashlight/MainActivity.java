package info.ascetx.flashlight;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import info.ascetx.flashlight.util.IabHelper;
import info.ascetx.flashlight.util.IabResult;
import info.ascetx.flashlight.util.Inventory;
import info.ascetx.flashlight.util.Purchase;

import static info.ascetx.flashlight.util.IabHelper.BILLING_RESPONSE_RESULT_OK;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    //    private ImageButton btnSwitch;
    // define the display assembly compass picture
    private ImageView image, btnSwitch, btnSos, btnRemAd;
    private FirebaseAnalytics mFirebaseAnalytics;
    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;
    private Button flashFreq;
    private Button [] freq;
    private TextView tvHeading,tvCompassError;
    private Camera camera;
    private boolean isFlashOn;
    private boolean isFlashSOSOn;
    private boolean hasFlash;
    private boolean enableDebugFL;
    private Parameters params;
    private MediaPlayer mp;

    private static Timer t;
    private Timer showInterstitialAd;
    private boolean isOn = false;
    private boolean isOnPause = false;
    private boolean pauseIntAd = false;
    private boolean pauseBilling = false;
    private static int selectedFlashRate = 0;
    private static int flashCount = 0;
    private ArrayList<Integer> morseArray;
    private ArrayList<String> skuList;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    public static final int REQUEST_CODE = 1001;
    private IInAppBillingService mService;
    private ServiceConnection mServiceConn;
    private IabHelper mHelper;
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        final Activity mActivity = this;

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

//        MobileAds.initialize(this,
//                "ca-app-pub-3940256099942544~3347511713");

        MobileAds.initialize(this,
                "ca-app-pub-3752168151808074~1222126147");

        mInterstitialAd = new InterstitialAd(this);
//        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        mInterstitialAd.setAdUnitId("ca-app-pub-3752168151808074/8311769360");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

        showInterstitialAd = new Timer();
        showInterstitialAd.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (mInterstitialAd.isLoaded() && !isOnPause){
                            mInterstitialAd.show();
                            pauseIntAd = true;
                        }
                    }
                });
            }
        },0, 40000 );

//        Attempt to resolve: java.lang.RuntimeException: Camera is being used after Camera.release() was called
        getCamera();

        final IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
                new IabHelper.OnConsumeFinishedListener() {
                    public void onConsumeFinished(Purchase purchase, IabResult result) {
                        if (result.isSuccess()) {
                            // provision the in-app purchase to the user
                            // (for example, credit 50 gold coins to player's character)
                            logError("Consumed success");
                        }
                        else {
                            // handle error
                            logError("Consume error");
                        }
                    }
                };

        // ...
        // TODO Split the public into two and encode both with separate hashing technique and query the encoded keys in app then decode each and join.
        // Reason is that the key changes in play store when ever it likes to change, mostly when a new app is uploaded.
        // If the key is taken from server we have the power to change it without making changes in app and publishing it.
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuL+OTWzTlbpHUg1hcIdLocokjNd2fvmAwxCnb6R7TTx4dzZJVNu2jcV2FNoGgXL+yfR3S4YLC1o9+kEY5zi1yc1S58VjD3KrXpX11OHd5RI/5PwpQ/6HQQCqINNF9PwIIhwMPxpkI3bqd8aBZWWQ9YyCKJ9hCpaorDmFEe0QCpDt3RJ6C3c8V8WKmBRHEBWoEA7EocqQjbxUafOSSUV27vLgVInVgi7O701JUjr4apmfqiIuitVZrK/TNfz0Yh2o7K7Rfz9fAPbSClDPn4YF/lKdco5pb9/szDE5Jldbp4jjZMFFdganX8XFxm1ncrzSsFEpqukegHCf73xJ4xlUuwIDAQAB";

        // compute your public key and store it in base64EncodedPublicKey
        mHelper = new IabHelper(this, base64EncodedPublicKey);

//        TODO to enable/disable IAB and FL loggers
        mHelper.enableDebugLogging(false);
        enableDebugFL = false;

        final IabHelper.QueryInventoryFinishedListener mGotInventoryListener
                = new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result,
                                                 Inventory inventory) {
                logDebug("Querying purchased items: " + result+ " inventory: "+inventory);
                if (result.isFailure()) {
                    // handle error here
                    logError("Query purchase failed");

                }
                else {
                    boolean mIsPremium;
                    logError("Query purchase pass");
                    // does the user have the premium upgrade?
                    mIsPremium = inventory.hasPurchase("no_ads");
                    logError("mIsPremium: "+ mIsPremium);
                    // update UI accordingly
                    if(mIsPremium)
                        hideAd();

                    /*try {
                        mHelper.consumeAsync(inventory.getPurchase("no_ads"),
                                mConsumeFinishedListener);
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        e.printStackTrace();
                    }*/
                }
            }
        };

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh no, there was a problem.
                    logDebug("Problem setting up In-app Billing: " + result);
                }
                // Hooray, IAB is fully set up!
                logDebug("Hooray, IAB is fully set up!: " + result);
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });

        mPurchaseFinishedListener
                = new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase)
            {
                logError("purchase: "+purchase);
                if (result.isFailure()) {
                    logDebug("Error purchasing: " + result);
                    if(purchase != null)
                        logError("purchase.getSku().equals(\"no_ads\"): "+purchase.getSku().equals("no_ads"));
                    return;
                }
                else if (purchase.getSku().equals("no_ads")) {
                    // consume the gas and update the UI
                    logDebug("Purchased no ads and hide Ad: " + result);
                    hideAd();
                }
//                else if (purchase.getSku().equals(SKU_PREMIUM)) {
//                    // give user access to premium content and update the UI
//                }
            }
        };

/*********************** Starting connection to iap *****************************
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
                new GetItemList(getPackageName()).execute();
            }
        };
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
/************************ END - Starting connection to iap *****************************/

        image = (ImageView) findViewById(R.id.imageViewCompass);
        btnSos = (ImageView) findViewById(R.id.fabBtnSos);
        btnRemAd = (ImageView) findViewById(R.id.fabBtnRemAd);

//        // Create a new ImageView
//        ImageView image = new ImageView(this);
        // Set the background color to white
//        image.setBackgroundColor(Color.WHITE);
        // Parse the SVG file from the resource
//        SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.compass2);
//        // Get a drawable from the parsed SVG and set it as the drawable for the ImageView
//        image.setImageDrawable(svg.createPictureDrawable());
//        // Set the ImageView as the content view for the Activity
//        setContentView(image);

        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        // TextView to show if compass works for the device or not
        tvCompassError = (TextView) findViewById(R.id.tvCompassError);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // for the system's orientation sensor registered listeners
        if(mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME)){
            tvCompassError.setVisibility(View.GONE);
        }else{
            tvHeading.setText("");
        }

        logError("OnCreate:mSensorManager.registerListener: "+mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME));
        // flash switch button
        btnSwitch = (ImageView) findViewById(R.id.btnSwitch);

        freq = new Button[10];

        freq[0] = (Button) findViewById(R.id.freq0);
        freq[1] = (Button) findViewById(R.id.freq1);
        freq[2] = (Button) findViewById(R.id.freq2);
        freq[3] = (Button) findViewById(R.id.freq3);
        freq[4] = (Button) findViewById(R.id.freq4);
        freq[5] = (Button) findViewById(R.id.freq5);
        freq[6] = (Button) findViewById(R.id.freq6);
        freq[7] = (Button) findViewById(R.id.freq7);
        freq[8] = (Button) findViewById(R.id.freq8);
        freq[9] = (Button) findViewById(R.id.freq9);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*
        * First check if device is supporting flashlight or not
        */
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            // device doesn't support flash
            // Show alert message and close the application
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                    .create();
            alert.setTitle("Error");
            alert.setMessage("Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // closing the application
                    finish();
                }
            });
            alert.show();
            return;
        }

        logError("Is flash ON? " + String.valueOf(isFlashOn));

        if (ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }

        // get the camera
        getCamera();

        // displaying button image
        toggleButtonImage();


        /*
        * Switch click event to toggle flash on/off
        */
        btnSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    if (t != null) {
                        logError("Turn flash off and async task too at init");
                        turnOffFlashAsync();
                    }
                } else {
                    logError("Request for Camera permission: "+ContextCompat.checkSelfPermission(mActivity,
                            Manifest.permission.CAMERA));
                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(mActivity,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {

                        // Should we show an explanation?
//                        if (ActivityCompat.shouldShowRequestPermissionRationale(getApplicationContext(),
//                                Manifest.permission.READ_CONTACTS)) {

                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

//                        } else {

                            // No explanation needed, we can request the permission.

                            ActivityCompat.requestPermissions(mActivity,
                                    new String[]{Manifest.permission.CAMERA},
                                    MY_PERMISSIONS_REQUEST_CAMERA);

                            // MY_PERMISSIONS_REQUEST_CAMERA is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
//                        }
                    } else {
                        if(t != null)
                            turnOffFlashAsync();
                        logError("Turn flash ON and async task too at init");
                        flashFrequency(freq[selectedFlashRate]);
                    }
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        logError("requestCode after alert for CAMERA permission: "+requestCode);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getCamera();
                    if(t != null)
                        turnOffFlashAsync();
                    logError("Turn flash ON and async task too at init");
                    flashFrequency(freq[selectedFlashRate]);

                } else {
                    if(t != null)
                        turnOffFlashAsync();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void hideAd(){
        mAdView.setVisibility(View.GONE);
        mInterstitialAd = null;
        showInterstitialAd.cancel();
    }

/*********************** The naked async class for iap service ******************************************
    /**private class GetItemList extends AsyncTask<Integer, Integer, Long> {

        private String pName;

        GetItemList(String packagename){
            pName = packagename;
        }

        @Override
        protected Long doInBackground(Integer... params) {
//            Querying for items available for purchase
            ArrayList<String> skuList = new ArrayList<String> ();
            skuList.add("no_ads");
            skuList.add("android.test.canceled");
            skuList.add("android.test.refunded");
            skuList.add("android.test.item_unavailable");
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
            Bundle skuDetails = null;
            try {
/*//*********************** Querying for items available for purchase ******************************************
                skuDetails = mService.getSkuDetails(3, pName, "inapp", querySkus);
                int response = skuDetails.getInt("RESPONSE_CODE");
                logError("Querying for items available for purchase, Response Code: " + response);
                if (response == 0) {
                    ArrayList<String> responseList
                            = skuDetails.getStringArrayList("DETAILS_LIST");
                    logError(String.valueOf(responseList));
                    for (String thisResponse : responseList) {
                        JSONObject object;
                        object = new JSONObject(thisResponse);
                        String sku = object.getString("productId");
                        String price = object.getString("price");
                        String mFirstIntermediate;
                        String mSecondIntermediate;
                        if (sku.equals("no_ads")) mFirstIntermediate = price;
//                        else if (sku.equals("i002")) mSecondIntermediate = price;
//                        pView.setText(sku + ": " + price);
                    }
                }
/*//*********************** Querying for purchased items ******************************************
                Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
                response = ownedItems.getInt("RESPONSE_CODE");
                logError("ownedItems response: "+ response);
                if (response == BILLING_RESPONSE_RESULT_OK) {
                    ArrayList<String> ownedSkus =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    ArrayList<String>  purchaseDataList =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    ArrayList<String>  signatureList =
                            ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                    String continuationToken =
                            ownedItems.getString("INAPP_CONTINUATION_TOKEN");

                    for (int i = 0; i < purchaseDataList.size(); ++i) {
                        String purchaseData = purchaseDataList.get(i);
                        String signature = signatureList.get(i);
                        String sku = ownedSkus.get(i);

                        logError("ownedItems: " + sku +" "+purchaseData+" sign: "+signature);
                        // do something with this purchase information
                        // e.g. display the updated list of products owned by user
                    }

                    // if continuationToken != null, call getPurchases again
                    // and pass in the token to retrieve more items
                }
/*//********** Consuming a purchase, using purchaseToken which is got in product data. Used for consuming coins in games ******************************************

                response = mService.consumePurchase(3, getPackageName(), "inapp:info.ascetx.flashlight:no_ads");
                logError("consume purchase response: "+ response);

            } catch (NullPointerException ne)  {
                logDebug("Error Null Pointer: " + ne.getMessage());
                ne.printStackTrace();
            }
            catch (RemoteException e) {
                // TODO Auto-generated catch block
                logDebug("Error Remote: " + e.getMessage());
                e.printStackTrace();
            }
            catch (JSONException je) {
                // TODO Auto-generated catch block
                logDebug("Error JSON: " + je.getMessage());
                je.printStackTrace();
            }
            return null;
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        logDebug("onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            logDebug("onActivityResult handled by IABUtil.");
        }

/******************* Naked code for handling data after purchase  ********************************************
//        Google Play sends a response to your PendingIntent to the onActivityResult method of your application.
//        The onActivityResult method has a result code of Activity.RESULT_OK (1) or Activity.RESULT_CANCELED (0)
        logError("onActivityResult: Google Play sends a response to your PendingIntent to the onActivityResult method ");
        if (requestCode == REQUEST_CODE) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            logError("purchaseData: "+purchaseData);
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    logError("You have bought the " + sku + ". Excellent choice,adventurer!");
                }
                catch (JSONException e) {
                    logError("Failed to parse purchase data.");
                    e.printStackTrace();
                }
            }
        }*/
    }

    public void removeAd(View view){
        pauseBilling = true;
        try {
            mHelper.launchPurchaseFlow(this, "no_ads", 1001,
                    mPurchaseFinishedListener, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
/************ Naked code for starting purchase intent ********************************************
//      Purchasing an item
        Bundle buyIntentBundle = null;
        try {
//            To start a purchase request from your app, call the getBuyIntent method on the In-app Billing service
            buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                    "no_ads", "inapp", "devPayLoad");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        logError("buy Intent Response: "+ buyIntentBundle.getInt("RESPONSE_CODE"));
//        The next step is to extract a PendingIntent from the response Bundle with key BUY_INTENT, as shown here:
        PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
//        BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED	7	Failure to purchase since item is already owned

//        To complete the purchase transaction, call the startIntentSenderForResult method and use the
//        PendingIntent that you created. This example uses an arbitrary value of 1001 for the request code:
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }

//        Google Play sends a response to your PendingIntent to the onActivityResult method of your application.
//        The onActivityResult method has a result code of Activity.RESULT_OK (1) or Activity.RESULT_CANCELED (0)
 */
    }

    private void turnOffFlashSOS(){
        logError("turnOffFlashSOS");
        logError("Cancel timer and flash light hopefully SOS");
        turnOffFlash();
        t.cancel();
        isFlashOn = false;
        isFlashSOSOn = false;
        toggleButtonImage();
    }

    private void turnOnFlashSOS(){
        logError("turnOnFlashSOS");
        final int temp = morseArray.get((int) Math.floor(flashCount/2));
        int period;
        t = new Timer();
        if (morseArray.get((int) Math.floor(flashCount/2)) == 0) {
            period = 300;
        }
        else {
            period = 600;
        }
        logError("FlashPeriod: "+ period);

        t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logError("FlashCount: "+ flashCount);
                    try{
                        if (morseArray.get((int) Math.floor(flashCount/2)) == temp){
                            if (!isOn) {
                                turnOnFlash();
                                logError("Flash ON "+ flashCount);
                            }
                            else {
                                turnOffFlash();
                                logError("Flash OFF "+ flashCount);
                            }
                        }else {
                            t.cancel();
                            turnOnFlashSOS();
                            flashCount --;
                        }
                    }catch (RuntimeException e){
                        logError("Run time exception: " + e.getMessage());
                        try{
//                            if (e.getMessage().contains("Invalid index")) {
//                                turnOffFlashSOS();
//                            If condition is commented as was getting different error message for api version 23 and 25
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                flashCount = -1;
                                t.cancel();
                                turnOnFlashSOS();
//                            }
                        }catch (RuntimeException ex){
                            logError("Run time exception: " + ex.getMessage());
                        }
                    }
                    flashCount ++;
                }
            },0, period );
    }

    public void flashSOS (View view){
        if (!isFlashSOSOn){
            if (isFlashOn)
                t.cancel();
            morseArray = new ArrayList<Integer>(Arrays.asList(0,0,0,1,1,1,0,0,0));
            flashCount = 0;
            turnOffFlash();
            turnOnFlashSOS();
            isFlashOn = true;
            isFlashSOSOn = true;
            toggleButtonImage();
        } else{
            logError("To: turnOffFlashSOS");
            turnOffFlashSOS();
        }
    }

        private void turnOffFlashAsync(){
        logError("turnOffFlashAsync");
        logError("Cancel timer and flash light hopefully");
            try {
                turnOffFlash();
            } catch (Exception e) {
                e.printStackTrace();
            }
            t.cancel();
        isFlashOn = false;
        isFlashSOSOn = false;
        toggleButtonImage();
    }

    private void turnOnFlashAsync(int f){
        logError("turnOnFlashAsync");
        logError("FlashRate: "+ f);
        t = new Timer();
        selectedFlashRate = f;
        if (f > 0){
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // this code will be executed after 2 seconds
                    if (!isOn)
                        turnOnFlash();
                    else
                        turnOffFlash();
                }
            },0, 1500/f );
        }
        else{
            turnOnFlash();
        }
        isFlashOn = true;
        isFlashSOSOn = false;
        toggleButtonImage();
    }

    private void turnOnFlash() {
        if (!isOn) {
            isOn = true;
            if (camera == null || params == null) {
                return;
            }
            // play sound
//            playSound();
            logError("turning on flash");
            params = camera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
//
//                // changing button/switch image
//                toggleButtonImage();
        }

    }

    private void turnOffFlash() {
        logError("isOn: "+isOn+" camera: "+camera+" params: "+params);
        if (isOn) {
            isOn = false;
            if (camera == null || params == null) {
                return;
            }
            // play sound
//            playSound();
            logError("turning off flash");
            params = camera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
//
//                // changing button/switch image
//                toggleButtonImage();
        }
    }

    public void flashFrequency(View view){
        flashFreq = (Button) view;
        flashFreq.setTextColor(getResources().getColor(R.color.colorFlashFreqButOn));
        flashFreq.setBackgroundDrawable(getResources().getDrawable(R.drawable.flon));
        logError("Toggle flash button's background");
        for (Button frq : freq)
            if (frq != flashFreq){
                frq.setTextColor(getResources().getColor(R.color.colorWhite));
                frq.setBackgroundDrawable(getResources().getDrawable(R.drawable.floff));
            }


        if(t != null)
            turnOffFlashAsync();

        logError("Array index " + String.valueOf(Arrays.asList(freq).indexOf(flashFreq)));

        turnOnFlashAsync(Arrays.asList(freq).indexOf(flashFreq));
    }

    // getting camera parameters
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                logError(e.getMessage());
            }
        }
    }

    private void toggleButtonImage(){
        if(isFlashOn)
            btnSwitch.setImageResource(R.drawable.pon);
        else
            btnSwitch.setImageResource(R.drawable.poff);
        if(isFlashSOSOn)
            btnSos.setImageResource(R.drawable.so);
        else
            btnSos.setImageResource(R.drawable.sf);

        if (isFlashOn) {
            turnOnFlash();
            logError("Flash ON "+ flashCount);
        }
        else {
            turnOffFlash();
            logError("Flash OFF "+ flashCount);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSensorManager.unregisterListener(this);

        if (mService != null) {
            unbindService(mServiceConn);
        }
        if (mHelper != null) try {
            mHelper.dispose();
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
        mHelper = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
// todo have to check what is causing activity to come in OnPause():
//        Interstitial ad
//        Google Play purchase
//        Back button
//        Home Button
        logError("On Pause");

        isOnPause = true;
        logError("pauseBilling: "+pauseBilling+" pauseIntAd: "+pauseIntAd);
        if (!pauseBilling && !pauseIntAd){
            // on pause turn off the flash
            try {
                turnOffFlashAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // to stop the listener and save battery
//        TODO Moving below to onDestroy as in pause its causing the compass to stop. Might have to enable to save battery
//        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        logError("On Resume");

        isOnPause = false;
        pauseBilling = false;
        pauseIntAd = false;

        // for the system's orientation sensor registered listeners
        if(mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME)){
            tvCompassError.setVisibility(View.GONE);
        }else{
            tvHeading.setText("");
        }
        logError("OnResume:mSensorManager.registerListener: "+mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME));

        // on resume turn on the flash
        if(hasFlash)
            flashFrequency(freq[selectedFlashRate]);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // on starting the app get the camera params
        getCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // on stop release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        int degree = Math.round(event.values[0]);
        if (degree >= 355 || degree <= 5)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 N");
        else if (degree > 5 && degree < 85)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 NE");
        else if (degree >= 85 && degree <= 95)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 E");
        else if (degree > 95 && degree < 175)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 SE");
        else if (degree >= 175 && degree <= 185)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 S");
        else if (degree > 185 && degree < 265)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 SW");
        else if (degree >= 265 && degree <= 275)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 W");
        else if (degree > 275 && degree < 355)
            tvHeading.setText(Integer.toString(degree) + "\u00B0 NW");

//            tvHeading.setText(Float.toString(degree) + "\u00B0 N");
//            tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    void logError(String msg) {
        if(enableDebugFL)
            Log.e(TAG, msg);
    }
    void logDebug(String msg) {
        if(enableDebugFL)
            Log.d(TAG, msg);
    }
}
