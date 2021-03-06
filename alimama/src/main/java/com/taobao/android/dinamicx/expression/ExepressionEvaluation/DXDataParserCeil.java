package com.taobao.android.dinamicx.expression.ExepressionEvaluation;

import com.taobao.android.dinamicx.DXRuntimeContext;
import com.taobao.android.dinamicx.expression.parser.DXAbsDinamicDataParser;

public class DXDataParserCeil extends DXAbsDinamicDataParser {
    public static final long DEFAULT_VALUE = 0;
    public static final long DX_PARSER_CEIL = 33272317873L;

    public Object evalWithArgs(Object[] objArr, DXRuntimeContext dXRuntimeContext) {
        if (objArr != null) {
            try {
                if (objArr.length == 1) {
                    Number number = objArr[0];
                    if (number instanceof Number) {
                        return new Double(Math.ceil(number.doubleValue()));
                    }
                    if (number instanceof String) {
                        return new Double(Math.ceil(Double.parseDouble((String) number)));
                    }
                    return 0L;
                }
            } catch (Throwable unused) {
                return 0L;
            }
        }
        return 0L;
    }
}
