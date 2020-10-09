package com.taobao.weex.devtools.inspector.elements.android;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import com.taobao.weex.devtools.common.Util;
import com.taobao.weex.devtools.inspector.elements.AbstractChainedDescriptor;
import com.taobao.weex.devtools.inspector.elements.AttributeAccumulator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

final class TextViewDescriptor extends AbstractChainedDescriptor<TextView> {
    private static final String TEXT_ATTRIBUTE_NAME = "text";
    private final Map<TextView, ElementContext> mElementToContextMap = Collections.synchronizedMap(new IdentityHashMap());

    TextViewDescriptor() {
    }

    /* access modifiers changed from: protected */
    public void onHook(TextView textView) {
        ElementContext elementContext = new ElementContext();
        elementContext.hook(textView);
        this.mElementToContextMap.put(textView, elementContext);
    }

    /* access modifiers changed from: protected */
    public void onUnhook(TextView textView) {
        this.mElementToContextMap.remove(textView).unhook();
    }

    /* access modifiers changed from: protected */
    public void onGetAttributes(TextView textView, AttributeAccumulator attributeAccumulator) {
        CharSequence text = textView.getText();
        if (text.length() != 0) {
            attributeAccumulator.store("text", text.toString());
        }
    }

    private final class ElementContext implements TextWatcher {
        private TextView mElement;

        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        private ElementContext() {
        }

        public void hook(TextView textView) {
            this.mElement = (TextView) Util.throwIfNull(textView);
            this.mElement.addTextChangedListener(this);
        }

        public void unhook() {
            if (this.mElement != null) {
                this.mElement.removeTextChangedListener(this);
                this.mElement = null;
            }
        }

        public void afterTextChanged(Editable editable) {
            if (editable.length() == 0) {
                TextViewDescriptor.this.getHost().onAttributeRemoved(this.mElement, "text");
            } else {
                TextViewDescriptor.this.getHost().onAttributeModified(this.mElement, "text", editable.toString());
            }
        }
    }
}