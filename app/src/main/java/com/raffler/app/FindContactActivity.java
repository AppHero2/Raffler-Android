package com.raffler.app;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.alertView.OnItemClickListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.fragments.ContactsFragment;
import com.raffler.app.models.User;
import com.raffler.app.utils.CircleImageView;
import com.raffler.app.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindContactActivity extends AppCompatActivity {

    private DatabaseReference usersRef;
    private DatabaseReference contactsRef;

    private KProgressHUD hud;
    private ListView listView;

    private FindContactListAdapter adapter;
    private List<User> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_find_contact);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef =  database.getReference("Users");
        contactsRef = database.getReference("Contacts").child(AppManager.getInstance().userId);

        hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setWindowColor(ContextCompat.getColor(this, R.color.colorTransparency))
                .setDimAmount(0.5f);

        // NavigationBar
        TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
        ImageView btnBack = (ImageView) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FindContactActivity.this.finish();
            }
        });
        ImageView btnSearch = (ImageView) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 18/8/2017 search bar
            }
        });

        listView = (ListView) findViewById(R.id.list_users);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final User user = users.get(position);
                new AlertView.Builder().setContext(FindContactActivity.this)
                        .setStyle(AlertView.Style.ActionSheet)
                        .setTitle(user.getName())
                        .setMessage(null)
                        .setCancelText(getString(R.string.alert_button_cancel))
                        .setDestructive(getString(R.string.find_contact_button_add), getString(R.string.find_contact_button_invite))
                        .setOthers(null)
                        .setOnItemClickListener(new OnItemClickListener() {
                            @Override
                            public void onItemClick(Object o, int position) {
                                if (position == 0){
                                    Map<String, Object> userData = new HashMap<>();
                                    long timeStamp = System.currentTimeMillis();
                                    long date = timeStamp/1000;
                                    userData.put(user.getIdx(), date);
                                    contactsRef.updateChildren(userData);
                                    String message = user.getName() + " has been added in your contacts.";
                                    Toast.makeText(FindContactActivity.this, message, Toast.LENGTH_SHORT).show();
                                }else if (position == 1){
                                    // TODO: 19/8/2017 invite friend
                                    Toast.makeText(FindContactActivity.this, "feature coming soon.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .build()
                        .show();
            }
        });

        adapter = new FindContactListAdapter(this);
        listView.setAdapter(adapter);

        loadAllContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void loadAllContacts(){
        hud.show();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot child: dataSnapshot.getChildren()){
                        Map<String, Object> userData = (Map<String, Object>) child.getValue();
                        User user = new User(userData);
                        String userId = AppManager.getInstance().userId;
                        if (!user.getIdx().equals(userId)) {
                            users.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();
                }

                hud.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                hud.dismiss();
            }
        });
    }


    private class FindContactListAdapter extends BaseAdapter {

        private Context context;
        private LayoutInflater inflater;

        public FindContactListAdapter(Context context){
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public User getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        private int lastPosition = -1;
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Cell cell;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_find_user, null);
                cell = new Cell(convertView);
                convertView.setTag(cell);
            } else {
                cell = (Cell) convertView.getTag();
            }

            Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            convertView.startAnimation(animation);
            lastPosition = position;

            cell.setUserData(getItem(position));

            return convertView;
        }
    }

    private class Cell {
        public ImageView imgProfile;
        public TextView txtName, txtBio;
        public Cell(View view){
            this.imgProfile = (ImageView)view.findViewById(R.id.imgProfile);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtBio = (TextView) view.findViewById(R.id.txtBio);
        }

        public void setUserData(User user){
            String name = user.getName();
            String photo = user.getPhoto();

            this.txtName.setText(name);
            if (photo != null)
                Util.setProfileImage(photo, this.imgProfile);
        }
    }
}
