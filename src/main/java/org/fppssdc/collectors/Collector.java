/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import org.fppssdc.model.MeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Abstract collector class for each collectortype
 */
public abstract class Collector implements Runnable
{
    protected ProviderAccountObject providerAccount;
    protected Integer interval;

    public abstract void run();

    protected ArrayList<TimeValueObject> buildHourValuesFromSpontanValues(MeteringPoint meteringPoint, ArrayList<TimeValueObject> spontanValues)
    {
        HashMap<OffsetDateTime, ArrayList<Float>> hourValuesMap = new HashMap<>();

        ArrayList<TimeValueObject> hourValues = new ArrayList<>();

        for ( var spontanValue : spontanValues )
        {
            OffsetDateTime hourTimestamp = OffsetDateTime.of(spontanValue.getTimestamp().getYear(),
                    spontanValue.getTimestamp().getMonthValue(),spontanValue.getTimestamp().getDayOfMonth(),
                    spontanValue.getTimestamp().getHour(),0,0,0, ZoneOffset.UTC);

            if ( !hourValuesMap.containsKey(hourTimestamp) )
                hourValuesMap.put(hourTimestamp, new ArrayList<Float>());

            hourValuesMap.get(hourTimestamp).add(spontanValue.getValue().floatValue());
        }

        for ( var hour : hourValuesMap.keySet() )
        {
            ArrayList<Float> values = hourValuesMap.get(hour);
            BigDecimal sum = new BigDecimal(values.stream().collect(Collectors.summingDouble(d->d)));

            hourValues.add(new TimeValueObject(hour, meteringPoint.getId(), meteringPoint.getDatapoints().get(0), providerAccount.getProviderAccountId(),
                    sum, new BigDecimal(0), meteringPoint.getType().ordinal()));
        }

        return hourValues;
    }
}
