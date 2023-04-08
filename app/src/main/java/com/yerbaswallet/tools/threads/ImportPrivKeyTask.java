package com.yerbaswallet.tools.threads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.yerbaswallet.R;
import com.yerbaswallet.core.BRCoreAddress;
import com.yerbaswallet.core.BRCoreKey;
import com.yerbaswallet.core.BRCoreTransaction;
import com.yerbaswallet.core.BRCoreTransactionInput;
import com.yerbaswallet.core.BRCoreTransactionOutput;
import com.yerbaswallet.presenter.customviews.BRDialogView;
import com.yerbaswallet.presenter.customviews.BRToast;
import com.yerbaswallet.tools.animation.BRDialog;
import com.yerbaswallet.tools.animation.SpringAnimator;
import com.yerbaswallet.tools.manager.BRApiManager;
import com.yerbaswallet.tools.manager.BRReportsManager;
import com.yerbaswallet.tools.manager.BRSharedPrefs;
import com.yerbaswallet.tools.threads.executor.BRExecutor;
import com.yerbaswallet.tools.util.BRConstants;
import com.yerbaswallet.tools.util.CurrencyUtils;
import com.yerbaswallet.tools.util.TypesConverter;
import com.yerbaswallet.tools.util.Utils;
import com.yerbaswallet.wallet.WalletsMaster;
import com.yerbaswallet.wallet.abstracts.BaseWalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * Yerbaswallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/2/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class ImportPrivKeyTask extends AsyncTask<String, String, String> {
    public static final String TAG = ImportPrivKeyTask.class.getName();
    private Activity app;
    private String key;
    private String iso;
    private BRCoreTransaction mTransaction;

    public ImportPrivKeyTask(Activity activity) {
        app = activity;
    }

    @Override
    protected String doInBackground(String... params) { //params[0] = private key, params[1] = current wallet's ISO
        if (params.length != 2) {
            throw new RuntimeException("Must have 2 params");
        }

        key = params[0];
        iso = params[1];
        if (Utils.isNullOrEmpty(key) || app == null || Utils.isNullOrEmpty(iso)) {
            Log.e(TAG, "ImportPrivKeyTask:doInBackground: failed: " + iso + "|" + app);
            return null;
        }

        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);

        BRCoreKey coreKey = new BRCoreKey();
        coreKey.setPrivKey(key);
        String tmpAddress = coreKey.address();
        if (Utils.isNullOrEmpty(tmpAddress)) {
            String err = "doInBackground: failed to create the address for iso " + iso;
            BRReportsManager.reportBug(new NullPointerException(err));
            Log.e(TAG, err);
            return null;
        }

        if (!iso.equalsIgnoreCase("YERB")) {
            String err = "doInBackground: Can't happen, uknown iso: " + iso;
            BRReportsManager.reportBug(new NullPointerException(err));
            Log.e(TAG, err);
            return null;
        }

        String decoratedAddress = wm.decorateAddress(app, tmpAddress);

        //automatically uses testnet if x-testnet is true
//        String fullUrl = String.format("https://%s/q/addr/%s/utxo?currency=%s", YerbasApp.HOST, decoratedAddress, iso);
        String fullUrl = String.format(BRConstants.networkUrl(), decoratedAddress);
        mTransaction = createSweepingTx(app, fullUrl);
        if (mTransaction == null) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                            app.getString(R.string.Import_Error_empty), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                }
            });
            return null;

        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (mTransaction == null) return;

        final BaseWalletManager walletManager = WalletsMaster.getInstance(app).getWalletByIso(app, iso);

        BigDecimal bigAmount = new BigDecimal(walletManager.getWallet().getTransactionAmount(mTransaction));
        BigDecimal bigFee = new BigDecimal(0);

        for (BRCoreTransactionInput in : mTransaction.getInputs())
            bigFee = bigFee.add(new BigDecimal(in.getAmount()));
        for (BRCoreTransactionOutput out : mTransaction.getOutputs())
            bigFee = bigFee.subtract(new BigDecimal(out.getAmount()));

        String formattedFiatAmount = CurrencyUtils.getFormattedAmount(app, BRSharedPrefs.getPreferredFiatIso(app), walletManager.getFiatForSmallestCrypto(app, bigAmount, null));

        //bits, BTCs..
        String amount = CurrencyUtils.getFormattedAmount(app, walletManager.getIso(app), bigAmount);
        String fee = CurrencyUtils.getFormattedAmount(app, walletManager.getIso(app), bigFee.abs());
        String message = String.format(app.getString(R.string.Import_confirm), amount, fee);
        String posButton = String.format("%s (%s)", amount, formattedFiatAmount);
        BRDialog.showCustomDialog(app, "", message, posButton, app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {

                        if (mTransaction == null) {
                            app.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                                            app.getString(R.string.Import_Error_notValid), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                }
                                            }, null, null, 0);
                                }
                            });
                            return;
                        }

                        BRCoreKey signingKey = new BRCoreKey(key);

                        mTransaction.sign(signingKey, walletManager.getForkId());

                        if (!mTransaction.isSigned()) {
                            String err = "transaction is not signed";
                            Log.e(TAG, "run: " + err);
                            BRReportsManager.reportBug(new IllegalArgumentException(err));
                            return;
                        }

                        walletManager.getPeerManager().publishTransaction(mTransaction);
                    }
                });

                brDialogView.dismissWithAnimation();

            }
        }, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismissWithAnimation();
            }
        }, null, 0);

    }

    private BRCoreTransaction createSweepingTx(final Context app, String url) {
        if (url == null || url.isEmpty()) return null;

        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(app, app.getString(R.string.Import_checking), Toast.LENGTH_LONG).show();
                    }
                }, 1000);
            }
        });

        String jsonString = BRApiManager.urlGET(app, url);
        if (jsonString == null || jsonString.isEmpty()) return null;
        BaseWalletManager walletManager = WalletsMaster.getInstance(app).getWalletByIso(app, iso);
        if (walletManager == null) {
            String err = "createSweepingTx: wallet is null for: " + iso;
            BRReportsManager.reportBug(new NullPointerException(err));
            Log.e(TAG, err);
            return null;
        }

        BRCoreTransaction transaction = new BRCoreTransaction();
        long totalAmount = 0;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            int length = jsonArray.length();

            for (int i = 0; i < length; i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                byte[] txid = TypesConverter.hexToBytesReverse(obj.getString("txid"));
                int vout = obj.getInt("vout");
                byte[] scriptPubKey = TypesConverter.hexToBytes(obj.getString("scriptPubKey"));
                long amount = obj.getLong("satoshis");
                totalAmount += amount;
                BRCoreTransactionInput in = new BRCoreTransactionInput(txid, vout, amount, scriptPubKey, new byte[]{}, -1);
                transaction.addInput(in);
            }

            if (totalAmount <= 0) return null;
