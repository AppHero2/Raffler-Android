package com.raffler.app.adapters;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
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
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.google.firebase.database.DatabaseReference;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.User;
import com.raffler.app.models.UserType;
import com.raffler.app.utils.Blur;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;
import com.raffler.app.widgets.ATextView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.IOException;
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
                    .inflate(R.layout.row_chat_date_label, parent, false);
            return new DateLabelViewHolder(view);

        } else if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_left, parent, false);
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

        /*holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });*/

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

        switch (holder.mItem.getMessageType()) {
            case TEXT:
                holder.layoutPhoto.setVisibility(View.GONE);
                break;
            case PHOTO:
                holder.layoutPhoto.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.imgPhoto.setImageBitmap(null);
                Uri imageUri = holder.mItem.getAttachFilePath();
                if (imageUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(holder.mView.getContext().getContentResolver(), imageUri);
                        Bitmap blurred = Blur.fastblur(holder.mView.getContext(), bitmap, 10);
                        holder.imgPhoto.setImageBitmap(blurred);
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                String imageURL = holder.mItem.getResource();
                if (imageURL != null) {
                    holder.loadPhoto(imageURL);
                }
                break;
            case AUDIO:
                holder.layoutPhoto.setVisibility(View.GONE);
                break;
            case VIDEO:
                holder.layoutPhoto.setVisibility(View.GONE);
                break;
            case LOCATION:
                holder.layoutPhoto.setVisibility(View.GONE);
                break;
            default:
                holder.layoutPhoto.setVisibility(View.GONE);
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

        holder.imgStatus.setImageResource(resId);

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
        public final ATextView messageView;
        public final TextView dateView;
        private final ImageView imgStatus;
        private ImageView imgPhoto;
        private RelativeLayout layoutPhoto;
        private ProgressBar progressBar;
        public Message mItem;

        public OwnMessageViewHolder(View view) {
            super(view);
            mView = view;
            messageView = (ATextView) view.findViewById(R.id.tv_message);
            dateView = (TextView) view.findViewById(R.id.textView_datetime);
            imgStatus = (ImageView) view.findViewById(R.id.img_status);
            imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
            layoutPhoto = (RelativeLayout) view.findViewById(R.id.layout_photo);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

            imgPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bitmap bitmap = ((BitmapDrawable)imgPhoto.getDrawable()).getBitmap();
                    if (bitmap != null) {
                        Dialog builder = new Dialog(mView.getContext());
                        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        builder.getWindow().setBackgroundDrawable(
                                new ColorDrawable(android.graphics.Color.TRANSPARENT));
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                //nothing;
                            }
                        });
                        ImageView imageView = new ImageView(mView.getContext());
                        imageView.setImageBitmap(bitmap);
                        //below code fullfil the requirement of xml layout file for dialoge popup

                        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        builder.show();
                    }
                }
            });
        }

        Transformation blurTransformation = new Transformation() {
            @Override
            public Bitmap transform(Bitmap source) {
                Bitmap blurred = Blur.fastblur(mView.getContext(), source, 10);
                source.recycle();
                return blurred;
            }

            @Override
            public String key() {
                return "blur()";
            }
        };

        public void loadPhoto(final String url){
            if (url == null) return;
            layoutPhoto.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            final int imageViewWidth = imgPhoto.getMeasuredWidth()==0?480:imgPhoto.getMeasuredWidth();
            final int imageViewHeight = imgPhoto.getMeasuredHeight()==0?250:imgPhoto.getMeasuredHeight();
            Picasso.with(mView.getContext())
                    .load(url) // thumbnail url goes here
                    .placeholder(imgPhoto.getDrawable())
//                    .resize(imageViewWidth, imageViewHeight)
                    .transform(blurTransformation)
                    .into(imgPhoto, new Callback() {
                        @Override
                        public void onSuccess() {
                            Picasso.with(mView.getContext())
                                    .load(url) // image url goes here
//                                    .resize(imageViewWidth, imageViewHeight)
                                    .placeholder(imgPhoto.getDrawable())
                                    .into(imgPhoto);
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                        }
                    });
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
