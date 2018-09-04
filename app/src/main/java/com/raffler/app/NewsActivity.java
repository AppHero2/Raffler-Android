package com.raffler.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.NewsValueListener;
import com.raffler.app.models.News;
import com.raffler.app.models.NewsType;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity implements NewsValueListener {

    private List<News> newsList = new ArrayList<>();
    private NewsAdapter adapter;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        user = AppManager.getSession();

        TextView txtTitle = (TextView) findViewById(R.id.txtTitle); txtTitle.setText(getString(R.string.news_title));
        ImageView btnBack = (ImageView) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewsActivity.this.finish();
            }
        });

        ListView listView = (ListView) findViewById(R.id.lst_notification);
        TextView emptyView = (TextView) findViewById(R.id.emptyView);
        listView.setEmptyView(emptyView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cell cell = (Cell) view.getTag();
                final News news = cell.getNews();
                if (!news.isRead()){
                    References.getInstance().newsRef.child(user.getIdx()).child(news.getIdx()).child("isRead").setValue(true);
                }

                if (news.getType() == NewsType.LOSER) {
                    // analysis
                    Bundle params = new Bundle();
                    params.putString("user", user.getPhone());
                    References.getInstance().analytics.logEvent("lose_raffle", params);
                } else if (news.getType() == NewsType.WINNER) {
                    // analysis
                    Bundle params = new Bundle();
                    params.putString("user", user.getPhone());
                    References.getInstance().analytics.logEvent("win_raffle", params);
                    startActivity(new Intent(NewsActivity.this, WalletActivity.class));
                } else {
                    // other event news.
                }
            }
        });

        newsList = AppManager.getInstance().newsList;
        adapter = new NewsAdapter(this, newsList);
        listView.setAdapter(adapter);

        AppManager.getInstance().setNewsValueListenerForNews(this);
    }

    @Override
    public void onUpdatedNewsList(List<News> newsList) {
        this.newsList = newsList;
        adapter.notifyDataSetChanged();
    }

    private class NewsAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater layoutInflater;
        private List<News> items = new ArrayList<>();
        private List<Cell> cells = new ArrayList<>();

        public NewsAdapter(Context context, List<News> items){
            this.context = context;
            this.items = items;
            this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public News getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {

            Cell cell;
            View cellView = convertView;
            if (convertView == null) {
                cellView = this.layoutInflater.inflate(R.layout.row_news, null);
                cell = new Cell(cellView);
                cellView.setTag(cell);
            } else {
                cell = (Cell) cellView.getTag();
            }

            cell.setData(getItem(position));

            return cellView;
        }
    }

    private class Cell {

        private View mView;
        private TextView tvTitle, tvContent, tvDate;
        private View badgeView;

        private News news;

        public News getNews() {
            return news;
        }

        public Cell(View itemView) {
            mView = itemView;
            tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            tvContent = (TextView) itemView.findViewById(R.id.tv_content);
            tvDate = (TextView) itemView.findViewById(R.id.tv_date);
            badgeView = (View) itemView.findViewById(R.id.badgeView);
        }

        public void setData(News news){
            this.news = news;
            tvTitle.setText(news.getTitle());
            tvContent.setText(news.getContent());
            String date = Util.getDateString(news.getUpdatedAt());
            String todayDate = Util.getDateString(System.currentTimeMillis());

            if (todayDate.compareTo(date) == 0) {
                tvDate.setText(Util.getUserTime(news.getUpdatedAt()));
            } else {
                tvDate.setText(Util.getUserFriendlyDate(
                        mView.getContext(),
                        news.getUpdatedAt().getTime()
                ));
            }

            if (news.isRead()) {
                badgeView.setVisibility(View.GONE);
            } else {
                badgeView.setVisibility(View.VISIBLE);
            }
        }
    }

}
