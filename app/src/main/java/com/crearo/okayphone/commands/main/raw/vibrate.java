package com.crearo.okayphone.commands.main.raw;

import android.content.Context;
import android.os.Vibrator;

import com.crearo.okayphone.R;
import com.crearo.okayphone.commands.CommandAbstraction;
import com.crearo.okayphone.commands.ExecutePack;
import com.crearo.okayphone.commands.main.MainPack;
import com.crearo.okayphone.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

public class vibrate implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        String text = pack.getString();
        Context context = ((MainPack) pack).context;

        char separator = Tuils.firstNonDigit(text);

        if(separator == 0) {
            int ms;
            try {
                ms = Integer.parseInt(text);
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
                return null;
            } catch (NumberFormatException e) {
                return context.getString(R.string.invalid_integer);
            } catch (Exception e) {
                return e.toString();
            }
        } else {
            if(separator == ' ') {
                char s2 = Tuils.firstNonDigit(Tuils.removeSpaces(text));
                if(s2 != 0) {
                    text = Tuils.removeSpaces(text);
                    separator = s2;
                }
            }

            String[] split = text.split(separator + Tuils.EMPTYSTRING);
            long pattern[] = new long[split.length];

            for(int c = 0; c < split.length; c++) {
                try {
                    pattern[c] = Long.parseLong(split[c]);
                } catch (Exception e) {
                    pattern[c] = 0;
                }
            }

            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(pattern, -1);
        }

        return null;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return R.string.help_vibrate;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return ((MainPack) pack).context.getString(helpRes());
    }
}
