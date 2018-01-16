package com.lmi.engine.worker.coocur;

import org.apache.ignite.cache.CacheEntryProcessor;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

public class IncrementObjClosure implements CacheEntryProcessor<String, Integer, Boolean> {

    public IncrementObjClosure() {
    }

    @Override
    public Boolean process(MutableEntry<String, Integer> entry, Object... arguments) throws EntryProcessorException {
        Integer e = entry.getValue();

        int incrementAmount = 1;

        if (arguments.length == 1 && arguments[0] instanceof Integer) {
            incrementAmount = (Integer) arguments[0];
        }

        if (e == null) {
            e = incrementAmount;
        } else {
            e = e + incrementAmount;
        }

        entry.setValue(e);
        return true;
    }

}
