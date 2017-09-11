package com.raffler.app;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.lid.lib.LabelImageView;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.Raffle;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;
import com.raffler.app.widgets.CustomTextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

public class PrizeWalletActivity extends AppCompatActivity {

    private static final String TAG = "PrizeWalletActivity";

    private RelativeLayout layout_inbox;
    private TextView tvBadge;

    private String userId;
    private int badgeCount = 0;

    private List<Raffle> prizes = new ArrayList<>();

    private PrizesAdapter adapter;

    private KProgressHUD hud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prize_wallet);

        hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setWindowColor(ContextCompat.getColor(this, R.color.colorTransparency))
                .setDimAmount(0.5f);

        TextView emptyView = (TextView) findViewById(R.id.emptyView);
        ListView lstPrize = (ListView) findViewById(R.id.lst_prize);
        lstPrize.setEmptyView(emptyView);

        adapter = new PrizesAdapter(this, prizes);
        lstPrize.setAdapter(adapter);

        layout_inbox = (RelativeLayout) findViewById(R.id.layout_inbox);
        tvBadge = (TextView) findViewById(R.id.tv_badge);
        ImageView btnBack = (ImageView) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        User user = AppManager.getSession();
        userId = user.getIdx();

        updateStatus();

        refreshData();
    }

    private void updateStatus(){
        badgeCount = prizes.size();
        tvBadge.setText(String.valueOf(badgeCount));
        if (badgeCount == 0) {
            tvBadge.setVisibility(View.GONE);
        } else {
            tvBadge.setVisibility(View.VISIBLE);
        }
    }

    private void refreshData(){

        hud.show();
        Query query = References.getInstance().prizesRef.child(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> prizeData = (Map<String, Object>) dataSnapshot.getValue();
                    for (Map.Entry<String, Object> entry : prizeData.entrySet()){
                        String rafflerId = entry.getKey();
                        Map<String, Object> raffleMap = (Map<String, Object>)entry.getValue();
                        Raffle raffle = new Raffle(raffleMap);
                        prizes.add(raffle);
                    }
                }
                hud.dismiss();
                adapter.notifyDataSetChanged();

                updateStatus();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
                hud.dismiss();
            }
        });
    }

    private class PrizesAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater layoutInflater;
        private List<Raffle> items = new ArrayList<>();
        private List<Cell> cells = new ArrayList<>();

        public PrizesAdapter(Context context, List<Raffle> items){
            this.context = context;
            this.items = items;
            this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Raffle getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {

            Cell cell;
            View cellView = convertView;
            if (convertView == null) {
                cellView = this.layoutInflater.inflate(R.layout.row_raffle_list, null);
                cell = new Cell(cellView);
                cellView.setTag(cell);
            } else {
                cell = (Cell) cellView.getTag();
            }

            cell.setData(getItem(position));

            return cellView;
        }
    }

    private class Cell {
        public LabelImageView imgCover;
        public TextView txtTimer;
        public TextView txtDescription;
        public ImageView imgMarker;
        private Raffle raffle;

        public Cell(View itemView) {
            imgCover = (LabelImageView) itemView.findViewById(R.id.img_cell_cover);
            txtTimer = (TextView) itemView.findViewById(R.id.txt_cell_timer);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_cell_description);
            imgMarker = (ImageView) itemView.findViewById(R.id.img_cell_marker);
        }

        public void setData(Raffle raffle){
            this.raffle = raffle;
            Util.setURLImage(raffle.getImageLink(), imgCover);
            imgCover.setLabelDistance(20);
            imgCover.setLabelHeight(30);
            imgCover.setLabelText("R" + raffle.getRaffles_num());
            txtDescription.setText(raffle.getDescription());

            if (raffle.isExistWinner(userId)) {
                imgMarker.setVisibility(View.VISIBLE);
                imgMarker.setImageResource(R.drawable.ic_raffle_winner);
            } else if (raffle.isExistDelivered(userId)) {
                imgMarker.setVisibility(View.VISIBLE);
                imgMarker.setImageResource(R.drawable.ic_raffle_delivered);
            } else {
                imgMarker.setVisibility(View.GONE);
            }
        }

        public Raffle getData() {
            return this.raffle;
        }

    }
}
