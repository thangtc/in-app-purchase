package com.asiantech.inapppurchase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.asiantech.util.IabHelper;
import com.asiantech.util.IabResult;
import com.asiantech.util.Inventory;
import com.asiantech.util.Purchase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // SKU for our subscription (infinite_amount gas)
    private static final String SKU_INFINITE_AMOUNT = "infinite_amount"; // "infinite_amount";
    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 10001;

    // Does the user have an active subscription to the infinite gas plan?
    private boolean mSubscribedToInfiniteGas = false;
    private IabHelper mHelper;

    // Listener that's called when we finish querying the items and subscriptions we own
    private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
                return;
            }

            // Is it a failure?
            if (result.isFailure()) {
                showErrorMessage("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the infinite gas plan?
            Purchase infiniteGasPurchase = inventory.getPurchase(SKU_INFINITE_AMOUNT);
            mSubscribedToInfiniteGas = (infiniteGasPurchase != null && verifyDeveloperPayload(infiniteGasPurchase));
            Log.d(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
                    + " infinite gas subscription.");
        }
    };

    // Callback for when a purchase is finished
    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) {
                return;
            }

            if (result.isFailure()) {
                showErrorMessage("Error purchasing: " + result);
                return;
            }

            if (!verifyDeveloperPayload(purchase)) {
                showErrorMessage("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_INFINITE_AMOUNT)) {
                // bought the infinite gas subscription
                Log.d(TAG, "Infinite gas subscription purchased.");
                showErrorMessage("Thank you for subscribing to infinite gas!");
                mSubscribedToInfiniteGas = true;

                /**
                 * {
                 "packageName":"com.asiantech.inapppurchase",
                 "productId":"infinite_amount",
                 "purchaseTime":1442891989248,
                 "purchaseState":0,
                 "developerPayload":"0923{=***000_3920}[]_0",
                 "purchaseToken":"ihkancifbbfjfdgemleiedfm.AO-J1Oy1v1atf3zvE-zK_T3VpKbSEwUtdTgH-Nt2ZXz1YTrLL0UeO2D3wdZVfzW-nAEMkzt_sVJydt8G2KwxSP1quG7lX01fO2AYqkjokByerdyrQGnaEg5VbB8LWU1zCvryHkHwirT4",
                 "autoRenewing":true
                 }
                 */
                purchase.getPackageName();
                purchase.getOrderId();
                purchase.getPurchaseTime();
                purchase.getPurchaseState();
                purchase.getDeveloperPayload();
                purchase.getToken();
                purchase.getSku();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        final String base64EncodedPublicKey = getString(R.string.app_publish_key);
        // Create the helper, passing it our context and the public key to verify signatures with
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        // Enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.e(TAG, "Problem setting up In-app Billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) {
                    return;
                }

                Log.d(TAG, "Setting up In-app Billing successful!!!!");
                // Hooray, IAB is fully set up!
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });

        findViewById(R.id.btnSub).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onInfiniteGasButtonClicked();
            }
        });
    }

    /*
     * "Subscribe to infinite gas" button clicked. Explain to user, then start purchase
     * flow for subscription.
     */
    private void onInfiniteGasButtonClicked() {
        if (!mHelper.subscriptionsSupported()) {
            showErrorMessage("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        /* verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         * an empty string, but on a production app you should carefully generate this. */
        String payload = getString(R.string.pay_load);

        Log.d(TAG, "Launching purchase flow for infinite gas subscription.");
        mHelper.launchPurchaseFlow(this, SKU_INFINITE_AMOUNT, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST,
                mPurchaseFinishedListener, payload);

    }

    private void showErrorMessage(@NonNull String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", null);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    private boolean verifyDeveloperPayload(Purchase p) {
        /*
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        String payload = p.getDeveloperPayload();
        return getString(R.string.pay_load).equals(payload);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) {
            return;
        }

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;
    }

}
