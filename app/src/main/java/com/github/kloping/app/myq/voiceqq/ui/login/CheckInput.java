package com.github.kloping.app.myq.voiceqq.ui.login;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import com.github.kloping.app.myq.voiceqq.R;

import java.util.Objects;

/**
 * @author github.kloping
 */
public class CheckInput implements View.OnFocusChangeListener, TextWatcher {
    private EditText editText;
    private Type type = Type.QQ_NUMBER;

    public CheckInput setStringId(int id) {
        this.stringId = id;
        return this;
    }

    public CheckInput setMinLength(int i) {
        this.minLength = i;
        return this;
    }

    public static enum Type {
        QQ_NUMBER, QQ_PASSWORD
    }

    public CheckInput(EditText editText) {
        this.editText = editText;
    }

    public EditText getEditText() {
        return editText;
    }

    public void setEditText(EditText editText) {
        this.editText = editText;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public CheckInput apply() {
        editText.setOnFocusChangeListener(this);
        editText.addTextChangedListener(this);
        return this;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        onFocusChange(editText, false);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            if (isErr()) {
                editText.setError(getErr());
                err(getErr());
            } else {
                ok(editText.getText().toString().trim());
            }
        }
    }

    private String err;

    public String getErr() {
        return err;
    }

    private int minLength = 6;
    public int stringId = R.string.qq_length_most_short;

    @SuppressLint("StringFormatMatches")
    public boolean isErr() {
        String s0 = editText.getText().toString().trim();
        if (type == Type.QQ_NUMBER) {
            if (s0.length() <= minLength) {
                err = String.format(editText.getContext().getString(stringId), minLength);
                return true;
            }
        }
        if (type == Type.QQ_PASSWORD) {
            if (s0.length() <= minLength) {
                err = String.format(editText.getContext().getString(stringId), minLength);
                return true;
            }
        }
        return false;
    }

    public void err(String err) {
    }

    public void ok(String s) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckInput that = (CheckInput) o;
        return minLength == that.minLength && stringId == that.stringId && Objects.equals(editText, that.editText) && type == that.type && Objects.equals(err, that.err);
    }

    @Override
    public int hashCode() {
        return Objects.hash(editText, type, err, minLength, stringId);
    }
}
