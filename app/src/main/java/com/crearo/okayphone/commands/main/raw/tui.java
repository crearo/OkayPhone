package com.crearo.okayphone.commands.main.raw;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import com.crearo.okayphone.BuildConfig;
import com.crearo.okayphone.R;
import com.crearo.okayphone.UIManager;
import com.crearo.okayphone.commands.CommandAbstraction;
import com.crearo.okayphone.commands.CommandsPreferences;
import com.crearo.okayphone.commands.ExecutePack;
import com.crearo.okayphone.commands.main.MainPack;
import com.crearo.okayphone.commands.specific.ParamCommand;
import com.crearo.okayphone.managers.xml.XMLPrefsManager;
import com.crearo.okayphone.tuils.Tuils;
import com.crearo.okayphone.tuils.stuff.PolicyReceiver;

import java.io.File;

/**
 * Created by francescoandreuzzi on 10/06/2017.
 */

public class tui extends ParamCommand {

    private enum Param implements com.crearo.okayphone.commands.main.Param {

        rm {
            @Override
            public String exec(ExecutePack pack) {
                MainPack info = (MainPack) pack;

                DevicePolicyManager policy = (DevicePolicyManager) info.context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName name = new ComponentName(info.context, PolicyReceiver.class);
                policy.removeActiveAdmin(name);

                Uri packageURI = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                info.context.startActivity(uninstallIntent);

                return null;
            }
        },
        about {
            @Override
            public String exec(ExecutePack pack) {
                MainPack info = (MainPack) pack;
                return "Version:" + Tuils.SPACE + BuildConfig.VERSION_NAME + " (code: " + BuildConfig.VERSION_CODE + ")" +
                        (BuildConfig.DEBUG ? Tuils.NEWLINE + BuildConfig.BUILD_TYPE : Tuils.EMPTYSTRING) +
                        Tuils.NEWLINE + Tuils.NEWLINE + info.res.getString(R.string.output_about);
            }
        },
        log {
            @Override
            public int[] args() {
                return new int[]{CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(UIManager.ACTION_LOGTOFILE);
                i.putExtra(UIManager.FILE_NAME, pack.getString());
                LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(i);

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.context.getString(R.string.help_tui);
            }
        },
        priority {
            @Override
            public int[] args() {
                return new int[]{CommandAbstraction.COMMAND, CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(Tuils.getFolder(), "cmd.xml");
                return XMLPrefsManager.set(file, pack.get().getClass().getSimpleName() + CommandsPreferences.PRIORITY_SUFFIX, new String[]{XMLPrefsManager.VALUE_ATTRIBUTE}, new String[]{String.valueOf(pack.getInt())});
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.context.getString(R.string.help_tui);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.output_invalidarg);
            }
        },
        telegram {
            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.webPage("https://t.me/tuilauncher"));
                return null;
            }
        },
        googlep {
            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.webPage("https://plus.google.com/communities/103936578623101446195"));
                return null;
            }
        },
        twitter {
            @Override
            public String exec(ExecutePack pack) {
                pack.context.startActivity(Tuils.webPage("https://twitter.com/tui_launcher"));
                return null;
            }
        },
        reset {
            @Override
            public String exec(ExecutePack pack) {
                Tuils.delete(Tuils.getFolder());
                return null;
            }
        },
        folder {
            @Override
            public String exec(ExecutePack pack) {

                Uri selectedUri = Uri.parse(Tuils.getFolder().getAbsolutePath());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");

                if (intent.resolveActivityInfo(pack.context.getPackageManager(), 0) != null) {
                    pack.context.startActivity(intent);
                } else {
                    return Tuils.getFolder().getAbsolutePath();
                }

                return null;
            }
        }
//        ,
//        exclude_message {
//            @Override
//            public int[] args() {
//                return new int[] {CommandAbstraction.INT};
//            }
//
//            @Override
//            public String exec(ExecutePack pack) {
//                return null;
//            }
//
//            @Override
//            public String onNotArgEnough(ExecutePack pack, int n) {
//
//            }
//        }
        ;

        @Override
        public int[] args() {
            return new int[0];
        }

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return null;
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return null;
        }
    }

    @Override
    protected com.crearo.okayphone.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_tui;
    }
}