//
            BRCoreAddress address = walletManager.getReceiveAddress(app);

            BRCoreKey signingKey = new BRCoreKey(key);

            long fee = walletManager.getWallet().getFeeForTransactionSize(transaction.getSize() + 34 + (signingKey.getPubKey().length - 33) * transaction.getInputs().length);
            transaction.addOutput(new BRCoreTransactionOutput(totalAmount - fee, address.getPubKeyScript()));
            return transaction;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean trySweepWallet(final Context ctx, final String privKey, final BaseWalletManager walletManager) {
        if (ctx == null) {
            Log.e(TAG, "trySweepWallet: ctx is null");
            return false;
        }
        if (BRCoreKey.isValidBitcoinBIP38Key(privKey)) {
            Log.d(TAG, "isValidBitcoinBIP38Key true");
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setTitle("password protected key");

                    final View input = ((Activity) ctx).getLayoutInflater().inflate(R.layout.view_bip38password_dialog, null);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    builder.setView(input);

                    final EditText editText = (EditText) input.findViewById(R.id.bip38password_edittext);

                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

                        }
                    }, 100);

                    // Set up the buttons
                    builder.setPositiveButton(ctx.getString(R.string.Button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (ctx != null)
                                ((Activity) ctx).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRToast.showCustomToast(ctx, ctx.getString(R.string.Import_checking), 500, Toast.LENGTH_LONG, R.drawable.toast_layout_blue);
                                    }
                                });
                            if (editText == null) {
                                Log.e(TAG, "onClick: edit text is null!");
                                return;
                            }

                            final String pass = editText.getText().toString();
                            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                @Override
                                public void run() {
                                    String decryptedKey = BRCoreKey.decryptBip38Key(privKey, pass);
                                    //if the decryptedKey is not empty then we have a regular private key and isValidBitcoinBIP38Key will be false
                                    if (decryptedKey.equals("")) {
                                        SpringAnimator.springView(input);
                                        trySweepWallet(ctx, privKey, walletManager);
                                    } else {
                                        trySweepWallet(ctx, decryptedKey, walletManager);
                                    }
                                }
                            });

                        }
                    });
                    builder.setNegativeButton(ctx.getString(R.string.Button_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            });
            return true;
        } else if (BRCoreKey.isValidBitcoinPrivateKey(privKey)) {
            Log.d(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(((Activity) ctx)).execute(privKey, walletManager.getIso(ctx));
            return true;
        } else {
            Log.e(TAG, "trySweepWallet: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
            return false;
        }
    }


}
