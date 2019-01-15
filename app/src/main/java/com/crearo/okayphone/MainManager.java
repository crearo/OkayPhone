package com.crearo.okayphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.crearo.okayphone.commands.Command;
import com.crearo.okayphone.commands.CommandGroup;
import com.crearo.okayphone.commands.CommandTuils;
import com.crearo.okayphone.commands.ExecutePack;
import com.crearo.okayphone.commands.main.MainPack;
import com.crearo.okayphone.commands.specific.RedirectCommand;
import com.crearo.okayphone.managers.AliasManager;
import com.crearo.okayphone.managers.AppsManager;
import com.crearo.okayphone.managers.ContactManager;
import com.crearo.okayphone.managers.RssManager;
import com.crearo.okayphone.managers.TerminalManager;
import com.crearo.okayphone.managers.ThemeManager;
import com.crearo.okayphone.managers.TimeManager;
import com.crearo.okayphone.managers.music.MusicManager2;
import com.crearo.okayphone.managers.music.MusicService;
import com.crearo.okayphone.managers.notifications.KeeperService;
import com.crearo.okayphone.managers.xml.XMLPrefsManager;
import com.crearo.okayphone.managers.xml.options.Behavior;
import com.crearo.okayphone.managers.xml.options.Theme;
import com.crearo.okayphone.tuils.Compare;
import com.crearo.okayphone.tuils.PrivateIOReceiver;
import com.crearo.okayphone.tuils.StoppableThread;
import com.crearo.okayphone.tuils.Tuils;
import com.crearo.okayphone.tuils.interfaces.CommandExecuter;
import com.crearo.okayphone.tuils.interfaces.OnRedirectionListener;
import com.crearo.okayphone.tuils.interfaces.Redirectator;
import com.crearo.okayphone.tuils.libsuperuser.Shell;
import com.crearo.okayphone.tuils.libsuperuser.ShellHolder;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

public class MainManager {

    public static String ACTION_EXEC = BuildConfig.APPLICATION_ID + ".main_exec";
    public static String CMD = "cmd", NEED_WRITE_INPUT = "writeInput", ALIAS_NAME = "aliasName", PARCELABLE = "parcelable", CMD_COUNT = "cmdCount", MUSIC_SERVICE = "musicService";
    public static Shell.Interactive interactive;
    public static int commandCount = 0;
    private final String COMMANDS_PKG = ".commands.main.raw";
    //
    String appFormat;
    int timeColor;
    int outputColor;
    Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    Pattern pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private RedirectCommand redirect;
    private OnRedirectionListener redirectionListener;
    private Redirectator redirectator = new Redirectator() {
        @Override
        public void prepareRedirection(RedirectCommand cmd) {
            redirect = cmd;

            if (redirectionListener != null) {
                redirectionListener.onRedirectionRequest(cmd);
            }
        }

        @Override
        public void cleanup() {
            if (redirect != null) {
                redirect.beforeObjects.clear();
                redirect.afterObjects.clear();

                if (redirectionListener != null) {
                    redirectionListener.onRedirectionEnd(redirect);
                }

                redirect = null;
            }
        }
    };
    private CmdTrigger[] triggers = new CmdTrigger[]{
            new GroupTrigger(),
            new AliasTrigger(),
            new TuiCommandTrigger(),
            new AppTrigger(),
            new ShellCommandTrigger()
    };
    private MainPack mainPack;
    private Context mContext;
    private boolean showAliasValue;
    private boolean showAppHistory;
    private int aliasContentColor;
    private String multipleCmdSeparator;
    private AliasManager aliasManager;
    private RssManager rssManager;
    private AppsManager appsManager;
    private ContactManager contactManager;
    private MusicManager2 musicManager2;
    private ThemeManager themeManager;
    private BroadcastReceiver receiver;
    private boolean keeperServiceRunning;

    protected MainManager(LauncherActivity c) {
        mContext = c;

        keeperServiceRunning = XMLPrefsManager.getBoolean(Behavior.tui_notification);

        showAliasValue = XMLPrefsManager.getBoolean(Behavior.show_alias_content);
        showAppHistory = XMLPrefsManager.getBoolean(Behavior.show_launch_history);
        aliasContentColor = XMLPrefsManager.getColor(Theme.alias_content_color);

        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator);

        CommandGroup group = new CommandGroup(mContext, c.getPackageName() + COMMANDS_PKG);

