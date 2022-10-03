/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc;

import org.fppssdc.model.TimeValueObject;

import java.util.ArrayList;

/**
 * Abstract collector class for each collectortype
 */
public abstract class Collector implements Runnable
{
    public abstract void run();
}
