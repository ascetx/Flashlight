package info.ascetx.flashlight.app;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

import info.ascetx.flashlight.MainActivity;
import info.ascetx.flashlight.R;
import info.ascetx.flashlight.billing.BillingProvider;
import info.ascetx.flashlight.helper.SkuRowData;

import static com.android.billingclient.api.BillingClient.BillingResponse;
import static info.ascetx.flashlight.billing.BillingConstants.getSkuList;

/**
 * Created by JAYESH on 14-08-2018.
 */

public class AcquireDialog {

    private static final String TAG = "AcquireDialog";

    private BillingProvider mBillingProvider;
    
    private MainActivity activity;

    private String price;
    /**
     * Notifies the fragment/class that billing manager is ready and provides a BillingProviders
     * instance to access it
     */
    public void onManagerReady(BillingProvider billingProvider) {
        mBillingProvider = billingProvider;
//        if (mRecyclerView != null) {
//            handleManagerAndUiReady();
//        }
    }

    public void show(final SkuRowData skuRowData) {
        /* Alert Dialog Code Start*/
        AlertDialog.Builder alert = new AlertDialog.Builder(activity, R.style.MyDialogTheme);
        alert.setTitle(activity.getResources().getString(R.string.premium_title));
        alert.setMessage(Html.fromHtml("<font color='#ffffff'>"+activity.getResources().getString(R.string.premium_message)
                +"<br><b>"+price+"</b>"
                +"</font>")); //Message here

        alert.setPositiveButton("GET NOW", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.e(TAG,"skuRowData: "+skuRowData);
                Log.e(TAG,"isPremiumPurchased: "+mBillingProvider.isPremiumPurchased());
                if (skuRowData != null && mBillingProvider.isPremiumPurchased()) {
                    showAlreadyPurchasedToast();
                }  else {
                    mBillingProvider.getBillingManager().initiatePurchaseFlow(skuRowData.getSku(),
                            skuRowData.getSkuType());
                }
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton

        alert.setNegativeButton("LATER", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.show();

        Button buttonPositive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonPositive.setTextColor(ContextCompat.getColor(activity, R.color.premium_alert_dialog_button_positive));
        Button buttonNegative = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        buttonNegative.setTextColor(ContextCompat.getColor(activity, R.color.premium_alert_dialog_button_negative));

       /* Alert Dialog Code End*/
    }

    protected void showAlreadyPurchasedToast() {
        Context context = mBillingProvider.getBillingManager().getContext();
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), R.string.alert_already_purchased, Snackbar.LENGTH_LONG).setAction("Action",null);
//        GradientDrawable gradientDrawable = new GradientDrawable(
//                GradientDrawable.Orientation.LEFT_RIGHT,
//                new int[]{ContextCompat.getColor(activity, R.color.color5),
//                        ContextCompat.getColor(activity, R.color.color1),
//                        ContextCompat.getColor(activity, R.color.color2),
//                        ContextCompat.getColor(activity, R.color.color3),
//                        ContextCompat.getColor(activity, R.color.color4),
//                        ContextCompat.getColor(activity, R.color.color5)});
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            snackbar.getView().setBackground(activity.getResources().getDrawable(R.drawable.premium_gold_dialog));
        } else {
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(context,R.color.premium_alert_dialog_start));
        }*/
        snackbar.getView().setBackgroundResource(R.drawable.premium_gold_dialog);
        TextView textView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        textView.setTextColor(ContextCompat.getColor(context,R.color.white));
        snackbar.show();
    }


        /**
         * Executes query for SKU details at the background thread
         */
    public void handleManagerAndUiReady(MainActivity mainActivity) {
        // If Billing Manager was successfully initialized - start querying for SKUs
        this.activity = mainActivity;
//        setWaitScreen(true);
        querySkuDetails();
    }

    private void displayAnErrorIfNeeded() {
        if (activity == null || activity.isFinishing()) {
            Log.i(TAG, "No need to show an error - activity is finishing already");
            return;
        }

//        mLoadingView.setVisibility(View.GONE);
//        mErrorTextView.setVisibility(View.VISIBLE);
        int billingResponseCode = mBillingProvider.getBillingManager()
                .getBillingClientResponseCode();

        switch (billingResponseCode) {
            case BillingResponse.OK:
                // If manager was connected successfully, then show no SKUs error
                showSnackBar(activity.getWindow().getDecorView().getRootView(),activity.getResources().getString(R.string.error_no_skus));
                break;
            case BillingResponse.BILLING_UNAVAILABLE:
                showSnackBar(activity.getWindow().getDecorView().getRootView(),activity.getResources().getString(R.string.error_billing_unavailable));
                break;
            default:
                showSnackBar(activity.getWindow().getDecorView().getRootView(),activity.getResources().getString(R.string.error_billing_default));
        }
    }

    /**
     * Queries for in-app and subscriptions SKU details and updates an adapter with new data
     */
    private void querySkuDetails() {
        long startTime = System.currentTimeMillis();

        Log.d(TAG, "querySkuDetails() got subscriptions and inApp SKU details lists for: "
                + (System.currentTimeMillis() - startTime) + "ms");

        if (activity != null && !activity.isFinishing()) {
            final List<SkuRowData> dataList = new ArrayList<>();
//            mAdapter = new SkusAdapter();
//            final UiManager uiManager = createUiManager(mAdapter, mBillingProvider);
//            mAdapter.setUiManager(uiManager);
            // Filling the list with all the data to render subscription rows
//            List<String> subscriptionsSkus = getSkuList(BillingClient.SkuType.SUBS);
//            addSkuRows(dataList, subscriptionsSkus, BillingClient.SkuType.SUBS, new Runnable() {
//                @Override
//                public void run() {
                    // Once we added all the subscription items, fill the in-app items rows below
                    List<String> inAppSkus = getSkuList(BillingClient.SkuType.INAPP);
                    addSkuRows(dataList, inAppSkus, BillingClient.SkuType.INAPP, null);
//                }
//            });
        }
    }

    private void addSkuRows(final List<SkuRowData> inList, List<String> skusList,
                            final @BillingClient.SkuType String billingType, final Runnable executeWhenFinished) {

        mBillingProvider.getBillingManager().querySkuDetailsAsync(billingType, skusList,
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {

                        if (responseCode != BillingResponse.OK) {
                            Log.w(TAG, "Unsuccessful query for type: " + billingType
                                    + ". Error code: " + responseCode);
                        } else if (skuDetailsList != null
                                && skuDetailsList.size() > 0) {
                            // If we successfully got SKUs, add a header in front of the row
//                            int stringRes = BillingClient.SkuType.INAPP.equals(billingType)
//                                    ? R.string.header_inapp : R.string.header_subscriptions;
//                            inList.add(new SkuRowData(getString(stringRes)));
                            // Then fill all the other rows
                            for (SkuDetails details : skuDetailsList) {
                                Log.i(TAG, "Adding sku: " + details);
                                inList.add(new SkuRowData(details, billingType));
                            }

                            if (inList.size() == 0) {
                                displayAnErrorIfNeeded();
                            } else {

                                price = inList.get(0).getPrice();
                                show(inList.get(0));
//                                if (mRecyclerView.getAdapter() == null) {
//                                    mRecyclerView.setAdapter(mAdapter);
//                                    Resources res = getContext().getResources();
//                                    mRecyclerView.addItemDecoration(new CardsWithHeadersDecoration(
//                                            mAdapter, (int) res.getDimension(R.dimen.header_gap),
//                                            (int) res.getDimension(R.dimen.row_gap)));
//                                    mRecyclerView.setLayoutManager(
//                                            new LinearLayoutManager(getContext()));
//                                }
//
//                                mAdapter.updateData(inList);
//                                setWaitScreen(false);
                            }
                        } else {
                            // Handle empty state
                            displayAnErrorIfNeeded();
                        }

                        if (executeWhenFinished != null) {
                            executeWhenFinished.run();
                        }
                    }
                });
    }

    private void showSnackBar(View view, String message){
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG).setAction("Action",null);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(activity,R.color.snackbar_err_color));
        TextView textView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }
}
