package com.raffler.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.raffler.app.R;
import com.raffler.app.RegisterUserActivity;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.User;
import com.raffler.app.utils.CircleImageView;
import com.raffler.app.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContactsFragment extends Fragment {

    private DatabaseReference contactsRef;
    private String userId;

    private ListView listView;
    private KProgressHUD hud;

    private List<User> contacts = new ArrayList<>();

    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        TextView txtNoData = (TextView) view.findViewById(R.id.txtNoData);
        listView = (ListView) view.findViewById(R.id.list_contacts);
        listView.setEmptyView(txtNoData);

        hud = KProgressHUD.create(getActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setWindowColor(ContextCompat.getColor(getActivity(), R.color.colorTransparency))
                .setDimAmount(0.5f);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = firebaseUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        contactsRef =  database.getReference("Contacts").child(userId);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contacts_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    private void loadContacts(){
        Query query = contactsRef;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot child: dataSnapshot.getChildren()){
                        Map<String, Object> userData = (Map<String, Object>) child.getValue();
                        User user = new User(userData);
                        contacts.add(user);
                    }

                    // TODO: 18/8/2017 reload data
                }

                hud.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getActivity(), databaseError.toString(), Toast.LENGTH_SHORT).show();
                hud.dismiss();
            }
        });
    }


    private class ContactListAdapter extends BaseAdapter {

        private Context context;
        private LayoutInflater inflater;

        public ContactListAdapter(Context context){
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return contacts.size();
        }

        @Override
        public User getItem(int position) {
            return contacts.get(position);
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
                convertView = inflater.inflate(R.layout.row_contact_cell, null);
                cell = new Cell(convertView);
                convertView.setTag(cell);
            } else {
                cell = (Cell) convertView.getTag();
            }

            Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            convertView.startAnimation(animation);
            lastPosition = position;

            return null;
        }
    }

    private class Cell {
        public CircleImageView imgProfile;
        public TextView txtName, txtBio;
        public Cell(View view){
            this.imgProfile = (CircleImageView)view.findViewById(R.id.imgProfile);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtBio = (TextView) view.findViewById(R.id.txtBio);
        }
    }
}
