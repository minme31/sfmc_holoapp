package com.example.sfmc_holoapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.evergage.android.Campaign;
import com.evergage.android.CampaignHandler;
import com.evergage.android.ClientConfiguration;
import com.evergage.android.Evergage;
import com.evergage.android.Screen;
import com.evergage.android.promote.Category;
import com.evergage.android.promote.Product;
import com.evergage.android.promote.Tag;

import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CHANNEL = "demo.sfmc_holoapp/info";
    private FlutterActivity thisActivity = this;
    private MyFlutterApplication myApp;
    private Evergage myEvg;
    private Screen myScreen;
    private Campaign activeCampaign;
    private CampaignHandler handler;

    @Override
    public void onStart() {
        super.onStart();

        myEvg = Evergage.getInstance();
        myScreen = myEvg.getScreenForActivity(this);
        handler = new CampaignHandler() {

            @Override
            public void handleCampaign(@NonNull Campaign campaign) {
                Log.i(TAG, "handleCampaign: " + campaign.getData());
                // Validate the campaign data since it's dynamic JSON. Avoid processing if fails.
                String featuredProductName = campaign.getData().optString("productSelected");
                if (featuredProductName == null || featuredProductName.isEmpty()) {
                    return;
                }

                // Check if the same content is already visible/active (see Usage Details above).
                if (activeCampaign != null && activeCampaign.equals(campaign)) {
                    return;
                }

                // Track the impression for statistics even if the user is in the control group.
                myScreen.trackImpression(campaign);

                // Only display the campaign if the user is not in the control group.
                if (!campaign.isControlGroup()) {
                    // Keep active campaign as long as needed for (re)display and comparison
                    activeCampaign = campaign;
                    //Log.d(TAG, "New active campaign name " + campaign.getCampaignName() +" for target " + campaign.getTarget() + " with data " + campaign.getData());

                    // Display campaign content
                    //May Not need This: TextView featuredProductTextView = (TextView) findViewById(R.id.evergage_in_app_message);

                    //May Not Need This: featuredProductTextView.setText("Our featured product is " + featuredProductName + "!");
                }
            }
        };
        myScreen.setCampaignHandler(handler, "selectedProduct");
        // App Foreground Action
        myScreen.trackAction("App Foreground");
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        // myScreen.trackAction("App Foreground");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop: ");
        // myScreen.trackAction("App Background");
        super.onStop();
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (methodCall, result) -> {
                            Map<String, Object> arguments = methodCall.arguments();

                            if (methodCall.method.equals("androidInitialize")) {

                                // String account = (String) arguments.get("account");
                                // String ds = (String) arguments.get("ds");

                                // NOTE: MyFlutterApplication() は、Androidシステムによって呼び出されるので、
                                //       newしてインスタンス化するのは非推奨です。

                                // if (myApp == null) {
                                //     myApp = new MyFlutterApplication();
                                // }

                                // NOTE: evergage.startはApplication.onCreateで行うことが推奨されていたため、移動しました。
                                //       MethodChannelのトリガで行いたい場合はMainActivityにstartEvgメソッドを作り、呼び出してください。

                                // if (myEvg == null) {
                                //     myEvg = myApp.startEvg(account, ds);
                                // }

                                myScreen = myEvg.getScreenForActivity(thisActivity);

                                String message = "Initialized!!";
                                result.success(message);
                            }

                            if (methodCall.method.equals("androidLogEvent")) {

                                String event = (String) arguments.get("event");
                                String description = (String) arguments.get("description");
                                String message = null;

                                if (event.equals("setUserId")) {
                                    myEvg.setUserId(description);
                                    message = "Successfully set User Id";
                                } else {

                                    myScreen = refreshScreen(event, description, myScreen);

                                    if (activeCampaign != null) {
                                        message = "Campaign: " + activeCampaign.getCampaignName() + " For target: " + activeCampaign.getTarget() + " With data: " + activeCampaign.getData();
                                    } else {
                                        message = "No Campaign Returned";
                                    }
                                }
                                result.success(message);
                            }
                        }
                );
    }

    public Screen refreshScreen(String event, String description, Screen screen) {
        // Evergage track screen view
        //final Screen screen = myEvg.getScreenForActivity(fa);

        if (screen != null) {
            // If screen is viewing a product:
            //screen.viewItem(new Product("p123"));

            // If screen is viewing a category, like women's merchandise:
            //screen.viewCategory(new Category("Womens"));

            // Or if screen is viewing a tag, like some specific brand:
            //screen.viewTag(new Tag("SomeBrand", Tag.Type.Brand));

            switch(event){
                case "trackAction":
                    screen.trackAction(description);
                    break;
                case "viewItem":
                    screen.viewItem(new Product(description));
                    break;
                case "viewCategory":
                    screen.viewCategory(new Category(description));
                    break;
                case "viewTag":
                    screen.viewTag(new Tag(description, Tag.Type.Brand));
                    break;
            }
        }
        return screen;
    }
}
