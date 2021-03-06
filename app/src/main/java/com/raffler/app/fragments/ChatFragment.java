package com.raffler.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.raffler.app.R;
import com.raffler.app.adapters.ChatRecyclerViewAdapter;
import com.raffler.app.adapters.NewMessageListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.models.Message;
import com.raffler.app.utils.References;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends BaseFragment {

    private static final String USER_ID = "user_id";
    private static final String CHAT_ID = "chat_id";
    private static final String LAST_ID = "last_id";
    private static final String TAG = ChatFragment.class.getSimpleName();

    private int mColumnCount = 1;
    private int limit = 1000;
    private String userId, chatId, lastMessageId;

    private RecyclerView recyclerView;
    private boolean isMax = false;
    private ChildEventListener newMessagesListener;
    private Query query;

    private List<Message> messages = new ArrayList<>();
    private ChatRecyclerViewAdapter adapter;

    private DatabaseReference messagesRef;

    public NewMessageListener messageListener;

    private ResultListener resultListener;

    public void setResultListener(ResultListener resultListener) {
        this.resultListener = resultListener;
    }

    public ChatFragment() {
        messageListener = new NewMessageListener() {
            @Override
            public void onGetNewMessage(Message message) {
                messages.add(message);
                if (adapter != null)
                    adapter.notifyDataSetChanged();

                readjust();
            }
        };
    }

    public static ChatFragment newInstance(String userId, String chatId, String lastMessageId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putSerializable(USER_ID, userId);
        args.putSerializable(CHAT_ID, chatId);
        args.putSerializable(LAST_ID, lastMessageId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            userId = (String) getArguments().getSerializable(USER_ID);
            chatId = (String) getArguments().getSerializable(CHAT_ID);
            lastMessageId = (String) getArguments().getSerializable(LAST_ID);
            messagesRef = References.getInstance().messagesRef.child(chatId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
                recyclerView.setLayoutManager(linearLayoutManager);
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            adapter = new ChatRecyclerViewAdapter(messages, chatId, lastMessageId);
            recyclerView.setAdapter(adapter);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int y = recyclerView.computeVerticalScrollOffset();

                    android.util.Log.d(TAG, y + "," + recyclerView.computeVerticalScrollOffset() + ", " + recyclerView.computeVerticalScrollExtent() + ", " + recyclerView.getScrollY() + ", " + limit);

                    if (dy < 0)
                        if (y > 0 && y <= 20 && messages.size() >= limit) {
                            limit += 10;
                            query.removeEventListener(newMessagesListener);
                            query = messagesRef.orderByChild("createdAt").limitToLast(limit);
                            query.addChildEventListener(newMessagesListener);
                        }
                }
            });

            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                int height = 0;

                @Override
                public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {

                    if (i7 > i3) {
                        int maxScroll = recyclerView.computeVerticalScrollRange();
                        int currentScroll = recyclerView.computeVerticalScrollOffset() + recyclerView.computeVerticalScrollExtent();

                        final int scroll = currentScroll + Math.abs(i7 - i3);

                        if (scroll >= maxScroll)
                            if (getView() != null)
                                getView().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        recyclerView.scrollBy(0, scroll);

                                    }
                                }, 200);
                    }

                    height = view.getHeight();
                }
            });

            trackMessages();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * this method is used to track messages
     */
    private void trackMessages() {
        query = messagesRef.orderByChild("updatedAt").limitToLast(limit);
        newMessagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    long count = dataSnapshot.getChildrenCount();
                    Log.d(TAG, "" + count);
                    Map<String, Object> messageData = (Map<String, Object>) dataSnapshot.getValue();
                    updateMessage(messageData);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> messageData = (Map<String, Object>) dataSnapshot.getValue();
                    updateMessage(messageData);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    //TODO: remove data from existing
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

        query.addChildEventListener(newMessagesListener);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (resultListener != null)
                    resultListener.onResult(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (resultListener != null)
                    resultListener.onResult(true);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (newMessagesListener != null) {
            query.removeEventListener(newMessagesListener);
        }
    }

    public boolean isMaxScroll() {
        return isMaxScrollReached(recyclerView);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * this method is used to update incoming new message and exist one.
     * @param messageData
     */
    private void updateMessage (Map<String, Object> messageData) {
        Message message = new Message(userId, messageData);
        if (message.getIdx() == null)
            return;

        boolean isExist = false;
        for (Message item : messages) {
            if (item.getIdx().equals(message.getIdx())) {
                item.updateValue(messageData);
                isExist = true;
                break;
            }
        }

        if (!isExist) {
            messages.add(message);
        }
        adapter.notifyDataSetChanged();
        readjust();
    }

    /**
     * this method is used to scroll down for chat list
     */
    public void readjust() {
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
    }

    static private boolean isMaxScrollReached(RecyclerView recyclerView) {
        int maxScroll = recyclerView.computeVerticalScrollRange();

        int currentScroll = recyclerView.computeVerticalScrollOffset() + recyclerView.computeVerticalScrollExtent();

        android.util.Log.d(TAG, maxScroll + ", " + currentScroll);
        if (maxScroll == 0)
            return false;
        return currentScroll >= maxScroll;
    }

}
