package com.yerbaswallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.platform.assets.Asset;
import com.platform.assets.AssetsRepository;
import com.yerbaswallet.R;
import com.yerbaswallet.core.BRCoreAddress;
import com.yerbaswallet.core.MyTransactionAsset;
import com.yerbaswallet.presenter.customviews.BRText;
import com.yerbaswallet.presenter.entities.TxUiHolder;
import com.yerbaswallet.tools.manager.BRClipboardManager;
import com.yerbaswallet.tools.manager.BRSharedPrefs;
import com.yerbaswallet.tools.util.BRDateUtil;
import com.yerbaswallet.tools.util.CurrencyUtils;
import com.yerbaswallet.wallet.WalletsMaster;
import com.yerbaswallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;

import static com.yerbaswallet.tools.util.BRConstants.SATOSHIS;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentTxDetails extends DialogFragment {

    private static final String EXTRA_TX_ITEM = "tx_item";
    private static final String TAG = "FragmentTxDetails";

    private TxUiHolder mTransaction;

    private BRText mTxAction;
    private BRText mTxAmount;
    private BRText mTxStatus;
    private BRText mTxDate;
    private BRText mToFrom;
    private BRText mToFromAddress;

    private BRText mStartingBalance;
    private BRText mEndingBalance;
    private BRText mConfirmedInBlock;
    private BRText mTransactionId;
    private BRText mShowHide;
//    private BRText mAmountWhenSent;
//    private BRText mAmountNow,/* labelWhenSent,*/ labelAmountNow;

    private ImageButton mCloseButton;
    private RelativeLayout mDetailsContainer, layoutStartingBalance, layoutEndingBalance;

    boolean mDetailsShowing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.transaction_details, container, false);

//        mAmountNow = rootView.findViewById(R.id.amount_now);
//        mAmountWhenSent = rootView.findViewById(R.id.amount_when_sent);
        mTxAction = rootView.findViewById(R.id.tx_action);
        mTxAmount = rootView.findViewById(R.id.tx_amount);

        mTxStatus = rootView.findViewById(R.id.tx_status);
        mTxDate = rootView.findViewById(R.id.tx_date);
        mToFrom = rootView.findViewById(R.id.tx_to_from);
        mToFromAddress = rootView.findViewById(R.id.tx_to_from_address);
        mStartingBalance = rootView.findViewById(R.id.tx_starting_balance);
        mEndingBalance = rootView.findViewById(R.id.tx_ending_balance);
        mConfirmedInBlock = rootView.findViewById(R.id.confirmed_in_block_number);
        mTransactionId = rootView.findViewById(R.id.transaction_id);
        mShowHide = rootView.findViewById(R.id.show_hide_details);
        mDetailsContainer = rootView.findViewById(R.id.details_container);
        mCloseButton = rootView.findViewById(R.id.close_button);
        layoutStartingBalance = rootView.findViewById(R.id.layout_starting_balance);
        layoutEndingBalance = rootView.findViewById(R.id.layout_ending_balance);
