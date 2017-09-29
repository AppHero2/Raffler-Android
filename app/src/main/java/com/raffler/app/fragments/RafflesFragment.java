package com.raffler.app.fragments;


import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.lid.lib.LabelImageView;
import com.raffler.app.WalletActivity;
import com.raffler.app.R;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.alertView.OnItemClickListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Raffle;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.TimeUtil;
import com.raffler.app.utils.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.INPUT_METHOD_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class RafflesFragment extends Fragment {

    private Query query;
    private DatabaseReference rafflesRef, usersRef, holdersRef;
    private ChildEventListener childEventListener;

    private List<Raffle> raffles = new ArrayList<>();
    private RafflesAdapter adapter;

    private int entering_point = 1;
    private User mUser;

    public RafflesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        usersRef = References.getInstance().usersRef;
        rafflesRef = References.getInstance().rafflesRef;
        holdersRef = References.getInstance().holdersRef;
        query = rafflesRef.orderByChild("isClosed").equalTo(false);

        mUser = AppManager.getSession();
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

        TextView emptyView = (TextView) view.findViewById(R.id.tv_empty);
        ListView listView = (ListView) view.findViewById(R.id.list_raffles);
        listView.setEmptyView(emptyView);
        adapter = new RafflesAdapter(getActivity(), raffles);
        adapter.startUpdateTimer();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Cell cell = (Cell) view.getTag();
                final Raffle raffle = cell.getData();
                final long raffles_num = raffle.getRaffles_num();
                boolean isExpired = cell.isExpired();
                if (isExpired) {
                    Util.showAlert(getString(R.string.alert_title_notice),
                            getString(R.string.raffles_alert_expired), getActivity());
                } else {

                    final int user_raffle_point = AppManager.getSession().getRaffle_point();
                    if (user_raffle_point < 1) {
                        Util.showAlert(getString(R.string.alert_title_notice),
                                getString(R.string.raffles_alert_no_enough), getActivity());
                    } else {

                        AlertView alertView = new AlertView(getString(R.string.raffles_alert_entering), null, "Cancel", new String[]{"Okay"}, null, getActivity(), AlertView.Style.Alert, new OnItemClickListener() {
                            @Override
                            public void onItemClick(Object o, int position) {
                                if (position != AlertView.CANCELPOSITION) {

                                    User user = AppManager.getSession();
                                    for (int i=0; i<entering_point; i++){
                                        Map<String, Object> holder = new HashMap<>();
                                        holder.put("uid", user.getIdx());
                                        holder.put("phone", user.getPhone());
                                        holder.put("pushToken", user.getPushToken());
                                        holdersRef.child(raffle.getIdx()).push().setValue(holder);
                                    }

                                    Map<String, Object> dicRaffle = new HashMap<>();
                                    long holding_count = 1;
                                    if (user.isExistRaffle(raffle.getIdx())){
                                        String strValue = (String) user.getRaffles().get(raffle.getIdx());
                                        holding_count = Integer.valueOf(strValue) + entering_point;
                                    }

                                    String strHoldNum = String.valueOf(holding_count);
                                    dicRaffle.put(raffle.getIdx(), strHoldNum);
                                    usersRef.child(user.getIdx()).child("raffles").updateChildren(dicRaffle);
                                    usersRef.child(user.getIdx()).child("raffle_point").setValue((user_raffle_point - entering_point));

                                    // analysis
                                    Bundle params = new Bundle();
                                    params.putString("raffler", user.getIdx());
                                    References.getInstance().analytics.logEvent("enter_raffle", params);

                                    hideSoftKeyboard();

                                    Toast.makeText(getActivity(), getString(R.string.raffles_alert_good_luck), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        alertView.setCancelable(false);

                        ViewGroup extView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.layout_input_points, null);
                        final EditText etPoint = (EditText) extView.findViewById(R.id.et_points);
                        etPoint.setText(String.valueOf(entering_point));
                        etPoint.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void afterTextChanged(Editable editable) {
                                String value = etPoint.getText().toString();
                                if (!value.isEmpty())
                                    entering_point = Integer.valueOf(value);
                                else
                                    entering_point = 0;

                                if (entering_point > user_raffle_point) {
                                    Util.showAlert(getString(R.string.alert_title_notice), getString(R.string.raffles_alert_no_enough), getActivity());
                                    entering_point = user_raffle_point;
                                    etPoint.setText(String.valueOf(entering_point));
                                } else if (entering_point <= 0) {
                                    Util.showAlert(getString(R.string.alert_title_notice), getString(R.string.raffles_alert_minimum), getActivity());
                                    entering_point = 1;
                                    etPoint.setText(String.valueOf(entering_point));
                                }
                            }
                        });
                        ColorStateList csl_default = new ColorStateList(new int[][]{new int[0]}, new int[]{0xffffd700});
                        AppCompatButton btnMinus = (AppCompatButton) extView.findViewById(R.id.btn_minus); btnMinus.setSupportBackgroundTintList(csl_default);
                        AppCompatButton btnPlus = (AppCompatButton) extView.findViewById(R.id.btn_plus); btnPlus.setSupportBackgroundTintList(csl_default);
                        btnMinus.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (entering_point > 1) {
                                    entering_point -= 1;
                                }

                                etPoint.setText(String.valueOf(entering_point));
                            }
                        });
                        btnPlus.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (entering_point < user_raffle_point) {
                                    entering_point += 1;
                                }

                                etPoint.setText(String.valueOf(entering_point));
                            }
                        });

                        alertView.addExtView(extView);
                        alertView.show();
                    }
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_wallet);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), WalletActivity.class));
            }
        });

        startTrackingRaffles();

        AppManager.getInstance().setUserValueListenerForRaffles(new UserValueListener() {
            @Override
            public void onLoadedUser(User user) {
                if (user != null) {
                    mUser = user;
                    adapter.notifyDataSetChanged();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_raffles_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void startTrackingRaffles(){

        if (childEventListener != null) return;

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> raffleData = (Map<String, Object>) dataSnapshot.getValue();
                    updateRaffle(raffleData);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> raffleData = (Map<String, Object>) dataSnapshot.getValue();
                    updateRaffle(raffleData);
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
                FirebaseCrash.report(databaseError.toException());
            }
        };
        query.addChildEventListener(childEventListener);
    }

    private void stopTrackingRaffles(){
        if (childEventListener != null)
            query.removeEventListener(childEventListener);
    }

    /**
     * Hides the soft keyboard
     */
    public void hideSoftKeyboard() {
        if(getActivity().getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Shows the soft keyboard
     */
    public void showSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }

    private void updateRaffle(Map<String, Object> raffleData) {
        Raffle raffle = new Raffle(raffleData);
        if (raffle.getIdx() == null)
            return;

        boolean isExist = false;
        for (Raffle item : raffles) {
            if (item.getIdx().equals(raffle.getIdx())) {
                item.updateValue(raffleData);
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
                cellView = this.layoutInflater.inflate(R.layout.row_raffle, null);
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
        public TextView txtTimer;
        public TextView txtTitle;
        public TextView txtDescription;
        public ImageView imgMarker;
        private Raffle raffle;
        private boolean isExpired = false;

        public Cell(View itemView) {
            imgCover = (LabelImageView) itemView.findViewById(R.id.img_cell_cover);
            txtTimer = (TextView) itemView.findViewById(R.id.txt_cell_timer);
            txtTitle = (TextView) itemView.findViewById(R.id.txt_cell_title);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_cell_description);
            imgMarker = (ImageView) itemView.findViewById(R.id.img_cell_marker);
        }

        public void setData(Raffle raffle){
            this.raffle = raffle;
            Util.setURLImage(raffle.getImageLink(), imgCover);
            imgCover.setLabelDistance(20);
            imgCover.setLabelHeight(30);
            imgCover.setLabelText("R" + 0);
            txtTitle.setText(raffle.getTitle());
            txtDescription.setText(raffle.getDescription());

            if (mUser.isExistRaffle(raffle.getIdx())){
                String holding_count = (String) mUser.getRaffles().get(raffle.getIdx());
                imgCover.setLabelText("R" + holding_count);
                imgMarker.setVisibility(View.VISIBLE);
                imgMarker.setImageResource(R.drawable.ic_raffle_flag);
            } else if (raffle.isExistWinner(mUser.getIdx())) {
                imgMarker.setVisibility(View.VISIBLE);
                imgMarker.setImageResource(R.drawable.ic_raffle_winner);
            } else {
                imgMarker.setVisibility(View.GONE);
            }

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
                this.isExpired = false;
            } else {
                txtTimer.setText("Expired!!");
                this.isExpired = true;
            }
        }
    }
}