        try {
            contactManager = new ContactManager(mContext);
        } catch (NullPointerException e) {
            Tuils.log(e);
        }

        appsManager = new AppsManager(c);
        aliasManager = new AliasManager(mContext);

        ShellHolder shellHolder = new ShellHolder(mContext);
        interactive = shellHolder.build();

        OkHttpClient client = new OkHttpClient.Builder()
                .cache(new Cache(mContext.getCacheDir(), 10 * 1024 * 1024))
                .build();

        rssManager = new RssManager(mContext, client);

        themeManager = new ThemeManager(client, mContext, c);

        musicManager2 = XMLPrefsManager.getBoolean(Behavior.enable_music) ? new MusicManager2(mContext) : null;

        mainPack = new MainPack(mContext, group, aliasManager, appsManager, musicManager2, contactManager, redirectator, shellHolder, rssManager, client);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_EXEC);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_EXEC)) {
                    int cmdCount = intent.getIntExtra(CMD_COUNT, -1);
                    if (cmdCount < commandCount) return;
                    commandCount++;

                    String cmd = intent.getStringExtra(CMD);
                    if (cmd == null) cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);

                    if (cmd == null) {
                        return;
                    }

                    String aliasName = intent.getStringExtra(ALIAS_NAME);
                    boolean needWriteInput = intent.getBooleanExtra(NEED_WRITE_INPUT, false);
                    Parcelable p = intent.getParcelableExtra(PARCELABLE);

                    if (needWriteInput) {
                        Intent i = new Intent(PrivateIOReceiver.ACTION_INPUT);
                        i.putExtra(PrivateIOReceiver.TEXT, cmd);
                        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
                    }

                    if (p != null) {
                        onCommand(cmd, p, intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false));
                    } else {
                        onCommand(cmd, aliasName, intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false));
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(receiver, filter);
    }

    public void setRedirectionListener(OnRedirectionListener redirectionListener) {
        this.redirectionListener = redirectionListener;
    }

    private void updateServices(String cmd, boolean wasMusicService) {

        if (keeperServiceRunning) {
            Intent i = new Intent(mContext, KeeperService.class);
            i.putExtra(KeeperService.CMD_KEY, cmd);
            i.putExtra(KeeperService.PATH_KEY, mainPack.currentDirectory.getAbsolutePath());
            mContext.startService(i);
        }

        if (wasMusicService) {
            Intent i = new Intent(mContext, MusicService.class);
            mContext.startService(i);
        }
    }

    public void onCommand(String input, Object obj, boolean wasMusicService) {
        if (obj == null || !(obj instanceof AppsManager.LaunchInfo)) {
            onCommand(input, null, wasMusicService);
            return;
        }

        updateServices(input, wasMusicService);

        if (obj instanceof AppsManager.LaunchInfo && ((AppsManager.LaunchInfo) obj).publicLabel.equals(input)) {
            performLaunch((AppsManager.LaunchInfo) obj);
        } else {
            onCommand(input, null, wasMusicService);
        }
    }

    //    command manager
    public void onCommand(String input, String alias, boolean wasMusicService) {
        input = Tuils.removeUnncesarySpaces(input);

        if (alias == null) updateServices(input, wasMusicService);

        if (redirect != null) {
            if (!redirect.isWaitingPermission()) {
                redirect.afterObjects.add(input);
            }
            String output = redirect.onRedirect(mainPack);
            Tuils.sendOutput(mContext, output);

            return;
        }

        if (alias != null && showAliasValue) {
            Tuils.sendOutput(aliasContentColor, mContext, aliasManager.formatLabel(alias, input));
        }

        String[] cmds;
        if (multipleCmdSeparator.length() > 0) {
            cmds = input.split(multipleCmdSeparator);
        } else {
            cmds = new String[]{input};
        }

        for (String cmd : cmds) {
            for (CmdTrigger trigger : triggers) {
                boolean r;
                try {
                    r = trigger.trigger(mainPack, cmd);
                } catch (Exception e) {
                    Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    break;
                }
                if (r) {
                    break;
                }
            }
        }
    }

    public void onLongBack() {
        Tuils.sendInput(mContext, Tuils.EMPTYSTRING);
    }

    public void sendPermissionNotGrantedWarning() {
        redirectator.cleanup();
    }

    public void dispose() {
        mainPack.dispose();
    }

    public void destroy() {
        mainPack.destroy();

        themeManager.dispose();
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).unregisterReceiver(receiver);

        new StoppableThread() {
            @Override
            public void run() {
                super.run();

                try {
                    interactive.kill();
                    interactive.close();
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }
            }
        }.start();
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    public CommandExecuter executer() {
        return new CommandExecuter() {
            @Override
            public void execute(String input, Object obj) {
                onCommand(input, obj, false);
            }
        };
    }

    public boolean performLaunch(AppsManager.LaunchInfo i) {
        Intent intent = appsManager.getIntent(i);
        if (intent == null) {
            return false;
        }

        if (showAppHistory) {
            if (appFormat == null) {
                appFormat = XMLPrefsManager.get(Behavior.app_launch_format);
                timeColor = XMLPrefsManager.getColor(Theme.time_color);
                outputColor = XMLPrefsManager.getColor(Theme.output_color);
            }

            String a = new String(appFormat);
            a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getClassName()));
            a = pp.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getPackageName()));
            a = pl.matcher(a).replaceAll(Matcher.quoteReplacement(i.publicLabel));
            a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            SpannableString text = new SpannableString(a);
            text.setSpan(new ForegroundColorSpan(outputColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            CharSequence s = TimeManager.instance.replace(text, timeColor);

            Tuils.sendOutput(mContext, s, TerminalManager.CATEGORY_OUTPUT);
        }

        mContext.startActivity(intent);

        return true;
    }
