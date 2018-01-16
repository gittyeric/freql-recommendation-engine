package com.lmi.engine.worker.coocur;

import org.apache.ignite.cache.CacheEntryProcessor;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

public class IncrementPairClosure implements CacheEntryProcessor<String, CoocurrencePair, Boolean> {

    public IncrementPairClosure() {
    }

    @Override
    public Boolean process(MutableEntry<String, CoocurrencePair> entry, Object... arguments) throws EntryProcessorException {

        String key = entry.getKey();
        String targetAId = CoocurUtil.obj1IdFromKey(key);
        String targetBId = CoocurUtil.obj2IdFromKey(key);

        int newCount = 1;
        if (entry.exists()) {
            newCount = entry.getValue().getCoocurCount() + 1;
        }

        entry.setValue(new CoocurrencePair(targetAId, targetBId, newCount));
        return true;
    }

}
