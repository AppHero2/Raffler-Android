package com.raffler.app.fragments;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.lid.lib.LabelImageView;
import com.raffler.app.R;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.alertView.OnItemClickListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.Raffle;
import com.raffler.app.utils.References;
import com.raffler.app.utils.TimeUtil;
import com.raffler.app.utils.Util;
import com.raffler.app.widgets.CustomTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A simple {@link Fragment} subclass.
 */
public class RafflesFragment extends Fragment {

    private DatabaseReference rafflesRef;
    private ChildEventListener childEventListener;

    private List<Raffle> raffles = new ArrayList<>();
    private RafflesAdapter adapter;
    private String userId;

    public RafflesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        rafflesRef = References.getInstance().rafflesRef;
        userId = AppManager.getSession().getIdx();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopTrackingRaffles();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_raffles, container, false);

        ListView listView = (ListView) view.findViewById(R.id.list_raffles);
        adapter = new RafflesAdapter(getActivity(), raffles);
        adapter.startUpdateTimer();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Cell cell = (Cell) view.getTag();
                final Raffle raffle = cell.getData();
                int raffles_num = raffle.getRaffles_num();
                boolean isExpired = cell.isExpired();
                if (isExpired) {
                    Util.showAlert(getString(R.string.alert_title_notice),
                            getString(R.string.raffles_alert_expired), getActivity());
                } else {
                    if (AppManager.getSession().getRaffles() < raffles_num) {
                        Util.showAlert(getString(R.string.alert_title_notice),
                                getString(R.string.raffles_alert_no_enough), getActivity());
                    } else {
                        AlertView alertView = new AlertView(getString(R.string.alert_title_notice),
                                getString(R.string.raffles_alert_make_sure),
                                getString(R.string.alert_button_cancel),
                                new String[]{getString(R.string.alert_button_okay)}, null,
                                getActivity(),
                                AlertView.Style.Alert, new OnItemClickListener() {
                            @Override
                            public void onItemClick(Object o, int position) {
                                if (position != AlertView.CANCELPOSITION) {
                                    Map<String, Object> dicUser = new HashMap<>();
                                    dicUser.put(userId, false);
                                    rafflesRef.child(raffle.getIdx()).updateChildren(dicUser);
                                }
                            }
                        });
                        alertView.show();
                    }
                }
            }
        });

        startTrackingRaffles();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_raffles_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void startTrackingRaffles(){
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> raffleData = (Map<String, Object>) dataSnapshot.getValue();
                    updateMessage(raffleData);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> raffleData = (Map<String, Object>) dataSnapshot.getValue();
                    updateMessage(raffleData);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> raffleData = (Map<String, Object>) dataSnapshot.getValue();
                    Raffle raffle = new Raffle(raffleData);
                    for (Raffle item : raffles) {
                        if (item.getIdx().equals(raffle.getIdx())) {
                            raffles.remove(item);
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
        rafflesRef.addChildEventListener(childEventListener);
    }

    private void stopTrackingRaffles(){
        rafflesRef.removeEventListener(childEventListener);
    }

    private void updateMessage (Map<String, Object> messageData) {
        Raffle raffle = new Raffle(messageData);
        if (raffle.getIdx() == null)
            return;

        boolean isExist = false;
        for (Raffle item : raffles) {
            if (item.getIdx().equals(raffle.getIdx())) {
                item.updateValue(messageData);
                isExist = true;
                break;
            }
        }

        if (!isExist) {
            raffles.add(raffle);
        }
        adapter.notifyDataSetChanged();

    }

    private class RafflesAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater layoutInflater;
        private List<Raffle> items = new ArrayList<>();
        private List<Cell> cells = new ArrayList<>();
        private Handler mHandler = new Handler();
        private Runnable updateRemainingTimeRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (cells) {
                    long currentTime = System.currentTimeMillis();
                    for (Cell cell: cells){
                        cell.updateTimeRemaining(currentTime);
                    }
                }
            }
        };

        public RafflesAdapter(Context context, List<Raffle> items){
            this.context = context;
            this.items = items;
            this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private void startUpdateTimer() {
            Timer tmr = new Timer();
            tmr.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(updateRemainingTimeRunnable);
                }
            }, 1000, 1000);
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
                cellView.requestLayout();
                synchronized (cells) {
                    cells.add(cell);
                }
            } else {
                cell = (Cell) cellView.getTag();
            }

            cell.setData(getItem(position));

            return cellView;
        }
    }

    private class Cell {
        public LabelImageView imgCover;
        public CustomTextView txtTimer;
        public TextView txtDescription;
        private Raffle raffle;
        private boolean isExpired = false;

        public Cell(View itemView) {
            imgCover = (LabelImageView) itemView.findViewById(R.id.img_cell_cover);
            txtTimer = (CustomTextView) itemView.findViewById(R.id.txt_cell_timer);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_cell_description);
        }

        public void setData(Raffle raffle){
            this.raffle = raffle;
            Util.setURLImage(raffle.getImageLink(), imgCover);
            imgCover.setLabelDistance(20);
            imgCover.setLabelHeight(30);
            imgCover.setLabelText("R"+raffle.getRaffles_num());
            txtDescription.setText(raffle.getDescription());
        }

        public Raffle getData() {
            return this.raffle;
        }

        public boolean isExpired() {
            return isExpired;
        }

        public void updateTimeRemaining(long currentTime) {
            long timeDiff = raffle.getEndingAt().getTime() - currentTime;
            if (timeDiff > 0) {
                String remaining = TimeUtil.formatHMSM(timeDiff);
                txtTimer.setText(remaining);
            } else {
                txtTimer.setText("Expired!!");
                this.isExpired = true;
            }
        }
    }
}