//

    interface CmdTrigger {
        boolean trigger(ExecutePack info, String input) throws Exception;
    }

    public interface Group {
        List<? extends Compare.Stringable> members();

        boolean use(MainPack mainPack, String input);

        String name();
    }

    private class AliasTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) {
            String alias[] = aliasManager.getAlias(input, true);

            String aliasValue = alias[0];
            if (alias[0] == null) {
                return false;
            }

            String aliasName = alias[1];
            String residual = alias[2];

            aliasValue = aliasManager.format(aliasValue, residual);

            onCommand(aliasValue, aliasName, false);

            return true;
        }
    }

    private class GroupTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) throws Exception {
            int index = input.indexOf(Tuils.SPACE);
            String name;

            if (index != -1) {
                name = input.substring(0, index);
                input = input.substring(index + 1);
            } else {
                name = input;
                input = null;
            }

            List<? extends Group> appGroups = ((MainPack) info).appsManager.groups;
            if (appGroups != null) {
                for (Group g : appGroups) {
                    if (name.equals(g.name())) {
                        if (input == null) {
                            Tuils.sendOutput(mContext, AppsManager.AppUtils.printApps(AppsManager.AppUtils.labelList((List<AppsManager.LaunchInfo>) g.members(), false)));
                            return true;
                        } else {
                            return g.use(mainPack, input);
                        }
                    }
                }
            }

            return false;
        }
    }

    private class ShellCommandTrigger implements CmdTrigger {

        final int CD_CODE = 10;
        final int PWD_CODE = 11;

        final Shell.OnCommandResultListener pwdResult = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (commandCode == PWD_CODE && output.size() == 1) {
                    File f = new File(output.get(0));
                    if (f.exists()) {
                        mainPack.currentDirectory = f;

                        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_HINT));
                    }
                }
            }
        };

        final Shell.OnCommandResultListener cdResult = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (commandCode == CD_CODE) {
                    interactive.addCommand("pwd", PWD_CODE, pwdResult);
                }
            }
        };

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

            new StoppableThread() {
                @Override
                public void run() {
                    if (input.trim().equalsIgnoreCase("su")) {
                        if (Shell.SU.available())
                            LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_ROOT));
                        interactive.addCommand("su");

                    } else if (input.contains("cd ")) {
                        interactive.addCommand(input, CD_CODE, cdResult);
                    } else {
                        interactive.addCommand(input);
                    }

                }
            }.start();

            return true;
        }
    }

    private class AppTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) {
            AppsManager.LaunchInfo i = appsManager.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
            return i != null && performLaunch(i);
        }
    }

    private class TuiCommandTrigger implements CmdTrigger {

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

            final Command command = CommandTuils.parse(input, info, false);
            if (command == null) return false;

            mainPack.lastCommand = input;

            new StoppableThread() {
                @Override
                public void run() {
                    super.run();

                    try {
                        String output = command.exec(mContext.getResources(), info);

                        if (output != null) {
                            Tuils.sendOutput(mContext, output);
                        }
                    } catch (Exception e) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                        Tuils.log(e);
                    }
                }
            }.start();

            return true;
        }
    }
}
