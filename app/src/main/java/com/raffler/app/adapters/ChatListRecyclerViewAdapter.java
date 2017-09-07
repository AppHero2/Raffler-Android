package com.raffler.app.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.ChatType;
import com.raffler.app.models.Message;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.List;
import java.util.Map;

import static com.raffler.app.utils.Util.getMapDataFromData;

public class ChatListRecyclerViewAdapter extends RecyclerView.Adapter<ChatListRecyclerViewAdapter.ViewHolder> {

    private final List<String> mValues;
    private final Map<String, Integer> mBadges;
    private ChatItemClickListener chatItemClickListener;

    public void setChatItemClickListener(ChatItemClickListener chatItemClickListener) {
        this.chatItemClickListener = chatItemClickListener;
    }

    public ChatListRecyclerViewAdapter(List<String> items, Map<String, Integer> badges) {
        mValues = items;
        mBadges = badges;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_chatlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        holder.loadDataFromId(holder.mItem);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public final TextView typeView;
        private final TextView countView;
        private final View countLayout;
        private final ImageView countImageView;
        private ImageView imgProfile;
        public String mItem;
        public User mUser;
        public Message mMessage;
        public String mContactPhone;
        public String mContactName;
        public int mUnreadCount;
        public int typeTextColor;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
            typeView = (TextView) view.findViewById(R.id.type);
            countView = (TextView) view.findViewById(R.id.count);
            countImageView = (ImageView) view.findViewById(R.id.imageView_count);
            countLayout = view.findViewById(R.id.layout_count);
            imgProfile = (ImageView) view.findViewById(R.id.imgProfile);

            typeTextColor = typeView.getCurrentTextColor();
        }

        public void loadDataFromId(final String idx) {
            Query query = References.getInstance().chatsRef.child(idx);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        Map<String, Object> messageData = (Map<String, Object>) dataSnapshot.getValue();
                        Message message = new Message(messageData);
                        Map<String, Object> phones = getMapDataFromData("phones", messageData);
                        if (message.getChatType() == ChatType.PERSONAL) {
                            String key = dataSnapshot.getKey();
                            String[] userIds = Util.getUserIdsFrom(key);
                            String contactId = null;
                            for (String userId : userIds) {
                                if (!userId.equals(AppManager.getInstance().userId)) {
                                    contactId = userId;
                                }
                            }

                            mContactPhone = (String) phones.get(contactId);
                            if (AppManager.getInstance().isExistingPhoneContact(mContactPhone)){
                                mContactName = AppManager.getInstance().phoneContacts.get(mContactPhone);
                                mIdView.setText(mContactName);
                            } else {
                                mIdView.setText(mContactPhone);
                            }

                            updateData(message, contactId);

                        } else {

                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ChatListAdapter", databaseError.toString());
                }
            });

            int count = mBadges.get(idx);
            if(count == 0){
                typeView.setTextColor(typeTextColor);
                countLayout.setVisibility(View.GONE);
            } else {
                int color = typeView.getResources().getColor(R.color.colorEmerland);
                typeView.setTextColor(color);
                countView.setText(String.valueOf(count));
                countLayout.setVisibility(View.VISIBLE);
                countImageView.setColorFilter(color);
            }

        }

        private void updateData(final Message message, String contactId) {

            mMessage = message;

            AppManager.getUser(contactId, new UserValueListener() {
                @Override
                public void onLoadedUser(final User user) {
                    if (mContactPhone == null)
                        mIdView.setText(user.getName());
                    Util.setProfileImage(user.getPhoto(), imgProfile);
                    mUser = user;
                }
            });
            String lastMessage = message.getText();
            String[] lastMessages = lastMessage.split("\n");
            if (lastMessages.length > 1) {
                if (lastMessages[0].length() > 25)
                    lastMessage = lastMessages[0].substring(0, 25).concat("...");
                else
                    lastMessage = lastMessages[0].concat("...");
            } else if (lastMessage.length() > 25) {
                lastMessage = lastMessage.substring(0, 25).concat("...");
            }
            mContentView.setText(lastMessage);
            String date = Util.getDateString(message.getUpdatedAt());
            String todayDate = Util.getDateString(System.currentTimeMillis());

            if (todayDate.compareTo(date) == 0) {
                typeView.setText(Util.getUserTime(message.getUpdatedAt()));
            } else {
                typeView.setText(Util.getUserFriendlyDate(
                        mView.getContext(),
                        message.getUpdatedAt().getTime()
                ));
            }

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (chatItemClickListener != null) {
                        Chat chat = new Chat(mUser, mMessage, mUnreadCount);
                        chatItemClickListener.onSelectedChat(chat);
                    }
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
