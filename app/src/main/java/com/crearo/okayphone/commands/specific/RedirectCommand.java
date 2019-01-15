package com.crearo.okayphone.commands.specific;

import java.util.ArrayList;
import java.util.List;

import com.crearo.okayphone.commands.CommandAbstraction;
import com.crearo.okayphone.commands.ExecutePack;

/**
 * Created by francescoandreuzzi on 03/03/2017.
 */

public abstract class RedirectCommand implements CommandAbstraction {

    public List<Object> beforeObjects = new ArrayList<>();
    public List<Object> afterObjects = new ArrayList<>();

    public abstract String onRedirect(ExecutePack pack);
    public abstract int getHint();
    public abstract boolean isWaitingPermission();

    public void cleanup() {
        beforeObjects.clear();
        afterObjects.clear();
    }
}
