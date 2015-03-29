package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.List;

import com.datdo.mobilib.util.MblUtils;

public class MblIDConverter {

    private static final String SEPARATOR = "#";

    private String mPrefix;

    @SuppressWarnings("rawtypes")
    public MblIDConverter(Class forClass) {
        mPrefix = forClass.getName() + SEPARATOR;
    }

    public String toComboId(String id) {
        if (isComboId(id)) {
            return id;
        }
        if (id != null) {
            return mPrefix + id;
        }
        return null;
    }

    public List<String> toComboIds(List<String> ids) {
        List<String> comboIds = new ArrayList<String>();
        if (!MblUtils.isEmpty(ids)) {
            for (String id : ids) {
                comboIds.add(toComboId(id));
            }
        }
        return comboIds;
    }

    public String toOriginId(String id) {
        if (isOriginId(id)) {
            return id;
        }
        if (isComboId(id)) {
            return id.substring(mPrefix.length());
        }
        return null;
    }

    public List<String> toOriginIds(List<String> ids) {
        List<String> originIds = new ArrayList<String>();
        if (!MblUtils.isEmpty(ids)) {
            for (String id : ids) {
                originIds.add(toOriginId(id));
            }
        }
        return originIds;
    }

    public boolean isComboId(String id) {
        return id != null && id.startsWith(mPrefix);
    }

    public boolean isOriginId(String id) {
        return id != null && !isComboId(id);
    }
}
