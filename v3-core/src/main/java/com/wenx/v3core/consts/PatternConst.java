package com.wenx.v3core.consts;

/**
 * 正则匹配常量
 *
 * @author wenx
 */
public class PatternConst {

    String REGEX_PHONE =
            "^(1[1-9][0-9]|14[5|7]|15[0|1|2|3|4|5|6|7|8|9]|18[0|1|2|3|5|6|7|8|9])\\d{8}$";

    String REGEX_PASSWORD =
            "^(?=.*\\d)(?=.*[a-zA-Z]).{8,}$";
}
