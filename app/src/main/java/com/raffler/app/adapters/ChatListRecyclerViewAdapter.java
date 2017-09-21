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
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.ChatInfo;
import com.raffler.app.models.ChatType;
import com.raffler.app.models.Contact;
import com.raffler.app.models.Message;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.raffler.app.utils.Util.getMapDataFromData;

public class ChatListRecyclerViewAdapter extends RecyclerView.Adapter<ChatListRecyclerViewAdapter.ViewHolder> {

    private final List<ChatInfo> mValues;
    private ChatItemClickListener chatItemClickListener;

    public void setChatItemClickListener(ChatItemClickListener chatItemClickListener) {
        this.chatItemClickListener = chatItemClickListener;
    }

    public ChatListRecyclerViewAdapter(List<ChatInfo> items) {
        mValues = items;
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

        if (holder.mItem.getLastMessage() == null) {
            holder.loadDataFromId(holder.mItem.getIdx());
        } else {
            holder.updateInfo(holder.mItem);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView tvUsername;
        public final TextView tvMessage;
        public final TextView tvDate;
        private final TextView tvUnreadCount;
        private final View countLayout;
        private final ImageView countImageView;
        private ImageView imgProfile;

        public ChatInfo mItem;
        public User mUser;
        public String mMessageId;
        public String mPhoneContactId, mPhoneContactName, mPhoneContactNumber;
        public long mUnreadCount = 0;
        public int typeTextColor;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            tvUsername = (TextView) view.findViewById(R.id.tv_username);
            tvMessage = (TextView) view.findViewById(R.id.tv_message);
            tvDate = (TextView) view.findViewById(R.id.tv_date);
            tvUnreadCount = (TextView) view.findViewById(R.id.tv_unreadcount);
            countImageView = (ImageView) view.findViewById(R.id.imageView_count);
            countLayout = view.findViewById(R.id.layout_count);
            imgProfile = (ImageView) view.findViewById(R.id.img_profile);

            typeTextColor = tvDate.getCurrentTextColor();
        }

        public void updateInfo(ChatInfo info){

            String idx = info.getIdx();
            String[] userIds = Util.getUserIdsFrom(idx);
            String contactId = null;
            for (String userId : userIds) {
                if (!userId.equals(AppManager.getInstance().userId)) {
                    contactId = userId;
                }
            }

            String userPhoto = (String) info.getPhotos().get(contactId);
            String userPhone = (String) info.getPhones().get(contactId);
            String lastMessage = info.getLastMessage();
            mMessageId = info.getLastMessageId();
            long unreadCount = info.getUnreadCount();
            Date updatedAt = info.getUpdatedAt();

            updateDate(updatedAt);

            updateContactName(userPhone);

            updateUnreadCount(unreadCount);

            updateLastMessage(lastMessage);

            updateContactPhoto(userPhoto);

            AppManager.getUser(contactId, new UserValueListener() {
                @Override
                public void onLoadedUser(final User user) {
                    if (user != null) {
                        mUser = user;
                    }
                }
            });

            updateOnClickListener();
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

                            mPhoneContactNumber = (String) phones.get(contactId);
                            updateContactName(mPhoneContactNumber);

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

            updateUnreadCount(0);
        }

        private void updateLastMessage(String message){
            String lastMessage = message;
            String[] lastMessages = lastMessage.split("\n");
            if (lastMessages.length > 1) {
                if (lastMessages[0].length() > 25)
                    lastMessage = lastMessages[0].substring(0, 25).concat("...");
                else
                    lastMessage = lastMessages[0].concat("...");
            } else if (lastMessage.length() > 25) {
                lastMessage = lastMessage.substring(0, 25).concat("...");
            }

            tvMessage.setText(lastMessage);
        }

        private void updateDate(Date date){
            String dateString = Util.getDateString(date);
            String todayDate = Util.getDateString(System.currentTimeMillis());
            if (todayDate.compareTo(dateString) == 0) {
                tvDate.setText(Util.getUserTime(date));
            } else {
                tvDate.setText(Util.getUserFriendlyDate(
                        mView.getContext(),
                        date.getTime()
                ));
            }
        }

        private void updateUnreadCount(long count){
            mUnreadCount = count;
            if(mUnreadCount == 0){
                tvDate.setTextColor(typeTextColor);
                countLayout.setVisibility(View.GONE);
            } else {
                int color = tvDate.getResources().getColor(R.color.colorEmerland);
                tvDate.setTextColor(color);
                tvUnreadCount.setText(String.valueOf(mUnreadCount));
                countLayout.setVisibility(View.VISIBLE);
                countImageView.setColorFilter(color);
            }
        }

        private void updateContactName(String phone) {
            mPhoneContactNumber = phone;
            String phoneContactId = AppManager.getPhoneContactId(phone);
            if (phoneContactId != null){
                mPhoneContactId = phoneContactId;
                Contact contact = AppManager.getContacts().get(phoneContactId);
                mPhoneContactName = contact.getName();
                tvUsername.setText(mPhoneContactName);
            } else {
                mPhoneContactId = null;
                mPhoneContactName = null;
                tvUsername.setText(phone);

                try {
                    // phone must begin with '+'
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    PhoneNumber numberProto = phoneUtil.parse(phone, "");
                    String formatedPhoneNumber = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                    tvUsername.setText(formatedPhoneNumber);

                } catch (NumberParseException e) {
                    System.err.println("NumberParseException was thrown: " + e.toString());
                }
            }
        }

        private void updateContactPhoto(String photo) {
            Util.setProfileImage(photo, imgProfile);
        }

        private void updateOnClickListener(){
            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (chatItemClickListener != null) {
                        Chat chat = new Chat(mUser, mMessageId, mUnreadCount);
                        chatItemClickListener.onSelectedChat(chat);
                    }
                }
            });
        }

        private void updateData(final Message message, String contactId) {

            mMessageId = message.getIdx();

            imgProfile.setImageResource(R.drawable.ic_profile_person);
            AppManager.getUser(contactId, new UserValueListener() {
                @Override
                public void onLoadedUser(final User user) {
                    if (user != null) {
                        if (mPhoneContactNumber == null)
                            tvUsername.setText(user.getName());
                        Util.setProfileImage(user.getPhoto(), imgProfile);
                        mUser = user;
                    }
                }
            });
            String lastMessage = message.getText();
            updateLastMessage(lastMessage);

            updateDate(message.getUpdatedAt());

            updateOnClickListener();
        }

        @Override
        public String toString() {
            return super.toString() + " '" + tvMessage.getText() + "'";
        }
    }
}
