package com.crearo.okayphone.tuils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.TextView;

import com.crearo.okayphone.BuildConfig;
import com.crearo.okayphone.R;
import com.crearo.okayphone.managers.TerminalManager;
import com.crearo.okayphone.managers.music.MusicManager2;
import com.crearo.okayphone.managers.music.Song;
import com.crearo.okayphone.managers.notifications.NotificationService;
import com.crearo.okayphone.managers.xml.XMLPrefsManager;
import com.crearo.okayphone.managers.xml.classes.XMLPrefsSave;
import com.crearo.okayphone.managers.xml.options.Behavior;
import com.crearo.okayphone.managers.xml.options.Ui;
import com.crearo.okayphone.tuils.interfaces.OnBatteryUpdate;
import com.crearo.okayphone.tuils.stuff.FakeLauncherActivity;

import org.xml.sax.SAXParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dalvik.system.DexFile;

public class Tuils {

    public static final String SPACE = " ";
    public static final String DOUBLE_SPACE = "  ";
    public static final String NEWLINE = "\n";
    public static final String TRIBLE_SPACE = "   ";
    public static final String DOT = ".";
    public static final String EMPTYSTRING = "";
    private static final String TUI_FOLDER = "t-ui";
    public static final String MINUS = "-";

    public static Pattern patternNewline = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    private static Typeface globalTypeface = null;
    public static String fontPath = null;

    public static Typeface getTypeface(Context context) {
        if (globalTypeface == null) {
            try {
                XMLPrefsManager.loadCommons(context);
            } catch (Exception e) {
                return null;
            }

            boolean systemFont = XMLPrefsManager.getBoolean(Ui.system_font);
            if (systemFont) globalTypeface = Typeface.DEFAULT;
            else {
                File tui = Tuils.getFolder();
                if (tui == null) {
                    return Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
                }

                Pattern p = Pattern.compile(".[ot]tf$");

                File font = null;
                for (File f : tui.listFiles()) {
                    String name = f.getName();
                    if (p.matcher(name).find()) {
                        font = f;
                        fontPath = f.getAbsolutePath();
                        break;
                    }
                }

                if (font != null) {
                    try {
                        globalTypeface = Typeface.createFromFile(font);
                        if (globalTypeface == null) throw new UnsupportedOperationException();
                    } catch (Exception e) {
                        globalTypeface = null;
                    }
                }
            }

            if (globalTypeface == null)
                globalTypeface = systemFont ? Typeface.DEFAULT : Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");
        }
        return globalTypeface;
    }

    public static void cancelFont() {
        globalTypeface = null;
        fontPath = null;
    }

