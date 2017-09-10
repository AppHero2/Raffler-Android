package com.raffler.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PrizeWalletActivity extends AppCompatActivity {

    private RelativeLayout layout_inbox;
    private TextView tvBadge;

    private int badgeCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prize_wallet);

        TextView emptyView = (TextView) findViewById(R.id.emptyView);
        ListView lstPrize = (ListView) findViewById(R.id.lst_prize);
        lstPrize.setEmptyView(emptyView);

        layout_inbox = (RelativeLayout) findViewById(R.id.layout_inbox);
        tvBadge = (TextView) findViewById(R.id.tv_badge);

        updateStatus();
    }

    private void updateStatus(){
        tvBadge.setText(String.valueOf(badgeCount));
        if (badgeCount == 0) {
            tvBadge.setVisibility(View.GONE);
        } else {
            tvBadge.setVisibility(View.VISIBLE);
        }
    }
}
