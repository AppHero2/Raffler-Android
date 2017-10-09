package com.raffler.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.Contact;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactsFragment extends Fragment {

    private static final int REQUEST_CONTACT = 711;
    private static final int REQUEST_INVITE = 712;

    private static final String TAG = "ContactsFragment";

    private User mUser;
    private DatabaseReference usersRef, contactsRef;

    private ListView listView;
    private FloatingActionButton btnNewContact;

    private MenuItem progressBar;

    private List<Contact> contacts = new ArrayList<>();
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

        mUser = AppManager.getSession();
        usersRef = References.getInstance().usersRef;
        contactsRef = References.getInstance().contactsRef.child(mUser.getIdx());

        TextView txtNoData = (TextView) view.findViewById(R.id.txtNoData);
        listView = (ListView) view.findViewById(R.id.list_contacts);
        listView.setEmptyView(txtNoData);

        adapter = new ContactListAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (i < contacts.size()) {
                    Cell cell = (Cell) view.getTag();
                    if (listener != null) {
                        if (cell.userId != null){
                            Chat chat = new Chat(cell.userName, cell.userId, null, 0);
                            listener.onSelectedChat(chat);
                        }
                    }
                } else if (i == contacts.size()){

                    String link_val = "https://play.google.com/store/apps/details?id=" + getActivity().getPackageName();
//                    String body = "<a href=" + link_val + ">" + link_val + "</a>";
                    String data = "Hey,\n\n" +
                            "I'm trying this new texting app called Raffler.\n" +
                            "You get raffle points for each text, and you can win free prizes.\n\n" + link_val;
                    Intent sendIntent = new Intent();
                    sendIntent.setType("text/plain");
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Awesome Raffler app");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, data);
                    startActivityForResult(Intent.createChooser(sendIntent, getString(R.string.contact_invite)) , REQUEST_INVITE);

                } else {
                    Toast.makeText(getActivity(), "This feature is coming soon.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*btnNewContact = (FloatingActionButton) view.findViewById(R.id.fab_contact);
        btnNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                *//*  set profile photo
                ArrayList<ContentValues> data = new ArrayList<ContentValues>();
                Bitmap bit = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                ContentValues row = new ContentValues();
                row.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bitmapToByteArray(bit));
                data.add(row);*//*

                Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
                // Sets the MIME type to match the Contacts Provider
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                // Inserts a Phone number
                *//*intent.putExtra(ContactsContract.Intents.Insert.PHONE, mPhoneNumber.getText())*//*

                *//* set profile photo
                intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);*//*

                startActivityForResult(intent, REQUEST_CONTACT);
            }
        });*/

        return view;
    }

    /**
     * This function is prepared for update ic_contact2 profile
     * @param bitmap
     * @return
     */
    private byte[] bitmapToByteArray(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK) {

            // Get the URI and query the content provider for the phone number
            Uri contactUri = data.getData();

            /* This method was removed at git v1.0.2 b51~52
            AppManager.getInstance().addNewContact(contactUri, new ResultListener() {
                @Override
                public void onResult(boolean success) {
                    // ???
                }
            });*/

        } else if (requestCode == REQUEST_INVITE && resultCode == RESULT_OK) {
            User user = AppManager.getSession();
            long raffles_point = user.getRaffle_point();
            usersRef.child(user.getIdx()).child("raffle_point").setValue(raffles_point + 5);
            Util.showAlert(getString(R.string.alert_title_notice), getString(R.string.contact_alert_earning_extra), getActivity());
        } else if (requestCode == REQUEST_INVITE){
            User user = AppManager.getSession();
            long raffles_point = user.getRaffle_point();
            usersRef.child(user.getIdx()).child("raffle_point").setValue(raffles_point + 5);
            Util.showAlert(getString(R.string.alert_title_notice), getString(R.string.contact_alert_earning_extra), getActivity());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // refresh contact list once entering foreground
        new FindContactsTask().execute("");
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        progressBar = menu.findItem(R.id.menu_refresh);
        super.onPrepareOptionsMenu(menu);
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
            case R.id.menu_refresh:
                findContactsInLocal();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * this method is used to check contacts between local & server.
     */
    private void findContactsInLocal(){
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressBar != null) progressBar.setVisible(true);
                }
            });
        }
        Query query = References.getInstance().contactListRef;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> userData = (Map<String, Object>) dataSnapshot.getValue();
                    for (Map.Entry<String, Object> entry : userData.entrySet()) {
                        String key = entry.getKey();
                        Map<String, Object> data = (Map<String, Object>) entry.getValue();
                        String phone = (String) data.get("phone");
                        String photo = (String) data.get("photo");

                        try {
                            // phone must begin with '+'
                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phone, "");
                            String countryCode = "+" + String.valueOf(numberProto.getCountryCode());

                            Contact contact = AppManager.getInstance().getPhoneContact(phone, countryCode);
                            if (contact != null) {
                                contact.setUid(key);
                                contact.setPhoto(photo);
                                boolean isExist = false;
                                for (Contact c : contacts) {
                                    if (c.getPhone().equals(contact.getPhone())){
                                        isExist = true;
                                        break;
                                    }
                                }
                                if (!isExist) {
                                    if (!mUser.getPhone().equals(contact.getPhone())) contacts.add(contact);
                                }
                            }

                        } catch (NumberParseException e) {
                            System.err.println("NumberParseException was thrown: " + e.toString());
                        }

                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressBar != null) progressBar.setVisible(false);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        });
    }

    public void setListener(ChatItemClickListener listener) {
        this.listener = listener;
    }

    public class FindContactsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            findContactsInLocal();

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, result);
        }

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute");
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private class ContactListAdapter extends BaseAdapter {

        private Context context;
        private LayoutInflater inflater;

        public ContactListAdapter(Context context){
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position < contacts.size()){
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getCount() {
            return contacts.size() + 2;
        }

        @Override
        public Contact getItem(int position) {
            return contacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (position < contacts.size()) {

                Cell cell;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_contact_cell, null);
                    cell = new Cell(convertView);
                    convertView.setTag(cell);
                } else {
                    cell = (Cell) convertView.getTag();
                }

                Contact contact = getItem(position);
                if (contact != null) {
                    cell.setUserData(getItem(position));
                }
                return convertView;

            } else {
                CellExtra cell;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.row_contact_extra, null);
                    cell = new CellExtra(convertView);
                    convertView.setTag(cell);
                } else {
                    cell = (CellExtra) convertView.getTag();
                }

                if (position == contacts.size()) {
                    cell.imgIcon.setImageResource(R.drawable.ic_contact_invite);
                    cell.tvTitle.setText(getString(R.string.contact_invite));
                } else {
                    cell.imgIcon.setImageResource(R.drawable.ic_contact_help);
                    cell.tvTitle.setText(getString(R.string.contact_help));
                }

                return convertView;
            }
        }
    }

    private class Cell {
        public ImageView imgProfile;
        public TextView txtName, txtBio;
        public String userId, userName, userPhoto;

        public Cell(View view){
            this.imgProfile = (ImageView) view.findViewById(R.id.img_profile);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtBio = (TextView) view.findViewById(R.id.txtBio);
        }

        public void setUserData(Contact contact){
            userId = contact.getUid();
            userPhoto = contact.getPhoto();
            userName = contact.getName();
            txtName.setText(userName);
            Util.setProfileImage(userPhoto, imgProfile);
        }
    }

    private class CellExtra {
        public ImageView imgIcon;
        public TextView tvTitle;
        public CellExtra(View view){
            this.tvTitle = (TextView) view.findViewById(R.id.tv_title);
            this.imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
        }
    }
}
