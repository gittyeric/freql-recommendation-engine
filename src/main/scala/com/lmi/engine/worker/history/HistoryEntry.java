package com.lmi.engine.worker.history;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class HistoryEntry implements Externalizable {

    public static final String ORIGIN_ID_FIELD = "originId";
    public static final String DESTINATION_ID_FIELD = "destinationId";

    @QuerySqlField(index = true, name = ORIGIN_ID_FIELD)
    private String originId;
    @QuerySqlField(index = true, name = DESTINATION_ID_FIELD)
    private String destinationId;

    //Necessary for Serialization
    public HistoryEntry() {
    }

    public HistoryEntry(String originId, String destinationId) {
        this.originId = originId;
        this.destinationId = destinationId;
    }

    public String getOriginId() {
        return originId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(originId);
        out.writeUTF(destinationId);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        originId = in.readUTF();
        destinationId = in.readUTF();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryEntry that = (HistoryEntry) o;

        return originId.equals(that.originId) && destinationId.equals(that.destinationId);
    }

    @Override
    public int hashCode() {
        int result = originId.hashCode();
        result = 31 * result + destinationId.hashCode();
        return result;
    }
}
