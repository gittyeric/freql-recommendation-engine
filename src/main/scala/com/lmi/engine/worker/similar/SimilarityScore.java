package com.lmi.engine.worker.similar;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SimilarityScore implements Externalizable {

    public static final String ORIGIN_ID_FIELD = "originId";
    public static final String SCORE_FIELD = "score";

    private String id;

    @AffinityKeyMapped
    @QuerySqlField(index = true, name = ORIGIN_ID_FIELD)
    private String originId;

    @QuerySqlField(name = SCORE_FIELD)
    private float score;

    public SimilarityScore() {

    }

    public SimilarityScore(String id, String originId, float score) {
        this.id = id;
        this.originId = originId;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getOriginId() {
        return originId;
    }

    public float getScore() {
        return score;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(originId);
        out.writeFloat(score);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        id = in.readUTF();
        originId = in.readUTF();
        score = in.readFloat();
    }

}
