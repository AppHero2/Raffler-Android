package com.raffler.app.adapters;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.firebase.database.DatabaseReference;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.User;
import com.raffler.app.models.UserType;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private User mUser;
    private final List<Message> mValues;
    private final String chatId, lastMessageId;

    public ChatRecyclerViewAdapter(List<Message> items, String chatId, String lastMessageId) {
        mValues = items;
        this.chatId = chatId;
        this.lastMessageId = lastMessageId;
        this.mUser = AppManager.getSession();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view;
        if (viewType == 2) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_chat_date_label, parent, false);
            return new DateLabelViewHolder(view);

        } else if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_chat_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_chat_left, parent, false);
        }
        return new OwnMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateLabelViewHolder) {
            onBindViewHolder((DateLabelViewHolder) holder, position);
        } else {
            onBindViewHolder((OwnMessageViewHolder) holder, position);
        }
    }

    public void onBindViewHolder(final DateLabelViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.labelView.setText(holder.mItem.getText());
    }

    public void onBindViewHolder(final OwnMessageViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        String text = String.format(
                Locale.getDefault(),
                "%s &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
                , holder.mItem.getText()
        );
        final SpannableString styledResultText = new SpannableString(text);
        styledResultText.setSpan((new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE)),
                text.length() - 5, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan((new ForegroundColorSpan(Color.GRAY)),
                text.length() - 5, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.messageView.setText(Html.fromHtml(text));

        holder.dateView.setText(
                Util.getMessageTime(holder.mItem.getUpdatedAt()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 22/8/2017 click message view
            }
        });

        if (holder.mItem.getUserType() != UserType.SELF) {
            if (holder.mItem.getIdx().equals(lastMessageId)) {
                References.getInstance().chatsRef.child(chatId).child("status").setValue(MessageStatus.READ.ordinal());
            }

            if (holder.mItem.getStatus() != MessageStatus.READ) {
                DatabaseReference reference = References.getInstance().messagesRef.child(chatId).child(holder.mItem.getIdx()).child("status");
                reference.setValue(MessageStatus.READ.ordinal());

                Map<String, Object> lastSeen = new HashMap<>();
                lastSeen.put(chatId, System.currentTimeMillis());
                References.getInstance().usersRef.child(mUser.getIdx()).child("lastSeen").updateChildren(lastSeen);
            }

        }

        int resId;
        switch (holder.mItem.getStatus()) {
            case SENT:
                resId = R.drawable.ic_sent_gray_24dp;
                break;
            case DELIVERED:
                resId = R.drawable.ic_receive_gray_24dp;
                break;
            case READ:
                resId = R.drawable.ic_read_cyan_24dp;
                break;
            default:
                resId = R.drawable.ic_schedule_gray_24dp;

        }

        holder.statusView.setImageResource(resId);

    }

    @Override
    public int getItemViewType(int position) {
        return mValues.get(position).getUserType().ordinal();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class OwnMessageViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView messageView;
        public final TextView dateView;
        private final ImageView statusView;
        public Message mItem;

        public OwnMessageViewHolder(View view) {
            super(view);
            mView = view;
            messageView = (TextView) view.findViewById(R.id.textView_message);
            dateView = (TextView) view.findViewById(R.id.textView_datetime);
            statusView = (ImageView) view.findViewById(R.id.imageView_status);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + dateView.getText() + "'";
        }
    }

    public class DateLabelViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView labelView;
        public Message mItem;

        public DateLabelViewHolder(View view) {
            super(view);
            mView = view;
            labelView = (TextView) view.findViewById(R.id.textView_label);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

}
