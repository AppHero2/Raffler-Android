package com.raffler.app;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.lid.lib.LabelImageView;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.News;
import com.raffler.app.models.Prize;
import com.raffler.app.models.Raffle;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {

    private static final String TAG = "WalletActivity";

    private String userId;
    private int badgeCount = 0;

    private List<Prize> prizes = new ArrayList<>();

    private PrizesAdapter adapter;

    private Query query;
    private ChildEventListener childEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        TextView emptyView = (TextView) findViewById(R.id.emptyView);
        ListView lstPrize = (ListView) findViewById(R.id.lst_prize);
        lstPrize.setEmptyView(emptyView);
        lstPrize.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cell cell = (Cell) view.getTag();
                final Prize prize = cell.getPrize();
                if (!prize.isDelivered()){
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("message/rfc822");
                    intent.putExtra(Intent.EXTRA_EMAIL  , new String[]{"recipient@example.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
                    intent.putExtra(Intent.EXTRA_TEXT   , "body of email");
                    try {
                        startActivity(Intent.createChooser(intent, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(WalletActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        adapter = new PrizesAdapter(this, prizes);
        lstPrize.setAdapter(adapter);

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

        startTrackingPrizes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTrackingPrizes();
    }

    private void updateStatus(){
        badgeCount = prizes.size();

    }

    private void startTrackingPrizes(){
        if (childEventListener != null){
            return;
        }

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> newsData = (Map<String, Object>) dataSnapshot.getValue();
                    updateNewsData(newsData);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> newsData = (Map<String, Object>) dataSnapshot.getValue();
                    updateNewsData(newsData);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> prizeData = (Map<String, Object>) dataSnapshot.getValue();
                    Prize prize = new Prize(prizeData);
                    for (Prize item : prizes) {
                        if (item.getIdx().equals(prize.getIdx())) {
                            prizes.remove(item);
                            break;
                        }
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        query = References.getInstance().prizesRef.child(userId);
        query.addChildEventListener(childEventListener);
    }

    private void stopTrackingPrizes(){
        query.removeEventListener(childEventListener);
    }

    private void updateNewsData(Map<String, Object> data){
        Prize prize = new Prize(data);
        if (prize.getIdx() == null)
            return;

        boolean isExist = false;
        for (Prize item : prizes) {
            if (item.getIdx().equals(prize.getIdx())) {
                item.updateValue(data);
                isExist = true;
                break;
            }
        }

        if (!isExist) {
            prizes.add(prize);
        }

        adapter.notifyDataSetChanged();
    }

    private class PrizesAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater layoutInflater;
        private List<Prize> items = new ArrayList<>();
        private List<Cell> cells = new ArrayList<>();

        public PrizesAdapter(Context context, List<Prize> items){
            this.context = context;
            this.items = items;
            this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Prize getItem(int i) {
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
                cellView = this.layoutInflater.inflate(R.layout.row_raffle, null);
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
        private View mView;
        public LabelImageView imgCover;
        public TextView txtTimer;
        public TextView txtTitle, txtDescription;
        public ImageView imgMarker;
        private Prize prize;

        public Cell(View itemView) {
            mView = itemView;
            imgCover = (LabelImageView) itemView.findViewById(R.id.img_cell_cover);
            txtTimer = (TextView) itemView.findViewById(R.id.txt_cell_timer);
            txtTitle = (TextView) itemView.findViewById(R.id.txt_cell_title);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_cell_description);
            imgMarker = (ImageView) itemView.findViewById(R.id.img_cell_marker);
        }

        public void setData(Prize prize){
            this.prize = prize;
            Util.setURLImage(prize.getImageLink(), imgCover);
            imgCover.setLabelDistance(20);
            imgCover.setLabelHeight(30);
            imgCover.setLabelText(getString(R.string.wallet_winner));
            txtTitle.setText(prize.getTitle());
            txtDescription.setText(prize.getDescription());

            if (prize.isDelivered()) {
                imgMarker.setVisibility(View.VISIBLE);
                imgMarker.setImageResource(R.drawable.ic_raffle_delivered);
            } else {
                imgMarker.setVisibility(View.GONE);
            }

            String date = Util.getDateString(prize.getUpdatedAt());
            String todayDate = Util.getDateString(System.currentTimeMillis());

            if (todayDate.compareTo(date) == 0) {
                txtTimer.setText(Util.getUserTime(prize.getUpdatedAt()));
            } else {
                txtTimer.setText(Util.getUserFriendlyDate(
                        mView.getContext(),
                        prize.getUpdatedAt().getTime()
                ));
            }
        }

        public Prize getPrize() {
            return prize;
        }
    }
}
