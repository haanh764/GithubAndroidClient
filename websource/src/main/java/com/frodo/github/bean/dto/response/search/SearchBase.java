package com.frodo.github.bean.dto.response.search;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Bernat on 08/08/2014.
 */
public class SearchBase implements Parcelable {

    @JsonProperty("total_count")
    public int totalCount;
    @JsonProperty("incomplete_results")
    public boolean incompleteResults;

    public SearchBase() {
    }

    protected SearchBase(Parcel in) {
        this.totalCount = in.readInt();
        this.incompleteResults = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.totalCount);
        dest.writeByte(incompleteResults ? (byte) 1 : (byte) 0);
    }
}
