package com.raffler.app.fragments;

import android.os.Bundle;
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
import com.raffler.app.models.RealmContact;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.realm.RealmList;

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
    public ResultListener contactUpdatedListener;

    public ChatListFragment() {
        contactUpdatedListener = new ResultListener() {
            @Override
            public void onResult(boolean success) {
                if (success) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (chatInfoList != null) {
                                    if (chatInfoList.size() > 0) {
                                        RealmList<RealmContact> phoneContacts = AppManager.getContacts(getActivity());
                                        adapter.setPhoneContacts(phoneContacts);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        });
                    }
                }
            }
        };
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

        RealmList<RealmContact> phoneContacts = AppManager.getContacts(getActivity());
        adapter = new ChatListRecyclerViewAdapter(chatInfoList, phoneContacts);
        recyclerView.setAdapter(adapter);
        if (chatItemClickListener != null) {
            adapter.setChatItemClickListener(chatItemClickListener);
        }

        startTrackingUserChatList();

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
}