//        labelWhenSent = rootView.findViewById(R.id.label_when_sent);
//        labelAmountNow = rootView.findViewById(R.id.label_now);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mShowHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mDetailsShowing) {
                    mDetailsContainer.setVisibility(View.VISIBLE);
                    mDetailsShowing = true;
                    mShowHide.setText("Hide Details");
                } else {
                    mDetailsContainer.setVisibility(View.GONE);
                    mDetailsShowing = false;
                    mShowHide.setText("Show Details");
                }
            }
        });

        updateUi();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void setTransaction(TxUiHolder item) {

        this.mTransaction = item;

    }

    private void updateUi() {

        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        // Set mTransction fields
        if (mTransaction != null) {
            boolean sent = mTransaction.getSent() > 0;
            String amountWhenSent;
            String amountNow;
            String exchangeRateFormatted;

            if (!mTransaction.isValid()) {
                mTxStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            //user prefers crypto (or fiat)
            boolean isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(getActivity());
            String cryptoIso = walletManager.getIso(getActivity());
            String fiatIso = BRSharedPrefs.getPreferredFiatIso(getContext());

            String iso = isCryptoPreferred ? cryptoIso : fiatIso;

            BigDecimal cryptoAmount = new BigDecimal(mTransaction.getAmount());

            BigDecimal fiatAmountNow = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), null);

//            BigDecimal fiatAmountWhenSent;
//            TxMetaData metaData = KVStoreManager.getInstance().getTxMetaData(getActivity(), mTransaction.getTxHash());
//            if (metaData == null || metaData.exchangeRate == 0 || Utils.isNullOrEmpty(metaData.exchangeCurrency)) {
//                fiatAmountWhenSent = new BigDecimal(0);
//                amountWhenSent = CurrencyUtils.getFormattedAmount(getActivity(), fiatIso, fiatAmountWhenSent);//always fiat amount
//            } else {
//
//                CurrencyEntity ent = new CurrencyEntity(metaData.exchangeCurrency, null, (float) metaData.exchangeRate, walletManager.getIso(getActivity()));
//                fiatAmountWhenSent = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), ent);
//                amountWhenSent = CurrencyUtils.getFormattedAmount(getActivity(), ent.code, fiatAmountWhenSent);//always fiat amount
//
//            }

//            amountNow = CurrencyUtils.getFormattedAmount(getActivity(), fiatIso, fiatAmountNow);//always fiat amount

//            mAmountWhenSent.setText(amountWhenSent);
//            mAmountNow.setText(amountNow);

            BigDecimal tmpStartingBalance = new BigDecimal(mTransaction.getBalanceAfterTx()).subtract(cryptoAmount.abs()).subtract(new BigDecimal(mTransaction.getFee()).abs());

            BigDecimal startingBalance = isCryptoPreferred ? walletManager.getCryptoForSmallestCrypto(getActivity(), tmpStartingBalance) : walletManager.getFiatForSmallestCrypto(getActivity(), tmpStartingBalance, null);

            BigDecimal endingBalance = isCryptoPreferred ? walletManager.getCryptoForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx())) : walletManager.getFiatForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx()), null);

            mStartingBalance.setText(CurrencyUtils.getFormattedAmount(getActivity(), iso, startingBalance == null ? null : startingBalance.abs()));
            mEndingBalance.setText(CurrencyUtils.getFormattedAmount(getActivity(), iso, endingBalance == null ? null : endingBalance.abs()));

            mTxAction.setText(sent ? "Sent" : "Received");
            mToFrom.setText(sent ? "To " : "Via ");
            String toAddress = getToAddress(walletManager, mTransaction.getTo());

            mToFromAddress.setText(walletManager.decorateAddress(getActivity(), toAddress)); //showing only the destination address

            // Allow the to/from address to be copyable
            mToFromAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Get the default color based on theme
                    final int color = mToFromAddress.getCurrentTextColor();

                    mToFromAddress.setTextColor(getContext().getColor(R.color.light_gray));
                    String address = mToFromAddress.getText().toString();
                    BRClipboardManager.putClipboard(getContext(), address);
                    Toast.makeText(getContext(), getString(R.string.Receive_copied), Toast.LENGTH_LONG).show();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mToFromAddress.setTextColor(color);

                        }
                    }, 200);


                }
            });

            mTxAmount.setText(CurrencyUtils.getFormattedAmount(getActivity(), walletManager.getIso(getActivity()), cryptoAmount));//this is always crypto amount


            if (!sent)
                mTxAmount.setTextColor(getContext().getColor(R.color.transaction_amount_received_color));
            else
                mTxAmount.setTextColor(getContext().getColor(R.color.viewfinder_laser));

