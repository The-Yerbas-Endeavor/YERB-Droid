package com.yerbaswallet.wallet;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.format.DateUtils;
import android.util.Log;

import com.yerbaswallet.YerbasApp;
import com.yerbaswallet.R;
import com.yerbaswallet.core.BRCoreKey;
import com.yerbaswallet.core.BRCoreMasterPubKey;
import com.yerbaswallet.presenter.customviews.BRDialogView;
import com.yerbaswallet.tools.animation.BRAnimator;
import com.yerbaswallet.tools.animation.BRDialog;
import com.yerbaswallet.tools.manager.BRReportsManager;
import com.yerbaswallet.tools.manager.BRSharedPrefs;
import com.yerbaswallet.tools.security.BRKeyStore;
import com.yerbaswallet.tools.threads.executor.BRExecutor;
import com.yerbaswallet.tools.util.BRConstants;
import com.yerbaswallet.tools.util.Bip39Reader;
import com.yerbaswallet.tools.util.TrustedNode;
import com.yerbaswallet.tools.util.Utils;
import com.yerbaswallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WalletsMaster {

    private static final String TAG = WalletsMaster.class.getName();

    private static WalletsMaster instance;

    private List<BaseWalletManager> mWallets = new ArrayList<>();

    private WalletsMaster(Context app) {
    }

    public synchronized static WalletsMaster getInstance(Context app) {
        if (instance == null) {
            instance = new WalletsMaster(app);
        }
        return instance;
    }

    public List<BaseWalletManager> getAllWallets() {
        return mWallets;
    }

    //return the needed wallet for the iso
    public BaseWalletManager getWalletByIso(Context app, String iso) {
        if (Utils.isNullOrEmpty(iso))
            throw new RuntimeException("getWalletByIso with iso = null, Cannot happen!");
        if (iso.equalsIgnoreCase("YERB"))
            return YerbWalletManager.getInstance(app);
        return null;
    }

    public BaseWalletManager getCurrentWallet(Context app) {
        return getWalletByIso(app, BRSharedPrefs.getCurrentWalletIso(app));
    }

    //get the total fiat balance held in all the wallets in the smallest unit (e.g. cents)
    public BigDecimal getAggregatedFiatBalance(Context app) {
        BigDecimal totalBalance = new BigDecimal(0);
        for (BaseWalletManager wallet : mWallets) {
            totalBalance = totalBalance.add(wallet.getFiatBalance(app));
        }
        return totalBalance;
    }

    public synchronized boolean generateRandomSeed(final Context ctx) {
        SecureRandom sr = new SecureRandom();
        final String[] words;
        List<String> list;
        String languageCode = Locale.getDefault().getLanguage();
        if (languageCode == null) languageCode = "en";
        list = Bip39Reader.bip39List(ctx, languageCode);
        words = list.toArray(new String[list.size()]);
        final byte[] randomSeed = sr.generateSeed(16);
        if (words.length != 2048) {
            BRReportsManager.reportBug(new IllegalArgumentException("the list is wrong, size: " + words.length), true);
            return false;
        }
        if (randomSeed.length != 16)
            throw new NullPointerException("failed to create the seed, seed length is not 128: " + randomSeed.length);
        byte[] paperKeyBytes = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);
        if (paperKeyBytes == null || paperKeyBytes.length == 0) {
            BRReportsManager.reportBug(new NullPointerException("failed to encodeSeed"), true);
            return false;
        }
        String[] splitPhrase = new String(paperKeyBytes).split(" ");
        if (splitPhrase.length != 12) {
            BRReportsManager.reportBug(new NullPointerException("phrase does not have 12 words:" + splitPhrase.length + ", lang: " + languageCode), true);
            return false;
        }
        boolean success = false;
        try {
            success = BRKeyStore.putPhrase(paperKeyBytes, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return false;
        }
        if (!success) return false;
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(ctx, 0);
        } catch (UserNotAuthenticatedException e) {
            throw new RuntimeException("Failed to retrieve the phrase even though at this point the system auth was asked for sure.");
        }
        if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("phrase is null!!");
        if (phrase.length == 0) throw new RuntimeException("phrase is empty");
        byte[] seed = BRCoreKey.getSeedFromPhrase(phrase);
        if (seed == null || seed.length == 0) throw new RuntimeException("seed is null");
        byte[] authKey = BRCoreKey.getAuthPrivKeyForAPI(seed);
        if (authKey == null || authKey.length == 0) {
            BRReportsManager.reportBug(new IllegalArgumentException("authKey is invalid"), true);
        }
//        BRKeyStore.putAuthKey(authKey, ctx);
        int walletCreationTime = (int) (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);
        BRKeyStore.putWalletCreationTime(walletCreationTime, ctx);
//        final WalletInfo info = new WalletInfo();
//        info.creationDate = walletCreationTime;
//        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
//            @Override
//            public void run() {
//                KVStoreManager.getInstance().putWalletInfo(ctx, info); //push the creation time to the kv store
//            }
//        });

        //store the serialized in the BRKeyStore
        byte[] pubKey = new BRCoreMasterPubKey(paperKeyBytes, true).serialize();
        BRKeyStore.putMasterPublicKey(pubKey, ctx);

        return true;

    }

    public boolean isIsoCrypto(Context app, String iso) {
        for (BaseWalletManager w : mWallets) {
            if (w.getIso(app).equalsIgnoreCase(iso)) return true;
        }
        return false;
    }

    public boolean wipeKeyStore(Context context) {
        Log.d(TAG, "wipeKeyStore");
        return BRKeyStore.resetWalletKeyStore(context);
    }

    /**
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(ctx, 0);
                //if not authenticated, an error will be thrown and returned false, so no worry about mistakenly removing the wallet
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (UserNotAuthenticatedException e) {
                return false;
            }

        }
        return false;
    }

    public boolean noWalletForPlatform(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);
        return pubkey == null || pubkey.length == 0;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public boolean isNetworkAvailable(Context ctx) {
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    public void wipeWalletButKeystore(final Context ctx) {
        Log.d(TAG, "wipeWalletButKeystore");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                for (BaseWalletManager wallet : mWallets) {
                    wallet.wipeData(ctx);
                }
//                wipeAll(ctx);
//                ((ActivityManager)ctx.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
            }
        });
    }

    public void wipeAll(Context app) {
        wipeKeyStore(app);
        wipeWalletButKeystore(app);
    }

    public void refreshBalances(Context app) {
        for (BaseWalletManager wallet : mWallets) {
            long balance = wallet.getWallet().getBalance();
            wallet.setCashedBalance(app, balance);
        }

    }

    public void initWallets(Context app) {
        if (!mWallets.contains(YerbWalletManager.getInstance(app)))
            mWallets.add(YerbWalletManager.getInstance(app));
    }

    public void initLastWallet(Context app) {
        if (app == null) {
            app = YerbasApp.getYerbContext();
            if (app == null) {
                Log.e(TAG, "initLastWallet: FAILED, app is null");
                return;
            }
        }
        BaseWalletManager wallet = getWalletByIso(app, BRSharedPrefs.getCurrentWalletIso(app));
        if (wallet == null) wallet = getWalletByIso(app, "YERB");
        wallet.connectWallet(app);
    }

    public void updateFixedPeer(Context app, BaseWalletManager wm) {
        String node = BRSharedPrefs.getTrustNode(app, wm.getIso(app));
        if (!Utils.isNullOrEmpty(node)) {
            String host = TrustedNode.getNodeHost(node);
            int port = TrustedNode.getNodePort(node);
            boolean success = wm.getPeerManager().useFixedPeer(host, port);
            if (!success) {
                Log.e(TAG, "updateFixedPeer: Failed to updateFixedPeer with input: " + node);
            } else {
                Log.d(TAG, "updateFixedPeer: succeeded");
            }
        }
        wm.getPeerManager().connect();

    }

    public void startTheWalletIfExists(final Activity app) {
        final WalletsMaster m = WalletsMaster.getInstance(app);
        if (!m.isPasscodeEnabled(app)) {
            //Device passcode/password should be enabled for the app to work
            BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.Prompts_NoScreenLock_body_android),
                    app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            app.finish();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            app.finish();
                        }
                    }, 0);
        } else {
            if (!m.noWallet(app)) {
                BRAnimator.startYerbActivity(app, true);
            }
            //else just sit in the intro screen

        }
    }

}