    public static boolean notificationServiceIsRunning(Context context) {
        ComponentName collectorComponent = new ComponentName(context, NotificationService.class);
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean collectorRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(collectorComponent)) {
                if (service.pid == Process.myPid()) {
                    collectorRunning = true;
                }
            }
        }

        return collectorRunning;
    }

    public static boolean arrayContains(int[] array, int value) {
        if (array == null) return false;

        for (int i : array) {
            if (i == value) {
                return true;
            }
        }
        return false;
    }

    static final char CHAR_SPACE = ' ';

    public static boolean equalsIgnoreSpaceAndCase(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }

        int i1 = 0, i2 = 0;
        while (true) {
            if (i1 == s1.length() || i2 == s2.length()) break;

            char c1 = Character.toLowerCase(s1.charAt(i1));
            char c2 = Character.toLowerCase(s2.charAt(i2));

            if (c1 != CHAR_SPACE) {
                if (c2 != CHAR_SPACE) {
                    if (c1 != c2) return false;
                } else {
//                    c1 is not space, c2 is space
//                    i1 remains, i2 changes
                    i2++;
                    continue;
                }
            }

//            c1 is space
            if (c2 == CHAR_SPACE) {
//                c1 is space, c2 is space
                i1++;
                i2++;
            } else {
//                c1 is space, c2 is not space
                i1++;
                continue;
            }
        }

        return true;
    }

    public static String readerToString(Reader initialReader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = initialReader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        initialReader.close();
        return buffer.toString();
    }

    private static OnBatteryUpdate batteryUpdate;
    private static BroadcastReceiver batteryReceiver = null;

    public static void registerBatteryReceiver(Context context, OnBatteryUpdate listener) {
        try {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (batteryUpdate == null) return;

                    switch (intent.getAction()) {
                        case Intent.ACTION_BATTERY_CHANGED:
                            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                            batteryUpdate.update(level);
                            break;
                        case Intent.ACTION_POWER_CONNECTED:
                            batteryUpdate.onCharging();
                            break;
                        case Intent.ACTION_POWER_DISCONNECTED:
                            batteryUpdate.onNotCharging();
                            break;
                    }
                }
            };

            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            iFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            iFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

            context.registerReceiver(batteryReceiver, iFilter);

            batteryUpdate = listener;
        } catch (Exception e) {
            Tuils.toFile(e);
        }
    }

    public static void unregisterBatteryReceiver(Context context) {
        if (batteryReceiver != null) context.unregisterReceiver(batteryReceiver);
    }

    public static boolean containsExtension(String[] array, String value) {
        try {
            value = value.toLowerCase().trim();
            for (String s : array) {
                if (value.endsWith(s)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<Song> getSongsInFolder(File folder) {
        List<Song> songs = new ArrayList<>();

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return songs;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                List<Song> s = getSongsInFolder(file);
                if (s != null) {
                    songs.addAll(s);
                }
            } else if (containsExtension(MusicManager2.MUSIC_EXTENSIONS, file.getName())) {
                songs.add(new Song(file));
            }
        }

        return songs;
    }

    public static String convertStreamToString(java.io.InputStream is) {
        if (is == null) return Tuils.EMPTYSTRING;

        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : Tuils.EMPTYSTRING;
    }

    public static long download(InputStream in, File file) throws Exception {
        OutputStream out = new FileOutputStream(file, false);

        byte data[] = new byte[1024];

        long bytes = 0;

        int count;
        while ((count = in.read(data)) != -1) {
            out.write(data, 0, count);
            bytes += count;
        }

        out.flush();
        out.close();
        in.close();

        return bytes;
    }

    public static void write(File file, String separator, String... ss) throws Exception {
        FileOutputStream headerStream = new FileOutputStream(file, false);

        for (int c = 0; c < ss.length - 1; c++) {
            headerStream.write(ss[c].getBytes());
            headerStream.write(separator.getBytes());
        }
        headerStream.write(ss[ss.length - 1].getBytes());

        headerStream.flush();
        headerStream.close();
    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public static boolean hasNotificationAccess(Context context) {
        String pkgName = BuildConfig.APPLICATION_ID;
        final String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void resetPreferredLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, FakeLauncherActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void openSettingsPage(Context c, String packageName) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        c.startActivity(intent);
    }

    public static Intent requestAdmin(ComponentName component, String explanation) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
        return intent;
    }

    public static Intent webPage(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    public static double getAvailableInternalMemorySize(int unit) {
        return getAvailableSpace(Environment.getDataDirectory(), unit);
    }

    public static double getTotalInternalMemorySize(int unit) {
        return getTotaleSpace(Environment.getDataDirectory(), unit);
    }

    public static double getAvailableExternalMemorySize(int unit) {
        try {
            return getAvailableSpace(XMLPrefsManager.get(File.class, Behavior.external_storage_path), unit);
        } catch (Exception e) {
            return -1;
        }
    }

    public static double getTotalExternalMemorySize(int unit) {
        try {
            return getTotaleSpace(XMLPrefsManager.get(File.class, Behavior.external_storage_path), unit);
        } catch (Exception e) {
            return -1;
        }
    }

    public static double getAvailableSpace(File dir, int unit) {
        if (dir == null) return -1;

        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long blocks = statFs.getAvailableBlocks();
        return formatSize(blocks * statFs.getBlockSize(), unit);
    }

    public static double getTotaleSpace(File dir, int unit) {
        if (dir == null) return -1;

        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long blocks = statFs.getBlockCount();
        return formatSize(blocks * statFs.getBlockSize(), unit);
    }

    public static double percentage(double part, double total) {
        return round(part * 100 / total, 2);
    }

    public static double formatSize(long bytes, int unit) {
        double convert = 1048576.0;
        double smallConvert = 1024.0;

        double result;

        switch (unit) {
            case TERA:
                result = (bytes / convert) / convert;
                break;
            case GIGA:
                result = (bytes / convert) / smallConvert;
                break;
            case MEGA:
                result = bytes / convert;
                break;
            case KILO:
                result = bytes / smallConvert;
                break;
            case BYTE:
                result = bytes;
                break;
            default:
                return -1;
        }

        return round(result, 2);
    }

    public static SpannableString span(CharSequence text, int color) {
        return span(null, text, color, Integer.MAX_VALUE);
    }

    public static SpannableString span(Context context, int size, CharSequence text) {
        return span(context, text, Integer.MAX_VALUE, size);
    }

    public static SpannableString span(Context context, CharSequence text, int color, int size) {
        SpannableString spannableString = new SpannableString(text);
        if (size != Integer.MAX_VALUE && context != null)
            spannableString.setSpan(new AbsoluteSizeSpan(convertSpToPixels(size, context)), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (color != Integer.MAX_VALUE) {
            spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    public static SpannableString span(int bgColor, int foreColor, CharSequence text) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new BackgroundColorSpan(bgColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(foreColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static int span(int bgColor, SpannableString text, String section, int fromIndex) {
        int index = text.toString().indexOf(section, fromIndex);
        if (index == -1) return index;

        text.setSpan(new BackgroundColorSpan(bgColor), index, index + section.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return index + section.length();
    }

    public static int convertSpToPixels(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static void delete(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) delete(f);
            f.delete();
        }
        dir.delete();
    }

    public static void deleteContent(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) delete(f);
            f.delete();
        }
    }

    public static boolean insertOld(File oldFile) {
        if (oldFile == null || !oldFile.exists()) return false;

        String oldPath = oldFile.getAbsolutePath();

        File oldFolder = new File(Tuils.getFolder(), "old");
        if (!oldFolder.exists()) oldFolder.mkdir();

        File dest = new File(oldFolder, oldFile.getName());
        if (dest.exists()) dest.delete();

        return oldFile.renameTo(dest) && new File(oldPath).delete();
    }

    public static File getOld(String name) {
        File old = new File(Tuils.getFolder(), "old");
        File file = new File(old, name);

        if (file.exists()) return file;
        return null;
    }

    public static void deepView(View v) {
        Tuils.log(v.toString());

        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;

        Tuils.log(g.getChildCount());
        for (int c = 0; c < g.getChildCount(); c++) deepView(g.getChildAt(c));

        Tuils.log("end of parents of: " + v.toString());
    }

    public static void sendOutput(Context context, int res) {
        sendOutput(Integer.MAX_VALUE, context, res);
    }

    public static void sendOutput(int color, Context context, int res) {
        sendOutput(color, context, context.getString(res));
    }

    public static void sendOutput(Context context, int res, int type) {
        sendOutput(Integer.MAX_VALUE, context, res, type);
    }

    public static void sendOutput(int color, Context context, int res, int type) {
        sendOutput(color, context, context.getString(res), type);
    }

    public static void sendOutput(Context context, CharSequence s) {
        sendOutput(Integer.MAX_VALUE, context, s);
    }

    public static void sendOutput(int color, Context context, CharSequence s) {
        sendOutput(color, context, s, TerminalManager.CATEGORY_OUTPUT);
    }

    public static void sendOutput(Context context, CharSequence s, int type) {
        sendOutput(Integer.MAX_VALUE, context, s, type);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOutput(Context context, CharSequence s, int type, Object action) {
        sendOutput(Integer.MAX_VALUE, context, s, type, action);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type, Object action) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);

        if (action instanceof String) intent.putExtra(PrivateIOReceiver.ACTION, (String) action);
        else if (action instanceof Parcelable)
            intent.putExtra(PrivateIOReceiver.ACTION, (Parcelable) action);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOutput(Context context, CharSequence s, int type, Object action, Object longAction) {
        sendOutput(Integer.MAX_VALUE, context, s, type, action, longAction);
    }

    public static void sendOutput(int color, Context context, CharSequence s, int type, Object action, Object longAction) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, s);
        intent.putExtra(PrivateIOReceiver.COLOR, color);
        intent.putExtra(PrivateIOReceiver.TYPE, type);

        if (action instanceof String) intent.putExtra(PrivateIOReceiver.ACTION, (String) action);
        else if (action instanceof Parcelable)
            intent.putExtra(PrivateIOReceiver.ACTION, (Parcelable) action);

        if (longAction instanceof String)
            intent.putExtra(PrivateIOReceiver.LONG_ACTION, (String) longAction);
        else if (longAction instanceof Parcelable)
            intent.putExtra(PrivateIOReceiver.LONG_ACTION, (Parcelable) longAction);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendInput(Context context, String text) {
        Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
        intent.putExtra(PrivateIOReceiver.TEXT, text);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static final int TERA = 0;
    public static final int GIGA = 1;
    public static final int MEGA = 2;
    public static final int KILO = 3;
    public static final int BYTE = 4;

    private static long total = -1;

    public static double freeRam(ActivityManager mgr, MemoryInfo info) {
        mgr.getMemoryInfo(info);
        return info.availMem;
    }

    public static long totalRam() {
        if (total > 0) return total;

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal")) {
                    line = line.replaceAll("\\D+", Tuils.EMPTYSTRING);
                    return Long.parseLong(line);
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static double round(double value, int places) {
        if (places < 0) places = 0;

        try {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } catch (Exception e) {
            return value;
        }
    }

    public static List<String> getClassesInPackage(String packageName, Context c) throws IOException {
        List<String> classes = new ArrayList<>();
        String packageCodePath = c.getPackageCodePath();
        DexFile df = new DexFile(packageCodePath);
        for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
            String className = iter.nextElement();
            if (className.contains(packageName) && !className.contains("$")) {
                classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
            }
        }

        return classes;
    }

    public static String[] toString(Enum[] enums) {
        String[] arr = new String[enums.length];
        for (int count = 0; count < enums.length; count++) arr[count] = enums[count].name();
        return arr;
    }

    private static String getNicePath(String filePath) {
        if (filePath == null) return "null";

        String home = XMLPrefsManager.get(File.class, Behavior.home_path).getAbsolutePath();

        if (filePath.equals(home)) {
            return "~";
        } else if (filePath.startsWith(home)) {
            return "~" + filePath.replace(home, Tuils.EMPTYSTRING);
        } else {
            return filePath;
        }
    }

    public static int find(Object o, Object[] array) {
        return find(o, Arrays.asList(array));
    }

    public static int find(Object o, List list) {
        for (int count = 0; count < list.size(); count++) {
            Object x = list.get(count);
            if (x == null) continue;

            if (o == x) return count;

            if (o instanceof XMLPrefsSave) {
                try {
                    if (((XMLPrefsSave) o).is((String) x)) return count;
                } catch (Exception e) {
                }
            }

            if (o instanceof String && x instanceof XMLPrefsSave) {
                try {
                    if (((XMLPrefsSave) x).is((String) o)) return count;
                } catch (Exception e) {
                }
            }

            try {
                if (o.equals(x) || x.equals(o)) return count;
            } catch (Exception e) {
                continue;
            }
        }
        return -1;
    }

    static Pattern pd = Pattern.compile("%d", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    static Pattern pu = Pattern.compile("%u", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    static Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    public static String getHint(String currentPath) {
        if (!XMLPrefsManager.getBoolean(Ui.show_session_info)) return null;

        String format = XMLPrefsManager.get(Behavior.session_info_format);
        if (format.length() == 0) return null;

        String deviceName = XMLPrefsManager.get(Ui.deviceName);
        if (deviceName == null || deviceName.length() == 0) {
            deviceName = Build.DEVICE;
        }

        String username = XMLPrefsManager.get(Ui.username);
        if (username == null) username = Tuils.EMPTYSTRING;

        format = pd.matcher(format).replaceAll(Matcher.quoteReplacement(deviceName));
        format = pu.matcher(format).replaceAll(Matcher.quoteReplacement(username));
        format = pp.matcher(format).replaceAll(Matcher.quoteReplacement(Tuils.getNicePath(currentPath)));

        return format;
    }

    public static int findPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++)
            if (list.get(count).startsWith(prefix))
                return count;
        return -1;
    }

    public static int mmToPx(DisplayMetrics metrics, int mm) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, metrics);
    }

    public static void insertHeaders(List<String> s, boolean newLine) {
        char current = 0;
        for (int count = 0; count < s.size(); count++) {
            String st = s.get(count).trim().toUpperCase();
            if (st.length() < 0) continue;

            char c = st.charAt(0);
            if (current != c) {
                s.add(count, (newLine ? NEWLINE : EMPTYSTRING) + c + (newLine ? NEWLINE : EMPTYSTRING));
                current = c;
            }
        }
    }

    public static void addPrefix(List<String> list, String prefix) {
        for (int count = 0; count < list.size(); count++) {
            list.set(count, prefix.concat(list.get(count)));
        }
    }

    public static void addSeparator(List<String> list, String separator) {
        for (int count = 0; count < list.size(); count++)
            list.set(count, list.get(count).concat(separator));
    }

    public static String toPlanString(String[] strings, String separator) {
        if (strings == null) {
            return Tuils.EMPTYSTRING;
        }

        String output = Tuils.EMPTYSTRING;
        for (int count = 0; count < strings.length; count++) {
            output = output.concat(strings[count]);
            if (count < strings.length - 1) output = output.concat(separator);
        }
        return output;
    }

    public static String toPlanString(String[] strings) {
        if (strings != null) {
            return Tuils.toPlanString(strings, Tuils.NEWLINE);
        }
        return Tuils.EMPTYSTRING;
    }

    public static String toPlanString(String separator, List<? extends Compare.Stringable> strings) {
        if (strings == null) {
            return Tuils.EMPTYSTRING;
        }

        String output = Tuils.EMPTYSTRING;
        for (int count = 0; count < strings.size(); count++) {
            output = output.concat(strings.get(count).getString());
            if (count < strings.size() - 1) output = output.concat(separator);
        }
        return output;
    }

    public static void log(Object o) {
//        Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));

        if (o instanceof Throwable) {
            Log.e("andre", "", (Throwable) o);
        } else {
            String text;
            if (o instanceof Object[]) text = Arrays.toString((Object[]) o);
            else text = o.toString();
            Log.e("andre", text);
        }
    }

    public static void log(Object o, Object o2) {
        if (o instanceof Object[] && o2 instanceof Object[]) {
            Log.e("andre", Arrays.toString((Object[]) o) + " -- " + Arrays.toString((Object[]) o2));
        } else {
            Log.e("andre", String.valueOf(o) + " -- " + String.valueOf(o2));
        }
    }

    public static void log(Object o, PrintStream to) {
//        Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));

        if (o instanceof Throwable) {
            ((Throwable) o).printStackTrace(to);
        } else {
            String text;
            if (o instanceof Object[]) text = Arrays.toString((Object[]) o);
            else text = o.toString();

            try {
                to.write(text.getBytes());
            } catch (IOException e) {
                Tuils.log(e);
            }
        }
    }

    public static void log(Object o, Object o2, OutputStream to) {
        try {
            if (o instanceof Object[] && o2 instanceof Object[]) {
                to.write((Arrays.toString((Object[]) o) + " -- " + Arrays.toString((Object[]) o2)).getBytes());
            } else {
                to.write((String.valueOf(o) + " -- " + String.valueOf(o2)).getBytes());
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public static boolean hasInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://clients3.google.com/generate_204").openConnection());
            return (urlc.getResponseCode() == 204 && urlc.getContentLength() == 0);
        } catch (IOException e) {
            return false;
        }
    }

    public static <T> T getDefaultValue(Class<T> clazz) {
        return (T) Array.get(Array.newInstance(clazz, 1), 0);
    }

    public static void toFile(Throwable e) {
        try {
            RandomAccessFile f = new RandomAccessFile(new File(Tuils.getFolder(), "crash.txt"), "rw");
            f.seek(0);
            f.write((new Date().toString() + Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
            OutputStream is = Channels.newOutputStream(f.getChannel());
            e.printStackTrace(new PrintStream(is));
            f.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());

            is.close();
            f.close();
        } catch (Exception e1) {
        }
    }

    public static String toPlanString(List<String> strings, String separator) {
        if (strings != null) {
            String[] object = new String[strings.size()];
            return Tuils.toPlanString(strings.toArray(object), separator);
        }
        return Tuils.EMPTYSTRING;
    }

    public static String filesToPlanString(List<File> files, String separator) {
        if (files == null || files.size() == 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        int limit = files.size() - 1;
        for (int count = 0; count < files.size(); count++) {
            builder.append(files.get(count).getName());
            if (count < limit) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    public static String toPlanString(List<String> strings) {
        return Tuils.toPlanString(strings, NEWLINE);
    }

    public static String toPlanString(Object[] objs, String separator) {
        if (objs == null) {
            return Tuils.EMPTYSTRING;
        }

        StringBuilder output = new StringBuilder();
        for (int count = 0; count < objs.length; count++) {
            output.append(objs[count]);
            if (count < objs.length - 1) {
                output.append(separator);
            }
        }
        return output.toString();
    }

    public static String removeUnncesarySpaces(String string) {
        while (string.contains(DOUBLE_SPACE)) {
            string = string.replace(DOUBLE_SPACE, SPACE);
        }
        return string;
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static boolean isAlpha(String s) {
        if (s == null) {
            return false;
        }
        char[] chars = s.toCharArray();

        for (char c : chars)
            if (!Character.isLetter(c))
                return false;

        return true;
    }

    public static boolean isPhoneNumber(String s) {
        if (s == null) {
            return false;
        }
        char[] chars = s.toCharArray();

        for (char c : chars) {
            if (Character.isLetter(c)) {
                return false;
            }
        }

        return true;
    }

    //    return 0 if only digit
    public static char firstNonDigit(String s) {
        if (s == null) {
            return 0;
        }

        char[] chars = s.toCharArray();

        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return c;
            }
        }

        return 0;
    }

    public static Intent openFile(File url) {
        Uri u = Uri.fromFile(url);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

        String extension = MimeTypeMap.getFileExtensionFromUrl(u.toString());
        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        intent.setDataAndType(u, mimetype);
        return intent;
    }

    public static Intent shareFile(File url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri u = Uri.fromFile(url);

        String extension = MimeTypeMap.getFileExtensionFromUrl(u.toString());
        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        intent.setDataAndType(u, mimetype);
        intent.putExtra(Intent.EXTRA_STREAM, u);

        return intent;
    }

    private static File getTuiFolder() {
        File internalDir = Environment.getExternalStorageDirectory();
        return new File(internalDir, TUI_FOLDER);
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    public static String getTextFromClipboard(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = manager.getPrimaryClip().getItemAt(0);
                return item.getText().toString();
            } else {
                android.text.ClipboardManager manager = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                return manager.getText().toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static int dpToPx(Resources resources, int dp) {
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static final int FILEUPDATE_DELAY = 50;
    private static File folder = null;

    public static File getFolder() {
        if (folder != null) return folder;

        int elapsedTime = 0;
        while (elapsedTime < 3000) {
            File tuiFolder = Tuils.getTuiFolder();
            if (tuiFolder != null && ((tuiFolder.exists() && tuiFolder.isDirectory()) || tuiFolder.mkdir())) {
                folder = tuiFolder;
                return folder;
            }

            try {
                Thread.sleep(FILEUPDATE_DELAY);
            } catch (InterruptedException e) {
            }

            elapsedTime += FILEUPDATE_DELAY;
        }

        return null;
    }

    public static int alphabeticCompare(String s1, String s2) {
        String cmd1 = removeSpaces(s1).toLowerCase();
        String cmd2 = removeSpaces(s2).toLowerCase();

        for (int count = 0; count < cmd1.length() && count < cmd2.length(); count++) {
            char c1 = cmd1.charAt(count);
            char c2 = cmd2.charAt(count);

            if (c1 < c2) {
                return -1;
            } else if (c1 > c2) {
                return 1;
            }
        }

        if (s1.length() > s2.length()) {
            return 1;
        } else if (s1.length() < s2.length()) {
            return -1;
        }
        return 0;
    }

    private static final String SPACE_REGEXP = "\\s";

    public static String removeSpaces(String string) {
        return string.replaceAll(SPACE_REGEXP, EMPTYSTRING);
    }

    public static String getNetworkType(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2g";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3g";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4g";
            default:
                return "unknown";
        }
    }

    public static void setCursorDrawableColor(EditText editText, int color) {
        try {
            Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            fCursorDrawableRes.setAccessible(true);
            int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
            Field fEditor = TextView.class.getDeclaredField("mEditor");
            fEditor.setAccessible(true);
            Object editor = fEditor.get(editText);
            Class<?> clazz = editor.getClass();
            Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
            fCursorDrawable.setAccessible(true);
            Drawable[] drawables = new Drawable[2];
            drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
            drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            fCursorDrawable.set(editor, drawables);
        } catch (Throwable ignored) {
        }
    }

    public static int nOfBytes(File file) {
        int count = 0;
        try {
            FileInputStream in = new FileInputStream(file);

            while (in.read() != -1) count++;

            return count;
        } catch (IOException e) {
            Tuils.log(e);
            return count;
        }
    }

    public static void sendXMLParseError(Context context, String PATH, SAXParseException e) {
        Tuils.sendOutput(
                Color.RED,
                context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2) + Tuils.NEWLINE + context.getString(R.string.output_errorlabel) +
                        "File: " + e.getSystemId() + Tuils.NEWLINE +
                        "Message" + e.getMessage() + Tuils.NEWLINE +
                        "Line" + e.getLineNumber() + Tuils.NEWLINE +
                        "Column" + e.getColumnNumber());
    }

    public static void sendXMLParseError(Context context, String PATH) {
        Tuils.sendOutput(Color.RED, context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2));
    }
}