//            // Set the memo text if one is available
//            String memo;
//            TxMetaData txMetaData = KVStoreManager.getInstance().getTxMetaData(getActivity(), mTransaction.getTxHash());
//
//            if (txMetaData != null) {
//                Log.d(TAG, "TxMetaData not null");
//                if (txMetaData.comment != null) {
//                    Log.d(TAG, "Comment not null");
//                    memo = txMetaData.comment;
//                    mMemoText.setText(memo);
//                } else {
//                    Log.d(TAG, "Comment is null");
//                    mMemoText.setText("");
//                }
//
//                String metaIso = Utils.isNullOrEmpty(txMetaData.exchangeCurrency) ? "USD" : txMetaData.exchangeCurrency;
//
//                exchangeRateFormatted = CurrencyUtils.getFormattedAmount(getActivity(), metaIso, new BigDecimal(txMetaData.exchangeRate));
//                mExchangeRate.setText(exchangeRateFormatted);
//            } else {
//                Log.d(TAG, "TxMetaData is null");
//                mMemoText.setText("");
//
//            }

                // timestamp is 0 if it's not confirmed in a block yet so make it now
                mTxDate.setText(BRDateUtil.getLongDate(mTransaction.getTimeStamp() == 0 ? System.currentTimeMillis() : (mTransaction.getTimeStamp() * 1000)));

            // Set the transaction id
            mTransactionId.setText(mTransaction.getTxHashHexReversed());

            // Allow the transaction id to be copy-able
            mTransactionId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Get the default color based on theme
                    final int color = mTransactionId.getCurrentTextColor();

                    mTransactionId.setTextColor(getContext().getColor(R.color.light_gray));
                    String id = mTransaction.getTxHashHexReversed();
                    BRClipboardManager.putClipboard(getContext(), id);
                    Toast.makeText(getContext(), getString(R.string.Receive_copied), Toast.LENGTH_LONG).show();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mTransactionId.setTextColor(color);

                        }
                    }, 200);
                }
            });

            // Set the transaction block number
            if (mTransaction.getBlockHeight() != 2147483647)
                mConfirmedInBlock.setText(String.valueOf(mTransaction.getBlockHeight()));
            else mConfirmedInBlock.setText("Unconfirmed");
            if (mTransaction.getAsset() != null) {
                MyTransactionAsset mAsset = mTransaction.getAsset();
                String amount;
                if (mAsset.getName().endsWith("!"))
                    amount = mAsset.getName();
                else {
                    Asset localAsset = AssetsRepository.getInstance(getContext()).getAsset(mAsset.getName());
                    int unit = localAsset != null ? localAsset.getUnits() : mAsset.getUnit();
                    double assetAmount = new BigDecimal(mAsset.getAmount() / SATOSHIS).doubleValue();
                    amount = com.platform.assets.Utils.formatAssetAmount(assetAmount, unit) + " " + mAsset.getName();
                }
                mTxAmount.setText(amount);
                layoutStartingBalance.setVisibility(View.GONE);
                layoutEndingBalance.setVisibility(View.GONE);
//                layoutExchangeRate.setVisibility(View.GONE);
                mDetailsContainer.setVisibility(View.VISIBLE);
                mShowHide.setVisibility(View.GONE);
//                mAmountWhenSent.setVisibility(View.GONE);
//                mAmountNow.setVisibility(View.GONE);
//                labelWhenSent.setVisibility(View.GONE);
//                labelAmountNow.setVisibility(View.GONE);
            }
//            layoutMemo.setVisibility(View.GONE);
        } else {

            Toast.makeText(getContext(), "Error getting transaction data", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Nullable
    private String getToAddress(BaseWalletManager wallet, String[] tos) {
        String toAddress = "";
        if (tos != null)
            for (String to : tos) {
                boolean isMyAddress = wallet.getWallet().containsAddress(new BRCoreAddress(to));
                if (!isMyAddress) {
                    toAddress = to;
                    break;
                }
            }
        if (TextUtils.isEmpty(toAddress) && tos != null)
            toAddress = tos[0];
        return toAddress;
    }
}
