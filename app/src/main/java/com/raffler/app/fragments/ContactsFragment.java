package com.raffler.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.interfaces.ResultListener;
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
    private static final String TAG = "ContactsFragment";

    private DatabaseReference usersRef, contactsRef;

    private ListView listView;
    private KProgressHUD hud;
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

                if (i < contacts.size()) {
                    Cell cell = (Cell) view.getTag();
                    if (listener != null) {
                        if (cell.user != null){
                            Chat chat = new Chat(cell.user, null, 0);
                            listener.onSelectedChat(chat);
                        }
                    }
                } else if (i == contacts.size()){
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Awesome Raffler app");
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            "Hey,\n\n " +
                                    "I'm trying this new texting app called Raffler.\n" +
                                    "You get raffle points for each text, and you can win free prizes.\n\n" +
                                    "https://play.google.com/apps/testing/com.raffler.app");
                                    //"https://play.google.com/store/apps/details?id=com.raffler.app");
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                } else {
                    Toast.makeText(getActivity(), "This feature is coming soon.", Toast.LENGTH_SHORT).show();
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
                /*  set profile photo
                ArrayList<ContentValues> data = new ArrayList<ContentValues>();
                Bitmap bit = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                ContentValues row = new ContentValues();
                row.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bitmapToByteArray(bit));
                data.add(row);*/

                Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
                // Sets the MIME type to match the Contacts Provider
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                // Inserts a Phone number
                /*intent.putExtra(ContactsContract.Intents.Insert.PHONE, mPhoneNumber.getText())*/

                /* set profile photo
                intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);*/

                startActivityForResult(intent, REQUEST_CONTACT);
            }
        });
        return view;
    }

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

            AppManager.getInstance().addNewContact(contactUri, new ResultListener() {
                @Override
                public void onResult(boolean success) {
                    refreshContact();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        loadContacts();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        progressBar = menu.findItem(R.id.action_refresh);
        refreshContact();
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
            case R.id.action_refresh:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshContact(){
        progressBar.setVisible(true);
        AppManager.getInstance().refreshPhoneContacts(new ResultListener() {
            @Override
            public void onResult(boolean success) {
                Log.d(TAG, "didRefresh Contacts");
                progressBar.setVisible(false);
                loadContacts();
            }
        });
    }

    private void loadContacts(){
        contacts.clear();
        Map<String, Contact> mapData = AppManager.getContacts();
        for (Map.Entry<String, Contact> entry : mapData.entrySet()){
            String contactId = entry.getKey();
            Contact contact = entry.getValue();
            if (contact.getUid() != null){
                boolean isExist = false;
                for (Contact item:contacts){
                    if (item.getUid().equals(contact.getUid())){
                        isExist = true;
                    }
                }

                if (!isExist)
                    contacts.add(contact);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private ArrayList<String> loadCustomTypeContacts(){
        Cursor c = getActivity().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                new String[] { ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                new String[] { "com.raffler.app" }, null);

        ArrayList<String> myContacts = new ArrayList<String>();
        int contactNameColumn = c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY);
        while (c.moveToNext())
        {
            // You can also read RawContacts.CONTACT_ID to read the
            // ContactsContract.Contacts table or any of the other related ones.
            myContacts.add(c.getString(contactNameColumn));
        }

        return myContacts;
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

        private int lastPosition = -1;
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

            /*Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            convertView.startAnimation(animation);
            lastPosition = position;*/

                cell.setUserData(getItem(position));
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
        public User user;

        public Cell(View view){
            this.imgProfile = (ImageView)view.findViewById(R.id.imgProfile);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtBio = (TextView) view.findViewById(R.id.txtBio);
        }

        public void setUserData(Contact contact){
            String name = contact.getName();
            String photo = contact.getPhoto();

            txtName.setText(name);
            Util.setProfileImage(photo, imgProfile);

            Query query = References.getInstance().usersRef.orderByChild("uid").equalTo(contact.getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            Map<String, Object> userData = (Map<String, Object>) child.getValue();
                            user = new User(userData);
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, databaseError.toString());
                }
            });
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
