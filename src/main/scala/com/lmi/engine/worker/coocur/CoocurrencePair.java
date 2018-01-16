package com.lmi.engine.worker.coocur;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class CoocurrencePair implements Externalizable {

    public static final String NODE_A_ID_FIELD = "nodeAId";
    public static final String NODE_B_ID_FIELD = "nodeBId";
    public static final String COOCUR_COUNT_FIELD = "coocurCount";

    @QuerySqlField(index = true, name = NODE_A_ID_FIELD)
    private String nodeAId;
    @QuerySqlField(index = true, name = NODE_B_ID_FIELD)
    private String nodeBId;

    @QuerySqlField(name = COOCUR_COUNT_FIELD)
    private int coocurCount;

    public CoocurrencePair() {
    }

    public CoocurrencePair(String nodeAId, String nodeBId, int count) {
        this.nodeAId = nodeAId;
        this.nodeBId = nodeBId;
        this.coocurCount = count;
    }

    public String getNodeAId() {
        return nodeAId;
    }

    public String getNodeBId() {
        return nodeBId;
    }

    public int getCoocurCount() {
        return coocurCount;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(nodeAId);
        out.writeUTF(nodeBId);
        out.writeInt(coocurCount);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        nodeAId = in.readUTF();
        nodeBId = in.readUTF();
        coocurCount = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoocurrencePair that = (CoocurrencePair) o;
        return nodeAId.equals(that.nodeAId) && nodeBId.equals(that.nodeBId);
    }

    @Override
    public int hashCode() {
        int result = nodeAId.hashCode();
        result = 31 * result + nodeBId.hashCode();
        return result;
    }
}
