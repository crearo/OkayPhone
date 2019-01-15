package com.crearo.okayphone;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;

import com.crearo.okayphone.commands.main.MainPack;
import com.crearo.okayphone.commands.tuixt.TuixtActivity;
import com.crearo.okayphone.managers.ContactManager;
import com.crearo.okayphone.managers.RegexManager;
import com.crearo.okayphone.managers.TerminalManager;
import com.crearo.okayphone.managers.TimeManager;
import com.crearo.okayphone.managers.notifications.KeeperService;
import com.crearo.okayphone.managers.notifications.NotificationManager;
import com.crearo.okayphone.managers.notifications.NotificationMonitorService;
import com.crearo.okayphone.managers.notifications.NotificationService;
import com.crearo.okayphone.managers.suggestions.SuggestionsManager;
import com.crearo.okayphone.managers.xml.XMLPrefsManager;
import com.crearo.okayphone.managers.xml.options.Behavior;
import com.crearo.okayphone.managers.xml.options.Notifications;
import com.crearo.okayphone.managers.xml.options.Theme;
import com.crearo.okayphone.managers.xml.options.Ui;
import com.crearo.okayphone.tuils.Assist;
import com.crearo.okayphone.tuils.CustomExceptionHandler;
import com.crearo.okayphone.tuils.LongClickableSpan;
import com.crearo.okayphone.tuils.PrivateIOReceiver;
import com.crearo.okayphone.tuils.PublicIOReceiver;
import com.crearo.okayphone.tuils.SimpleMutableEntry;
import com.crearo.okayphone.tuils.Tuils;
import com.crearo.okayphone.tuils.interfaces.Inputable;
import com.crearo.okayphone.tuils.interfaces.Outputable;
import com.crearo.okayphone.tuils.interfaces.Reloadable;

public class LauncherActivity extends AppCompatActivity implements Reloadable {

    private final String FIRSTACCESS_KEY = "x3";

    public static final int COMMAND_REQUEST_PERMISSION = 10;
    public static final int STARTING_PERMISSION = 11;
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;

    public static final int TUIXT_REQUEST = 10;

    private UIManager ui;
    private MainManager main;

    private PrivateIOReceiver privateIOReceiver;
    private PublicIOReceiver publicIOReceiver;

    private boolean openKeyboardOnStart, canApplyTheme;

    private Runnable stopActivity = new Runnable() {
        @Override
        public void run() {
            dispose();
            finish();

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startActivity(startMain);
        }
    };

