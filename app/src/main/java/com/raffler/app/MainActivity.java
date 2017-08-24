package com.raffler.app;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OSPermissionObserver;
import com.onesignal.OSPermissionStateChanges;
import com.onesignal.OneSignal;
import com.raffler.app.adapters.ViewPagerAdapter;
import com.raffler.app.classes.AppManager;
import com.raffler.app.fragments.ChatListFragment;
import com.raffler.app.fragments.ContactsFragment;
import com.raffler.app.fragments.RafflesFragment;
import com.raffler.app.interfaces.ChatItemClickListener;
import com.raffler.app.interfaces.UnreadMessageListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.User;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ChatItemClickListener, UnreadMessageListener{

    private TabLayout tabLayout;
    private ViewPager viewPager;

    //Fragments
    private RafflesFragment rafflesFragment;
    private ChatListFragment chatFragment;
    private ContactsFragment contactsFragment;

    private String[] tabTitle={"CHAT", "RAFFLES", "CONTACTS"};
    int[] unreadData ={0, 0, 0};
    Map<String, Integer> unreadCount = new HashMap<>();

    private UnreadMessageListener unreadMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        try
        {
            setupTabIcons();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                viewPager.setCurrentItem(position,false);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // TODO: 21/8/2017 save page index
        viewPager.setCurrentItem(0);

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(false)
                .setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
                    @Override
                    public void notificationOpened(OSNotificationOpenResult result) {
                        // TODO: 7/28/2017 open an activity
                    }
                })
                .init();

        OneSignal.addPermissionObserver(new OSPermissionObserver() {
            @Override
            public void onOSPermissionChanged(OSPermissionStateChanges stateChanges) {
                if (stateChanges.getFrom().getEnabled() &&
                        !stateChanges.getTo().getEnabled()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Notifications Disabled!")
                            .show();
                }
                Log.i("Debug", "onOSPermissionChanged: " + stateChanges);
            }
        });

        OneSignal.setSubscription(true);
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                Log.d("OneSignal", "PlayerID: " + userId + "\nPushToken: " + registrationId);
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                Map<String, Object> pushToken = new HashMap<>();
                pushToken.put("pushToken", userId);
                database.getReference("Users").child(AppManager.getInstance().userId).updateChildren(pushToken);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        // Associate searchable configuration with the SearchView
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_status:
                Toast.makeText(this, "Home Status Click", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_settings:
                Toast.makeText(this, "Home Settings Click", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSelectedChat(Chat chat) {
        AppManager.getInstance().selectedChat = chat;
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }

    @Override
    public void onUnreadMessages(String chatId, int count) {
        unreadCount.put(chatId, count);
        int total_unread_count = 0;
        for (Map.Entry<String, Integer> entry : unreadCount.entrySet()){
            String key = entry.getKey();
            Integer value = entry.getValue();
            total_unread_count += value;
        }
        updateTabBadgeCount(0, total_unread_count);
    }

    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        chatFragment = new ChatListFragment();
        chatFragment.setUnreadMessageListener(this);
        chatFragment.setChatItemClickListener(this);
        rafflesFragment = new RafflesFragment();
        contactsFragment = new ContactsFragment();
        contactsFragment.setListener(this);
        adapter.addFragment(chatFragment,"CHAT");
        adapter.addFragment(rafflesFragment, "RAFFLES");
        adapter.addFragment(contactsFragment,"CONTACTS");
        viewPager.setAdapter(adapter);
    }

    private View prepareTabView(int pos) {
        View view = getLayoutInflater().inflate(R.layout.custom_tab,null);
        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        TextView tv_count = (TextView) view.findViewById(R.id.tv_count);
        tv_title.setText(tabTitle[pos]);
        if(unreadData[pos]>0)
        {
            tv_count.setVisibility(View.VISIBLE);
            tv_count.setText(""+ unreadData[pos]);
        }
        else
            tv_count.setVisibility(View.GONE);


        return view;
    }

    private void setupTabIcons()
    {
        for(int i=0;i<tabTitle.length;i++)
        {
            /*TabLayout.Tab tabitem = tabLayout.newTab();
            tabitem.setCustomView(prepareTabView(i));
            tabLayout.addTab(tabitem);*/

            tabLayout.getTabAt(i).setCustomView(prepareTabView(i));
        }

    }

    private void updateTabBadgeCount(int index, int count){
        View customView = tabLayout.getTabAt(index).getCustomView();
        TextView tv_count = (TextView) customView.findViewById(R.id.tv_count);
        if(count > 0)
        {
            tv_count.setVisibility(View.VISIBLE);
            tv_count.setText(""+count);
        }
        else
            tv_count.setVisibility(View.GONE);
    }
}
