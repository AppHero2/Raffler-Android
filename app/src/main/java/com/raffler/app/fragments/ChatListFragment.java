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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.raffler.app.R;
import com.raffler.app.adapters.ChatListRecyclerViewAdapter;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.interfaces.UnreadMessageListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {

    private int mColumnCount = 1;

    private DatabaseReference chatsRef;

    private List<String> chats = new ArrayList<>();
    private Map<String, Integer> badges = new HashMap<>();
    private List<Query> queryList = new ArrayList<>();
    private List<ValueEventListener> listeners = new ArrayList<>();

    private TextView txtNodata;
    private RecyclerView recyclerView;

    private ChatListRecyclerViewAdapter adapter;

    private UnreadMessageListener unreadMessageListener;
    private ChatItemClickListener chatItemClickListener;
    private UserValueListener userValueListener;

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

        chatsRef = References.getInstance().chatsRef;

        txtNodata = (TextView) view.findViewById(R.id.txtNoData);
        recyclerView = (RecyclerView) view.findViewById(R.id.listChats);
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mColumnCount));
        }

        adapter = new ChatListRecyclerViewAdapter(chats, badges);
        recyclerView.setAdapter(adapter);
        if (chatItemClickListener != null) {
            adapter.setChatItemClickListener(chatItemClickListener);
        }

        loadData();

        UserValueListener listener = new UserValueListener() {
            @Override
            public void onLoadedUser(User user) {
                if (user != null) {
                    loadData();

                    if (userValueListener != null) {
                        userValueListener.onLoadedUser(user);
                    }
                }
            }
        };

        AppManager.getInstance().setUserValueListenerMain(listener);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopTrackingChat();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void loadData(){
        User user = AppManager.getSession();
        if (user.getChats() != null){
            for (Map.Entry<String, Object> entry : user.getChats().entrySet()){
                final String chatId = entry.getKey();
                if (!chats.contains(chatId)) {
                    chats.add(chatId);
                    badges.put(chatId, 0);
                    startTrackingChat(chatId);
                }
            }
            adapter.notifyDataSetChanged();
        }
        updateStatus();
    }

    private void updateStatus(){
        if (chats.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            txtNodata.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            txtNodata.setVisibility(View.GONE);
        }
    }

    private void startTrackingChat(final String chat_Id) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                AppManager.getUnreadMessageCount(chat_Id, new UnreadMessageListener() {
                    @Override
                    public void onUnreadMessages(final String chatId, final int count) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (unreadMessageListener != null) {
                                    unreadMessageListener.onUnreadMessages(chatId, count);
                                }
                                badges.put(chatId, count);
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        Query query = References.getInstance().chatsRef.child(chat_Id);
        query.addValueEventListener(valueEventListener);
        queryList.add(query);
        listeners.add(valueEventListener);
    }

    private void stopTrackingChat(){
        for (int i = queryList.size()-1; i >= 0; i--) {
            Query query = queryList.get(i);
            ValueEventListener listener = listeners.get(i);
            query.removeEventListener(listener);
        }
    }

    public void setUnreadMessageListener(UnreadMessageListener unreadMessageListener) {
        this.unreadMessageListener = unreadMessageListener;
    }

    public void setChatItemClickListener(ChatItemClickListener chatItemClickListener) {
        this.chatItemClickListener = chatItemClickListener;
    }

    public void setUserValueListener(UserValueListener userValueListener) {
        this.userValueListener = userValueListener;
    }
}
