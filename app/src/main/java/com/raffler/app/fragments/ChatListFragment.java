package com.raffler.app.fragments;

import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.raffler.app.R;
import com.raffler.app.adapters.ChatListRecyclerViewAdapter;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.interfaces.UnreadMessageListener;
import com.raffler.app.models.ChatInfo;
import com.raffler.app.models.User;
import com.raffler.app.tasks.LoadContactsTask;
import com.raffler.app.utils.References;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {

    private int mColumnCount = 1;

    private User user;
    private DatabaseReference userChatListRef;
    private Query queryChatList;

    private List<ChatInfo> chatInfoList = new ArrayList<>();
    private ValueEventListener valueEventListener;

    private TextView tvEmpty;
    private RecyclerView recyclerView;

    private ChatListRecyclerViewAdapter adapter;

    private UnreadMessageListener unreadMessageListener;
    private ChatItemClickListener chatItemClickListener;

    public ChatListFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        user = AppManager.getSession();
        userChatListRef = References.getInstance().usersRef.child(user.getIdx()).child("chats");

        tvEmpty = (TextView) view.findViewById(R.id.txtNoData);
        recyclerView = (RecyclerView) view.findViewById(R.id.listChats);
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mColumnCount));
        }

        adapter = new ChatListRecyclerViewAdapter(chatInfoList);
        recyclerView.setAdapter(adapter);
        if (chatItemClickListener != null) {
            adapter.setChatItemClickListener(chatItemClickListener);
        }

        startTrackingUserChatList();

        getActivity().getApplication().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true,new MyContentObserver(new Handler()));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopTrackingUserChatList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void updateStatus(){
        if (chatInfoList.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void startTrackingUserChatList(){
        if (valueEventListener != null)
            return;

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    chatInfoList.clear();
                    Map<String, Object> chatInfoData = (Map<String, Object>) dataSnapshot.getValue();
                    for (Map.Entry<String, Object> entry : chatInfoData.entrySet()) {
                        String idx = entry.getKey();
                        ChatInfo chatInfo = new ChatInfo(idx);
                        try {
                            Map<String, Object> infoData = (Map<String, Object>) entry.getValue();
                            chatInfo.updateValue(infoData);
                            if (unreadMessageListener != null) {
                                unreadMessageListener.onUnreadMessages(idx, chatInfo.getUnreadCount());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        chatInfoList.add(chatInfo);
                    }

                    updateStatus();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        };
        queryChatList = userChatListRef;
        queryChatList.addValueEventListener(valueEventListener);
    }

    private void stopTrackingUserChatList(){
        if (valueEventListener != null)
            queryChatList.removeEventListener(valueEventListener);
    }

    public void setUnreadMessageListener(UnreadMessageListener unreadMessageListener) {
        this.unreadMessageListener = unreadMessageListener;
    }

    public void setChatItemClickListener(ChatItemClickListener chatItemClickListener) {
        this.chatItemClickListener = chatItemClickListener;
    }
    
    /**
     * observer to check changed contacts
     */
    private class MyContentObserver extends ContentObserver {

        public MyContentObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {

            try
            {
                super.onChange(selfChange);

                /*Uri callUri =ContactsContract.CommonDataKinds.Email.CONTENT_URI;
                Cursor cur =  cr.query(callUri, null, null, null, null);
                while (cur.moveToNext()) {
                    String contact_id = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID));
                    String display_name = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME));
                    String data = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                    String content_Type = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE));
                    String type = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));

                    Log.d("------ ic_contact2 id : "+contact_id+"----", "----onChange fired by content ---observer--------");
                    Log.d("------display_name : "+display_name+"----", "----onChange fired by content ---observer--------");
                    Log.d("------data : "+data+"----", "----onChange fired by content ---observer--------");
                    Log.d("------content_Type : "+content_Type+"----", "----onChange fired by content ---observer--------");
                    Log.d("------type : "+type+"----", "----onChange fired by content ---observer--------");
                }*/

                // analysis
                Bundle params = new Bundle();
                params.putString("contact_changed", "");
                References.getInstance().analytics.logEvent("contact_changed", params);

                new LoadContactsTask(new ResultListener() {
                    @Override
                    public void onResult(boolean success) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (chatInfoList != null) {
                                        if (chatInfoList.size() > 0) {
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }).execute("");

            }catch(Exception e){e.printStackTrace();
                FirebaseCrash.report(e);
            }
        }

        @Override
        public boolean deliverSelfNotifications() {

            return true;
        }

    }
}
