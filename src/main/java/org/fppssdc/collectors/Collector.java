/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;

import java.util.ArrayList;

/**
 * Abstract collector class for each collectortype
 */
public abstract class Collector implements Runnable
{
    protected ProviderAccountObject providerAccount;

    public abstract void run();
}
