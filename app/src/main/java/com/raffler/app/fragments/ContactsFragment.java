package com.raffler.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.raffler.app.FindContactActivity;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContactsFragment extends Fragment {

    private DatabaseReference usersRef, contactsRef;

    private ListView listView;
    private KProgressHUD hud;
    private FloatingActionButton btnNewContact;

    private List<String> contacts = new ArrayList<>();

    private ContactListAdapter adapter;
    private ChatItemClickListener listener;

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

        usersRef = References.getInstance().usersRef;
        contactsRef = References.getInstance().contactsRef.child(AppManager.getInstance().userId);

        TextView txtNoData = (TextView) view.findViewById(R.id.txtNoData);
        listView = (ListView) view.findViewById(R.id.list_contacts);
        listView.setEmptyView(txtNoData);

        adapter = new ContactListAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Cell cell = (Cell) view.getTag();

                if (listener != null) {
                    Chat chat = new Chat(cell.user, null, 0);
                    listener.onSelectedChat(chat);
                }

            }
        });

        hud = KProgressHUD.create(getActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setWindowColor(ContextCompat.getColor(getActivity(), R.color.colorTransparency))
                .setDimAmount(0.5f);

        btnNewContact = (FloatingActionButton) view.findViewById(R.id.fab_contact);
        btnNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), FindContactActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadContacts();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contacts_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
           /* case R.id.action_contacts:
                startActivity(new Intent(getActivity(), FindContactActivity.class));
                return true;*/
            case R.id.action_refresh:

                // TODO: 18/8/2017 refresh

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadContacts(){
        contacts.clear();

        Query query = contactsRef;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot child: dataSnapshot.getChildren()){
                        String userId = child.getKey();
                        contacts.add(userId);
                    }

                    adapter.notifyDataSetChanged();
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

    public void setListener(ChatItemClickListener listener) {
        this.listener = listener;
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
        public String getItem(int position) {
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

            cell.setUserData(getItem(position));

            return convertView;
        }
    }

    private class Cell {
        public ImageView imgProfile;
        public TextView txtName, txtBio;
        public User user;

        public Cell(View view){
            this.imgProfile = (ImageView)view.findViewById(R.id.imgProfile);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtBio = (TextView) view.findViewById(R.id.txtBio);
        }

        public void setUserData(String userId){
            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        Map<String, Object> userData = (Map<String, Object>) dataSnapshot.getValue();
                        user = new User(userData);

                        String name = user.getName();
                        String photo = user.getPhoto();

                        txtName.setText(name);
                        if (photo != null)
                            Util.setProfileImage(photo, imgProfile);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("contact list", databaseError.toString());
                }
            });
        }
    }
}
