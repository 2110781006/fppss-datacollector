/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import org.fppssdc.model.ProviderAccountObject;

/**
 * Abstract collector class for each collectortype
 */
public abstract class Collector implements Runnable
{
    protected ProviderAccountObject providerAccount;
    protected Integer interval;

    public abstract void run();
}
