package org.yerbas.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.yerbas.wallet.core.NetworkParameters;

/** Minimal installable shell for validating the rebuilt Yerbas core on Android. */
public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        NetworkParameters network = NetworkParameters.mainnet();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("YERB Droid\nClean Rebuild Test");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("Mainnet protocol: " + network.protocolVersion()
                + "\nP2P port: " + network.port()
                + "\n\nWallet creation and funds are disabled until GhostRider, sync, signing, and device tests pass.");
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(status);
        setContentView(layout);
    }
}
