package com.muzima.messaging.contacts;

import android.content.Context;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.telephony.PhoneNumberUtils;
import android.text.Annotation;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;

import com.muzima.messaging.utils.RecipientsFormatter;
import com.muzima.model.SignalRecipient;

import java.util.ArrayList;
import java.util.List;

public class RecipientsEditor extends AppCompatMultiAutoCompleteTextView {
    private int mLongPressedPosition = -1;
    private final RecipientsEditorTokenizer mTokenizer;
    private char mLastSeparator = ',';
    private Context mContext;

    public RecipientsEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mTokenizer = new RecipientsEditorTokenizer(context, this);
        setTokenizer(mTokenizer);
        // For the focus to move to the message body when soft Next is pressed
        setImeOptions(EditorInfo.IME_ACTION_NEXT);

        /*
         * The point of this TextWatcher is that when the user chooses
         * an address completion from the AutoCompleteTextView menu, it
         * is marked up with Annotation objects to tie it back to the
         * address book entry that it came from.  If the user then goes
         * back and edits that part of the text, it no longer corresponds
         * to that address book entry and needs to have the Annotations
         * claiming that it does removed.
         */
        addTextChangedListener(new TextWatcher() {
            private Annotation[] mAffected;

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                mAffected = ((Spanned) s).getSpans(start, start + count,
                        Annotation.class);
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int after) {
                if (before == 0 && after == 1) {    // inserting a character
                    char c = s.charAt(start);
                    if (c == ',' || c == ';') {
                        // Remember the delimiter the user typed to end this recipient. We'll
                        // need it shortly in terminateToken().
                        mLastSeparator = c;
                    }
                }
            }

            public void afterTextChanged(Editable s) {
                if (mAffected != null) {
                    for (Annotation a : mAffected) {
                        s.removeSpan(a);
                    }
                }

                mAffected = null;
            }
        });
    }

    @Override
    public boolean enoughToFilter() {
        if (!super.enoughToFilter()) {
            return false;
        }
        // If the user is in the middle of editing an existing recipient, don't offer the
        // auto-complete menu. Without this, when the user selects an auto-complete menu item,
        // it will get added to the list of recipients so we end up with the old before-editing
        // recipient and the new post-editing recipient. As a precedent, gmail does not show
        // the auto-complete menu when editing an existing recipient.
        int end = getSelectionEnd();
        int len = getText().length();

        return end == len;
    }

    public int getRecipientCount() {
        return mTokenizer.getNumbers().size();
    }

    public List<String> getNumbers() {
        return mTokenizer.getNumbers();
    }

    private boolean isValidAddress(String number, boolean isMms) {
        return PhoneNumberUtils.isWellFormedSmsAddress(number);
    }

    public boolean hasValidRecipient(boolean isMms) {
        for (String number : mTokenizer.getNumbers()) {
            if (isValidAddress(number, isMms))
                return true;
        }
        return false;
    }

    public String formatInvalidNumbers(boolean isMms) {
        StringBuilder sb = new StringBuilder();
        for (String number : mTokenizer.getNumbers()) {
            if (!isValidAddress(number, isMms)) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(number);
            }
        }
        return sb.toString();
    }

    public static CharSequence contactToToken(SignalRecipient c) {
        String name       = c.getName();
        String number     = c.getAddress().serialize();
        SpannableString s = new SpannableString(RecipientsFormatter.formatNameAndNumber(name, number));
        int len           = s.length();

        if (len == 0) {
            return s;
        }

        s.setSpan(new Annotation("number", c.getAddress().serialize()), 0, len,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return s;
    }

    public void populate(List<SignalRecipient> list) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (SignalRecipient c : list) {
            if (sb.length() != 0) {
                sb.append(", ");
            }

            sb.append(contactToToken(c));
        }

        setText(sb);
    }

    private int pointToPosition(int x, int y) {
        x -= getCompoundPaddingLeft();
        y -= getExtendedPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        if (layout == null) {
            return -1;
        }

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        return off;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            mLongPressedPosition = pointToPosition(x, y);
        }

        return super.onTouchEvent(ev);
    }

    private static String getNumberAt(Spanned sp, int start, int end, Context context) {
        return getFieldAt("number", sp, start, end, context);
    }

    private static int getSpanLength(Spanned sp, int start, int end, Context context) {
        // TODO: there's a situation where the span can lose its annotations:
        //   - add an auto-complete contact
        //   - add another auto-complete contact
        //   - delete that second contact and keep deleting into the first
        //   - we lose the annotation and can no longer get the span.
        // Need to fix this case because it breaks auto-complete contacts with commas in the name.
        Annotation[] a = sp.getSpans(start, end, Annotation.class);
        if (a.length > 0) {
            return sp.getSpanEnd(a[0]);
        }
        return 0;
    }

    private static String getFieldAt(String field, Spanned sp, int start, int end,
                                     Context context) {
        Annotation[] a = sp.getSpans(start, end, Annotation.class);
        String fieldValue = getAnnotation(a, field);
        if (TextUtils.isEmpty(fieldValue)) {
            fieldValue = TextUtils.substring(sp, start, end);
        }
        return fieldValue;

    }

    private static String getAnnotation(Annotation[] a, String key) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].getKey().equals(key)) {
                return a[i].getValue();
            }
        }

        return "";
    }

    private class RecipientsEditorTokenizer
            implements MultiAutoCompleteTextView.Tokenizer {
        private final MultiAutoCompleteTextView mList;
        private final Context mContext;

        RecipientsEditorTokenizer(Context context, MultiAutoCompleteTextView list) {
            mList = list;
            mContext = context;
        }

        /**
         * Returns the start of the token that ends at offset
         * <code>cursor</code> within <code>text</code>.
         * It is a method from the MultiAutoCompleteTextView.Tokenizer interface.
         */
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            char c;

            while (i > 0 && (c = text.charAt(i - 1)) != ',' && c != ';') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }

            return i;
        }

        /**
         * Returns the end of the token (minus trailing punctuation)
         * that begins at offset <code>cursor</code> within <code>text</code>.
         * It is a method from the MultiAutoCompleteTextView.Tokenizer interface.
         */
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            char c;

            while (i < len) {
                if ((c = text.charAt(i)) == ',' || c == ';') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        /**
         * Returns <code>text</code>, modified, if necessary, to ensure that
         * it ends with a token terminator (for example a space or comma).
         * It is a method from the MultiAutoCompleteTextView.Tokenizer interface.
         */
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            char c;
            if (i > 0 && ((c = text.charAt(i - 1)) == ',' || c == ';')) {
                return text;
            } else {
                // Use the same delimiter the user just typed.
                // This lets them have a mixture of commas and semicolons in their list.
                String separator = mLastSeparator + " ";
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + separator);
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                            Object.class, sp, 0);
                    return sp;
                } else {
                    return text + separator;
                }
            }
        }
        public String getRawString() {
            return mList.getText().toString();
        }
        public List<String> getNumbers() {
            Spanned sp = mList.getText();
            int len = sp.length();
            List<String> list = new ArrayList<String>();

            int start = 0;
            int i = 0;
            while (i < len + 1) {
                char c;
                if ((i == len) || ((c = sp.charAt(i)) == ',') || (c == ';')) {
                    if (i > start) {
                        list.add(getNumberAt(sp, start, i, mContext));

                        // calculate the recipients total length. This is so if the name contains
                        // commas or semis, we'll skip over the whole name to the next
                        // recipient, rather than parsing this single name into multiple
                        // recipients.
                        int spanLen = getSpanLength(sp, start, i, mContext);
                        if (spanLen > i) {
                            i = spanLen;
                        }
                    }

                    i++;

                    while ((i < len) && (sp.charAt(i) == ' ')) {
                        i++;
                    }

                    start = i;
                } else {
                    i++;
                }
            }

            return list;
        }
    }

    static class RecipientContextMenuInfo implements ContextMenu.ContextMenuInfo {
        final SignalRecipient recipient;

        RecipientContextMenuInfo(SignalRecipient r) {
            recipient = r;
        }
    }
}
