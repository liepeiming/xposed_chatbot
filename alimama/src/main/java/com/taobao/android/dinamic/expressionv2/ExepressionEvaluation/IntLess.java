package com.taobao.android.dinamic.expressionv2.ExepressionEvaluation;

import com.taobao.android.dinamic.expression.parser.AbsDinamicDataParser;
import com.taobao.android.dinamic.log.DinamicLog;
import com.taobao.android.dinamic.model.DinamicParams;
import java.util.List;

public class IntLess extends AbsDinamicDataParser {
    public Object evalWithArgs(List list, DinamicParams dinamicParams) {
        DinamicLog.print("IntLess");
        if (list != null && list.size() == 2) {
            try {
                if (Integer.parseInt(list.get(0).toString()) < Integer.parseInt(list.get(1).toString())) {
                    return true;
                }
            } catch (NumberFormatException unused) {
                DinamicLog.print("Integer cast error!");
                return false;
            }
        }
        return false;
    }
}
