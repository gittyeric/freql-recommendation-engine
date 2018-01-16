package com.lmi.engine.worker.parse;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ParsedEvent implements Externalizable {

    private String eventType;
    private String originId;
    private String destinationId;

    //Needed for serialization
    public ParsedEvent() {
    }

    public ParsedEvent(String eventType, String originId, String destinationId) {
        this.eventType = eventType;
        this.originId = originId;
        this.destinationId = destinationId;
    }

    public String eventType() {
        return eventType;
    }

    public String originId() {
        return originId;
    }

    public String destinationId() {
        return destinationId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        eventType = in.readUTF();
        originId = in.readUTF();
        destinationId = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(eventType);
        out.writeUTF(originId);
        out.writeUTF(destinationId);
    }

}
