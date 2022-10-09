/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.model;

import java.util.ArrayList;

/**
 * Meetering point class
 */
public class MeeteringPoint
{
    public enum MeeteringPointType
    {
        AccountingPoint
    }

    public enum Type
    {
        Consumption,
        Feedin,
        Production
    }

    private String id;
    private Type type;
    private MeeteringPointType meeteringPointType;
    private ArrayList<String> datapoints;

    public MeeteringPoint(String id, Type type, MeeteringPointType meeteringPointType, ArrayList<String> datapoints)
    {
        this.id = id;
        this.type = type;
        this.meeteringPointType = meeteringPointType;
        this.datapoints = datapoints;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public MeeteringPointType getMeeteringPointType()
    {
        return meeteringPointType;
    }

    public void setMeeteringPointType(MeeteringPointType meeteringPointType)
    {
        this.meeteringPointType = meeteringPointType;
    }

    public ArrayList<String> getDatapoints()
    {
        return datapoints;
    }

    public void setDatapoints(ArrayList<String> datapoints)
    {
        this.datapoints = datapoints;
    }
}
