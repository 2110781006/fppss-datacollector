/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.model;

import java.util.ArrayList;

/**
 * Metering point class
 */
public class MeteringPoint
{
    public enum MeteringPointType
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
    private MeteringPointType meteringPointType;
    private ArrayList<String> datapoints;

    public MeteringPoint(String id, Type type, MeteringPointType meteringPointType, ArrayList<String> datapoints)
    {
        this.id = id;
        this.type = type;
        this.meteringPointType = meteringPointType;
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

    public MeteringPointType getMeteringPointType()
    {
        return meteringPointType;
    }

    public void setMeteringPointType(MeteringPointType meteringPointType)
    {
        this.meteringPointType = meteringPointType;
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
