package com.github.kloping.app.myq.voiceqq.ui.login;

import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author github.kloping
 */
public class CheckInputManager {
    private Button button;

    public CheckInputManager(Button button) {
        this.button = button;
        button.setEnabled(false);
    }

    private List<CheckInput> checkInputs = new LinkedList<>();
    public Map<EditText, Boolean> checkInputMap = new HashMap();

    private void check() {
        for (Boolean value : checkInputMap.values()) {
            if (!value) {
                button.setEnabled(false);
                return;
            }
        }
        button.setEnabled(true);
    }

    private synchronized CheckInput getCheckInput(EditText editText) {
        CheckInput checkInput = null;
        checkInput = new CheckInput(editText) {
            {
                checkInputMap.put(getEditText(), false);
            }

            @Override
            public void ok(String s) {
                checkInputMap.put(getEditText(), true);
                check();
            }

            @Override
            public void err(String err) {
                checkInputMap.put(getEditText(), false);
                check();
            }

        };
        return checkInput;
    }

    public CheckInputManager add(EditText editText) {
        checkInputs.add(getCheckInput(editText).apply());
        return this;
    }

    public CheckInputManager add(EditText editText, int stringId) {
        checkInputs.add(getCheckInput(editText).setStringId(stringId).apply());
        return this;
    }

    public CheckInputManager add(EditText editText, int stringId, int minLength) {
        checkInputs.add(getCheckInput(editText).setStringId(stringId).setMinLength(minLength).apply());
        return this;
    }
}
