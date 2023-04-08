package com.yerbaswallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.platform.APIClient;
import com.yerbaswallet.tools.api.ApiAddress;
import com.yerbaswallet.tools.api.ApiUTxo;
import com.yerbaswallet.tools.api.GsonRequest;
import com.yerbaswallet.YerbasApp;
import com.yerbaswallet.presenter.activities.util.ActivityUTILS;
import com.yerbaswallet.presenter.entities.CurrencyEntity;
import com.yerbaswallet.tools.sqlite.CurrencyDataSource;
import com.yerbaswallet.tools.threads.executor.BRExecutor;
import com.yerbaswallet.tools.util.BRConstants;
import com.yerbaswallet.tools.util.Utils;
import com.yerbaswallet.wallet.WalletsMaster;
import com.yerbaswallet.wallet.abstracts.BaseWalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Yerbaswallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();

    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private Handler handler;

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context, BaseWalletManager walletManager) {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchRates(context, walletManager);
            updateFeePerKb(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = /*1*/0; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        String selectedISO = BRSharedPrefs.getPreferredFiatIso(context);
                        Log.e(TAG, "selectedISO: " + selectedISO);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
                            //Log.e(TAG, "theIso : " + theIso);
                            //Log.e(TAG, "Putting in the shared prefs");
                            BRSharedPrefs.putPreferredFiatIso(context, tmp.code);
                        }
                        Log.e("TAG", tmp.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies -- arr is NULL!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e(TAG, "getCurrencies: " + set.size());
        return new LinkedHashSet<>(set);
    }

    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (YerbasApp.isAppInBackground(context)) {
                                    Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
                                    stopTimerTask();
                                }
                                for (BaseWalletManager w : WalletsMaster.getInstance(context).getAllWallets()) {
                                    String iso = w.getIso(context);
                                    Set<CurrencyEntity> tmp = getCurrencies((Activity) context, w);
                                    CurrencyDataSource.getInstance(context).putCurrencies(context, iso, tmp);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();
        Log.e(TAG, "startTimer: started...");
        //initialize the TimerTask's job
        initializeTimerTask(context);

        timer.schedule(timerTask, 1000, 60000);
    }

    private void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static JSONArray fetchRates(Activity app, BaseWalletManager walletManager) {
        JSONArray jsonArray = null;

        if ("YERB".equals(walletManager.getIso(app))) {
            String yerb_btc_MultiplierURL = "https://xeggex.com/api/v2/ticker/YERB_BTC";
            String yerb_btc_jsonString = urlGET(app, yerb_btc_MultiplierURL);
            String yerb_usd_MultiplierURL = "https://xeggex.com/api/v2/ticker/YERB_USDT";
            String yerb_usd_jsonString = urlGET(app, yerb_usd_MultiplierURL);
            try {
                jsonArray = new JSONArray();
                JSONObject yerb_obj = new JSONObject();

                double usdRate;
                double btcRate;

                if (yerb_btc_jsonString != null || yerb_usd_jsonString != null) {
                    JSONObject yerb_btc_obj = (JSONObject) new JSONTokener(yerb_btc_jsonString).nextValue();
                    JSONObject yerb_usd_obj = (JSONObject) new JSONTokener(yerb_usd_jsonString).nextValue();
                    String name = "US Dollar";
                    String code = "USD";
                    usdRate = Double.parseDouble(yerb_usd_obj.getString("last_price"));
                    btcRate = Double.parseDouble(yerb_btc_obj.getString("last_price"));
                    yerb_obj.put("code", code);
                    yerb_obj.put("name", name);
                    yerb_obj.put("rate", usdRate);
                    jsonArray.put(yerb_obj);

                    System.out.println("\nYERB-USD Rate: " + usdRate + "\n");
                    System.out.println("\nYERB-BTC Rate: " + btcRate + "\n");

                    JSONArray multiFiatJson = multiFiatCurrency(app);

                    assert multiFiatJson != null;
                    int length = multiFiatJson.length();
                    for (int i = 0; i < length; i++) {
                        try {
                            JSONObject tmpObj = (JSONObject) multiFiatJson.get(i);

                            if (tmpObj.getString("code").equalsIgnoreCase("USD"))
                                continue;

                            double yerbRate = (float) tmpObj.getDouble("rate") * btcRate;
                            tmpObj.remove("rate");
                            tmpObj.put("rate", yerbRate);

                            jsonArray.put(tmpObj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    return jsonArray;

                } else {
                    Log.e(TAG, "getCurrencies: failed to get currencies from URL -- String was NULL");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jsonArray;
    }

    private static JSONArray multiFiatCurrency(Activity app) {

        String jsonString = urlGET(app, "https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    private static void updateFeePerKb(Context app) {
        WalletsMaster wm = WalletsMaster.getInstance(app);
        for (BaseWalletManager wallet : wm.getAllWallets()) {
            wallet.updateFee(app);
        }
    }

    public static String urlGET(Context app, String myURL) {
        if (ActivityUTILS.isMainThread()) {
            Log.e(TAG, "urlGET: network on main thread");
            throw new RuntimeException("network on main thread");
        }
        Map<String, String> headers = new HashMap<>();//YerbasApp.getYerbHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Log.e(TAG, "urlGET: " + myURL + ", resp is null");
                return null;
            }
            assert resp.body() != null;
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            if (resp != null) resp.close();

        }
        return response;
    }

    public static boolean isAddressUsed(Context context, String address) {
        boolean isUsed = false;
//        String fullUrl = String.format(BRConstants.fetchTxsPath(), "mqaak11jfP7LsJ2hH6gcNiQetMX7ZWmwXB");
        String fullUrl = String.format(BRConstants.fetchTxsPath(), address);
        RequestQueue volleyQueue = Volley.newRequestQueue(context);
        RequestFuture<ApiAddress> future = RequestFuture.newFuture();
        GsonRequest<ApiAddress> request = new GsonRequest<>(fullUrl, ApiAddress.class, null, future, future);
        volleyQueue.add(request);
        try {
            ApiAddress apiAddress = future.get(); // this will block
            isUsed = apiAddress != null && apiAddress.getTransactions() != null && !apiAddress.getTransactions().isEmpty();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return isUsed;
    }

    public static List<ApiUTxo> fetchUTXOS(Context context, String address, boolean isAsset) {
        // for test
        // String path = "https://api.testnet.yerbas.orgapi/addr/mqaak11jfP7LsJ2hH6gcNiQetMX7ZWmwXB/utxo";
        String path = isAsset ? String.format(BRConstants.fetchAssetUtxosPath(), address)
                : String.format(BRConstants.fetchYerbUtxosPath(), address);

        RequestQueue volleyQueue = Volley.newRequestQueue(context);
        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(path, future, future);
        volleyQueue.add(request);
        try {
            JSONArray arrayUtxos = future.get(); // this will block
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ApiUTxo>>() {
            }.getType();
            return gson.fromJson(arrayUtxos.toString(), listType);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }
}