    private Inputable in = new Inputable() {

        @Override
        public void in(String s) {
            if(ui != null) ui.setInput(s);
        }

        @Override
        public void changeHint(final String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ui.setHint(s);
                }
            });
        }

        @Override
        public void resetHint() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ui.resetHint();
                }
            });
        }
    };

    private Outputable out = new Outputable() {

        private final int DELAY = 500;

        Queue<SimpleMutableEntry<CharSequence,Integer>> textColor = new LinkedList<>();
        Queue<SimpleMutableEntry<CharSequence,Integer>> textCategory = new LinkedList<>();

        boolean charged = false;
        Handler handler = new Handler();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(ui == null) {
                    handler.postDelayed(this, DELAY);
                    return;
                }

                SimpleMutableEntry<CharSequence,Integer> sm;
                while ((sm = textCategory.poll()) != null) {
                    ui.setOutput(sm.getKey(), sm.getValue());
                }

                while ((sm = textColor.poll()) != null) {
                    ui.setOutput(sm.getValue(), sm.getKey());
                }

                textCategory = null;
                textColor = null;
                handler = null;
                r = null;
            }
        };

        @Override
        public void onOutput(CharSequence output) {
            if(ui != null) ui.setOutput(output, TerminalManager.CATEGORY_OUTPUT);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, TerminalManager.CATEGORY_OUTPUT));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void onOutput(CharSequence output, int category) {
            if(ui != null) ui.setOutput(output, category);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, category));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void onOutput(int color, CharSequence output) {
            if(ui != null) ui.setOutput(color, output);
            else {
                textColor.add(new SimpleMutableEntry<>(output, color));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void dispose() {
            if(handler != null) handler.removeCallbacksAndMessages(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        if (isFinishing()) {
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, LauncherActivity.STARTING_PERMISSION);
        } else {
            canApplyTheme = true;
            finishOnCreate();
        }
    }

    private void finishOnCreate() {

        Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());

        XMLPrefsManager.loadCommons(this);
        new RegexManager(LauncherActivity.this);
        new TimeManager();

        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateIOReceiver.ACTION_INPUT);
        filter.addAction(PrivateIOReceiver.ACTION_OUTPUT);
        filter.addAction(PrivateIOReceiver.ACTION_REPLY);

        privateIOReceiver = new PrivateIOReceiver(this, out, in);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(privateIOReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(PublicIOReceiver.ACTION_CMD);
        filter1.addAction(PublicIOReceiver.ACTION_OUTPUT);

        publicIOReceiver = new PublicIOReceiver();
        getApplicationContext().registerReceiver(publicIOReceiver, filter1);

        int requestedOrientation = XMLPrefsManager.getInt(Behavior.orientation);
        if(requestedOrientation != 2 && requestedOrientation != -1) {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation != requestedOrientation) setRequestedOrientation(requestedOrientation);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color)) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(XMLPrefsManager.getColor(Theme.statusbar_color));
            window.setNavigationBarColor(XMLPrefsManager.getColor(Theme.navigationbar_color));
        }

        boolean showNotification = XMLPrefsManager.getBoolean(Behavior.tui_notification);
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (showNotification) {
            keeperIntent.putExtra(KeeperService.PATH_KEY, XMLPrefsManager.get(Behavior.home_path));
            startService(keeperIntent);
        } else {
            try {
                stopService(keeperIntent);
            } catch (Exception e) {}
        }

        boolean fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen);

        boolean useSystemWP = XMLPrefsManager.getBoolean(Ui.system_wallpaper);
        if (useSystemWP) {
            if(fullscreen) {
                setTheme(R.style.Custom_SystemWP_Fullscreen);
            } else {
                setTheme(R.style.Custom_SystemWP);
            }
        } else {
            if(fullscreen) {
                setTheme(R.style.Custom_Solid_Fullscreen);
            } else {
                setTheme(R.style.Custom_Solid);
            }
        }

        try {
            NotificationManager.create(this);
        } catch (Exception e) {
            Tuils.toFile(e);
        }

        boolean notifications = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");
        if(notifications) {
            try {
                ComponentName notificationComponent = new ComponentName(this, NotificationService.class);
                PackageManager pm = getPackageManager();
                pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                if(!Tuils.hasNotificationAccess(this)) {
                    Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    if(i.resolveActivity(getPackageManager()) == null) {
                        Toast.makeText(this, R.string.no_notification_access, Toast.LENGTH_LONG).show();
                    } else {
                        startActivity(i);
                    }
                }

                Intent monitor = new Intent(this, NotificationMonitorService.class);
                startService(monitor);

                Intent notificationIntent = new Intent(this, NotificationService.class);
                notificationIntent.putExtra(Theme.time_color.label(), XMLPrefsManager.getColor(Theme.time_color));
                startService(notificationIntent);
            } catch (NoClassDefFoundError er) {
                Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + Tuils.SPACE + er.toString());
            }
        }

        LongClickableSpan.longPressVibrateDuration = XMLPrefsManager.getInt(Behavior.long_click_vibration_duration);

        openKeyboardOnStart = XMLPrefsManager.getBoolean(Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setContentView(R.layout.base_view);

        main = new MainManager(this);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);
        ui = new UIManager(this, mainView, main.getMainPack(), canApplyTheme, main.executer());

        main.setRedirectionListener(ui.buildRedirectionListener());
        ui.pack = main.getMainPack();

        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        if(fullscreen) Assist.assistActivity(this);

        SharedPreferences preferences = getPreferences(0);
        SharedPreferences.Editor editor = preferences.edit();

        boolean firstAccess = preferences.getBoolean(FIRSTACCESS_KEY, true);
        if (firstAccess) {
            editor.putBoolean(FIRSTACCESS_KEY, false);

            ui.setOutput(getString(R.string.firsthelp_text), TerminalManager.CATEGORY_OUTPUT);
            ui.setInput("tutorial");
        }

        editor.apply();

        System.gc();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ui != null) ui.onStart(openKeyboardOnStart);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_SUGGESTIONS));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ui != null && main != null) {
            ui.pause();
            main.dispose();
        }
    }

    private boolean disposed = false;
    private void dispose() {
        if(disposed) return;

        try {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(privateIOReceiver);
            getApplicationContext().unregisterReceiver(publicIOReceiver);
        } catch (Exception e) {}

        try {
            stopService(new Intent(this, NotificationMonitorService.class));
        } catch (NoClassDefFoundError | Exception e) {
            Tuils.log(e);
        }

        try {
            stopService(new Intent(this, KeeperService.class));
        } catch (NoClassDefFoundError | Exception e) {
            Tuils.log(e);
        }

        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            notificationIntent.putExtra(NotificationService.DESTROY, true);
            startService(notificationIntent);
        } catch (Exception e) {
            Tuils.log(e);
        }

        overridePendingTransition(0,0);

        if(main != null) main.destroy();
        if(ui != null) ui.dispose();

        XMLPrefsManager.dispose();
        RegexManager.instance.dispose();
        TimeManager.instance.dispose();

        disposed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dispose();
    }

    @Override
    public void onBackPressed() {
        if (main != null) {
            ui.onBackPressed();
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            return super.onKeyLongPress(keyCode, event);

        if (main != null)
            main.onLongBack();
        return true;
    }

    @Override
    public void reload() {
        runOnUiThread(stopActivity);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && ui != null) {
            ui.focusTerminal();
        }
    }

    SuggestionsManager.Suggestion suggestion;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);

        if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
            ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;

            menu.setHeaderTitle(contact.name);
            for(int count = 0; count < contact.numbers.size(); count++) {
                menu.add(0, count, count, contact.numbers.get(count));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(suggestion != null) {
            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;
                contact.setSelectedNumber(item.getItemId());

                Tuils.sendInput(this, suggestion.getText());

                return true;
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == TUIXT_REQUEST && resultCode != 0) {
            if(resultCode == TuixtActivity.BACK_PRESSED) {
                Tuils.sendOutput(this, R.string.tuixt_back_pressed);
            } else {
                Tuils.sendOutput(this, data.getStringExtra(TuixtActivity.ERROR_KEY));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(permissions.length > 0 && permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(ContactManager.ACTION_REFRESH));
        }

        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, null, false);
                    } else {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                        main.sendPermissionNotGrantedWarning();
                    }
                    break;
                case STARTING_PERMISSION:
                    int count = 0;
                    while(count < permissions.length && count < grantResults.length) {
                        if(grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();

                                    try {
                                        sleep(2000);
                                    } catch (InterruptedException e) {}

                                    runOnUiThread(stopActivity);
                                }
                            }.start();
                            return;
                        }
                        count++;
                    }
                    canApplyTheme = false;
                    finishOnCreate();
                    break;
                case COMMAND_SUGGESTION_REQUEST_PERMISSION:
                    if (grantResults.length == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                    }
                    break;
            }
        } catch (Exception e) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);
        if(cmd != null) {
            Intent i = new Intent(MainManager.ACTION_EXEC);
            i.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            i.putExtra(MainManager.CMD, cmd);
            i.putExtra(MainManager.NEED_WRITE_INPUT, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
